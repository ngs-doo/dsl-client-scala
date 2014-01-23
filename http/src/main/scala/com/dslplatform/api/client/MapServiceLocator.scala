package com.dslplatform.api.client

import com.dslplatform.api.patterns.ServiceLocator
import scala.collection.mutable.{ Map => MMap }
import org.slf4j.Logger

import java.lang.reflect.Constructor
import java.lang.reflect.Type
import java.lang.reflect.TypeVariable
import java.lang.reflect.ParameterizedType

import sun.reflect.generics.reflectiveObjects.ParameterizedTypeImpl

import scala.reflect.ClassTag
import scala.reflect.runtime.universe._
import java.util.concurrent.ConcurrentHashMap

class MapServiceLocator(logger: Logger, cacheResult: Boolean = true) extends ServiceLocator {
  private val components: MMap[Object, AnyRef] =
    MMap(classOf[ServiceLocator] -> this, classOf[Logger] -> logger)

  def register(target: Class[_], service: AnyRef): MapServiceLocator = {
    logger.trace("About to register class " + target.getName() + " " + service)
    components.put(target, service)
    this
  }

  def register(typ: Type, service: AnyRef): MapServiceLocator = {
    logger.trace("About to register type " + typ.toString() + " " + service)
    components.put(typ, service)
    this
  }

  def register[I: ClassTag]: MapServiceLocator = register[I, I]

  def register[I: ClassTag, S: ClassTag]: MapServiceLocator = {
    val interfaceClass = implicitly[ClassTag[I]].runtimeClass.asInstanceOf[Class[I]]
    val serviceClass = implicitly[ClassTag[S]].runtimeClass.asInstanceOf[Class[S]]
    logger.trace("About to register " + interfaceClass.getName() + " " + serviceClass.getName())
    components.put(interfaceClass, serviceClass)
    this
  }

  def register[I: ClassTag](service: AnyRef): MapServiceLocator = {
    val clazz = implicitly[ClassTag[I]].runtimeClass.asInstanceOf[Class[I]]
    logger.trace("About to register " + clazz.getName() + " " + service)
    components.put(clazz, service)
    this
  }

  def resolve[T](clazz: Class[T]): T = {
    logger.trace("Resolving with class type: " + clazz.getName())
    resolveClass(clazz, true) match {
      case Some(inst) =>
        inst.asInstanceOf[T]
      case _ =>
        throw new RuntimeException("Container could not construct class of type: " + clazz.getName())
    }
  }

  private val mirror = runtimeMirror(getClass.getClassLoader)

  def resolve[T: TypeTag]: T = {
    typeOf[T] match {
      case TypeRef(_, sym, args) if args.isEmpty =>
        resolve(mirror.runtimeClass(sym.asClass).asInstanceOf[Class[T]])
      case TypeRef(_, sym, args) =>
        val symClass = mirror.runtimeClass(sym.asClass).asInstanceOf[Class[T]]
        val typeArgs = args.map(t => mirror.runtimeClass(t))
        val genType = ParameterizedTypeImpl.make(symClass, typeArgs.toArray, null)
        resolveType(genType, true) match {
          case Some(inst) =>
            inst.asInstanceOf[T]
          case _ =>
            throw new RuntimeException("Container could not construct class of type: " + sym)
        }
      case _ =>
        throw new RuntimeException("Invalid type tag argument")
    }
  }

  private def cacheIf(serv: Object, service: AnyRef) {
    logger.trace("Caching " + serv + " as " + service)
    if (cacheResult) components.put(serv, service)
  }

  private def resolveClass(clazz: Class[_], checkErrors: Boolean): Option[AnyRef] = {
    logger.trace("Getting class: " + clazz.getName())
    components.get(clazz) flatMap {
      case component: Class[_] =>
        logger.trace(component.getName() + " is instance of class. Constructing.")
        tryConstruct(clazz, component.getConstructors())
      case component =>
        logger.trace("found component for class " + clazz.getName() + ": " + component)
        Some(component)
    } orElse {
      if (clazz.isInterface()) {
        throw new RuntimeException(clazz + " is not registered in the container")
      }
      logger.trace(clazz.getName + " has not been mapped. Trying to construct.")
      tryConstruct(clazz, clazz.getConstructors())
    }
  }

  private def resolveType(typ: ParameterizedType, checkErrors: Boolean): Option[AnyRef] = {
    logger.trace("Getting type: " + typ.toString())
    components.get(typ) flatMap {
      case component: Class[_] =>
        logger.trace(component.getName() + " is instance of class. Constructing.")
        tryConstruct(typ, component.getConstructors())
      case component =>
        logger.trace("found component for type " + typ.toString() + ": " + component)
        Some(component)
    } orElse {
      logger.trace(typ.toString() + " has not been mapped. Trying to construct.")
      typ match {
        case ti: ParameterizedTypeImpl =>
          val clazz = ti.getRawType()
          if (clazz.isInterface()) {
            components.get(clazz) flatMap { impl =>
              impl match {
                case mt: Class[_] =>
                  val args = ti.getActualTypeArguments()
                  val genType = ParameterizedTypeImpl.make(mt, args, null)
                  val genClazz = genType.getRawType()
                  tryConstruct(typ, genClazz.getConstructors())
                case _ =>
                  Some(impl)
              }
            }
          } else {
            val tiClazz = ti.getRawType()
            tryConstruct(ti, tiClazz.getConstructors())
          }
        case _ =>
          logger.trace("Unknown type " + typ)
          None
      }
    }
  }

  @annotation.tailrec
  private def tryConstruct(target: Class[_], ctors: Traversable[Constructor[_]]): Option[AnyRef] = {
    if (ctors.isEmpty) None
    else {
      val ctor = ctors.head
      logger.trace("About to construct class " + target + " with " + ctor)
      val params = ctor.getGenericParameterTypes() map { p =>
        p match {
          case pi: ParameterizedTypeImpl =>
            resolveType(pi, false)
          case cl: Class[_] =>
            resolveClass(cl, false)
          case _ => throw new RuntimeException("Unknown type argument: " + p)
        }
      }
      if (params.length == 0 || params.forall(_.nonEmpty)) {
        val instance = ctor.newInstance(params.flatten[Object]: _*).asInstanceOf[AnyRef]
        cacheIf(target, instance)
        Some(instance)
      } else {
        logger.trace("Trying next constructor")
        tryConstruct(target, ctors.tail)
      }
    }
  }

  @annotation.tailrec
  private def tryConstruct(target: ParameterizedType, ctors: Traversable[Constructor[_]]): Option[AnyRef] = {
    if (ctors.isEmpty) None
    else {
      val ctor = ctors.head
      logger.trace("About to construct param " + target + " with " + ctor)
      val params = ctor.getGenericParameterTypes() map { p =>
        p match {
          case ct: ClassTag[_] =>
            None
          case pi: ParameterizedType =>
            val typ = target.getActualTypeArguments()(0)
            val ctt = ClassTag.apply(typ.asInstanceOf[Class[_]])
            Some(ctt)
          case cl: Class[_] =>
            resolveClass(cl, false)
          case _ => throw new RuntimeException("Unknown type argument: " + p)
        }
      }
      if (params.length == 0 || params.forall(_.nonEmpty)) {
        val instance = ctor.newInstance(params.flatten[Object]: _*).asInstanceOf[AnyRef]
        cacheIf(target, instance)
        Some(instance)
      } else {
        logger.trace("Trying next constructor")
        tryConstruct(target, ctors.tail)
      }
    }
  }
}
