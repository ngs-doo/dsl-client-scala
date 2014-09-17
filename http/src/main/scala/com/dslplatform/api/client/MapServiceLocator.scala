package com.dslplatform.api.client

import com.dslplatform.api.patterns.ServiceLocator
import scala.collection.mutable.{Map => MMap}
import org.slf4j.Logger
import java.lang.reflect.Constructor
import java.lang.reflect.Type
import java.lang.reflect.TypeVariable
import java.lang.reflect.ParameterizedType
import sun.reflect.generics.reflectiveObjects.ParameterizedTypeImpl
import scala.reflect.ClassTag
import scala.reflect.runtime.universe._
import java.lang.reflect.Modifier

class MapServiceLocator(initialComponents: Map[Object, AnyRef], cacheResult: Boolean = true)
    extends ServiceLocator {

  private val logger = initialComponents.get(classOf[Logger]).asInstanceOf[Some[Logger]]
    .getOrElse(throw new RuntimeException("Logger was not provided with initial components."))

  private val components: MMap[Object, AnyRef] = MMap.empty ++ initialComponents + (classOf[ServiceLocator] -> this)

  def register(target: Class[_], service: AnyRef): MapServiceLocator = {
    logger.trace("About to register class " + target.getName + " " + service)
    components.put(target, service)
    this
  }

  def register(typ: Type, service: AnyRef): MapServiceLocator = {
    logger.trace("About to register type " + typ.toString + " " + service)
    components.put(typ, service)
    this
  }

  def register[I: ClassTag]: MapServiceLocator = register[I, I]

  def register[I: ClassTag, S: ClassTag]: MapServiceLocator = {
    val interfaceClass = implicitly[ClassTag[I]].runtimeClass.asInstanceOf[Class[I]]
    val serviceClass = implicitly[ClassTag[S]].runtimeClass.asInstanceOf[Class[S]]
    logger.trace("About to register " + interfaceClass.getName + " " + serviceClass.getName)
    components.put(interfaceClass, serviceClass)
    this
  }

  def register[I: ClassTag](service: AnyRef): MapServiceLocator = {
    val clazz = implicitly[ClassTag[I]].runtimeClass.asInstanceOf[Class[I]]
    logger.trace("About to register " + clazz.getName + " " + service)
    components.put(clazz, service)
    this
  }

  override def resolve[T](tpe: Type): T = {
    logger.trace("Resolving with type: " + tpe)
    val found = tpe match {
      case pt: ParameterizedType => resolveType(pt, true)
      case clazz: Class[_] => resolveClass(clazz, true)
      case _ => None
    }
    found.map(_.asInstanceOf[T]).getOrElse(
      throw new RuntimeException("Container could not construct type: " + tpe)
    )
  }

  private val mirror = runtimeMirror(getClass.getClassLoader)

  def resolve[T: TypeTag]: T = {
    typeOf[T] match {
      case TypeRef(_, sym, args) if args.isEmpty =>
        resolve(mirror.runtimeClass(sym.asClass))
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
    logger.trace("Getting class: " + clazz.getName)
    components.get(clazz) flatMap {
      case component: Class[_] =>
        logger.trace(component.getName + " is instance of class. Constructing.")
        tryConstruct(clazz, component.getConstructors)
      case component =>
        logger.trace("found component for class " + clazz.getName + ": " + component)
        Some(component)
    } orElse {
      if (clazz.isInterface || Modifier.isAbstract(clazz.getModifiers)) {
        //TODO: return None!?
        throw new RuntimeException(clazz + " is not registered in the container")
      }
      logger.trace(clazz.getName + " has not been mapped. Trying to construct.")
      tryConstruct(clazz, clazz.getConstructors)
    }
  }

  private def resolveType(typ: ParameterizedType, checkErrors: Boolean): Option[AnyRef] = {
    logger.trace("Getting type: " + typ.toString)
    components.get(typ) flatMap {
      case component: Class[_] =>
        logger.trace(component.getName + " is instance of class. Constructing.")
        tryConstruct(typ, component.getConstructors)
      case component =>
        logger.trace("found component for type " + typ.toString + ": " + component)
        Some(component)
    } orElse {
      logger.trace(typ.toString + " has not been mapped. Trying to construct.")
      typ match {
        case ti: ParameterizedTypeImpl =>
          val clazz = ti.getRawType
          if (clazz.isInterface || Modifier.isAbstract(clazz.getModifiers)) {
            components.get(clazz) flatMap {
                case mt: Class[_] =>
                  val args = ti.getActualTypeArguments
                  val genType = ParameterizedTypeImpl.make(mt, args, null)
                  val genClazz = genType.getRawType
                  tryConstruct(typ, genClazz.getConstructors)
                case impl =>
                  Some(impl)
              }
          } else {
            tryConstruct(ti, clazz.getConstructors)
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
      val params = ctor.getGenericParameterTypes map {
        case pi: ParameterizedType => resolveType(pi, false)
        case cl: Class[_] => resolveClass(cl, false)
        case _ => None
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
      val params = ctor.getGenericParameterTypes map {
        case ct: ClassTag[_] =>
          None
        case pi: ParameterizedType =>
          target.getActualTypeArguments()(0) match {
            case cl: Class[_] =>
              Some(ClassTag(cl))
            case tv: TypeVariable[_] =>
              //TODO generic type!?
              logger.trace("Generic type found!?" + tv)
              None
            case _ =>
              None
          }
        case cl: Class[_] =>
          resolveClass(cl, false)
        case p =>
          logger.trace("Unknown type argument: " + p)
          None
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
