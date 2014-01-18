package com.fasterxml.jackson
package module.scala
package ser

import util.Implicits._
import modifiers.OptionTypeModifierModule
import core.JsonGenerator
import databind._
import jsontype.TypeSerializer
import jsonschema.{ JsonSchema, SchemaAware }
import ser.{ ContextualSerializer, BeanPropertyWriter, BeanSerializerModifier, Serializers }
import ser.std.StdSerializer
import `type`.CollectionLikeType
import jsonFormatVisitors.JsonFormatVisitorWrapper
import java.lang.reflect.Type
import java.{ util => ju }
import scala.Some
import scala.collection.JavaConverters._
import com.fasterxml.jackson.module.scala.deser.OptionDeserializerModule
import com.fasterxml.jackson.module.scala.introspect.ScalaClassIntrospectorModule
import com.fasterxml.jackson.module.scala.deser.UntypedObjectDeserializerModule
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeName
import com.fasterxml.jackson.databind.jsontype.impl.AsPropertyTypeSerializer
import com.fasterxml.jackson.databind.ser.BeanSerializer
import com.fasterxml.jackson.databind.ser.impl.ObjectIdWriter
import com.fasterxml.jackson.databind.ser.std.BeanSerializerBase

private class CustomBeanSerializer(bsb: BeanSerializerBase) extends BeanSerializer(bsb) {

  def serializeWithType(
    bean: Any,
    jgen: JsonGenerator,
    provider: SerializerProvider,
    jti: JsonTypeInfo,
    jtn: JsonTypeName) {
    jgen.writeStartObject()
    jgen.writeStringField(jti.property, jtn.value)
    if (_propertyFilterId != null) {
      serializeFieldsFiltered(bean, jgen, provider);
    } else {
      serializeFields(bean, jgen, provider);
    }
    jgen.writeEndObject()
  }
}

private class CustomOptionSerializer(
  elementType: Option[JavaType],
  valueTypeSerializer: Option[TypeSerializer],
  beanProperty: Option[BeanProperty],
  elementSerializer: Option[JsonSerializer[AnyRef]],
  jsonTypeInfo: Option[JsonTypeInfo])
    extends StdSerializer[Option[_]](classOf[Option[_]])
    with ContextualSerializer
    with SchemaAware {
  def serialize(value: Option[_], jgen: JsonGenerator, provider: SerializerProvider) {
    (value, elementSerializer) match {
      case (Some(v: AnyRef), Some(vs)) =>
        vs.serialize(v, jgen, provider)
      case (Some(v), _) =>
        jsonTypeInfo match {
          case Some(jti) =>
            val clazz = v.getClass
            val jtn = clazz.getAnnotation(classOf[JsonTypeName])
            if (jtn != null) {
              beanProperty match {
                case Some(bp) =>
                  provider.findValueSerializer(clazz, bp) match {
                    case bs: BeanSerializer =>
                      var cbs = new CustomBeanSerializer(bs)
                      cbs.serializeWithType(v, jgen, provider, jti, jtn)
                    case vs =>
                      vs.serialize(v.asInstanceOf[AnyRef], jgen, provider)
                  }
                case _ =>
                  provider.defaultSerializeValue(v, jgen)
              }
            } else {
              provider.defaultSerializeValue(v, jgen)
            }
          case _ =>
            provider.defaultSerializeValue(v, jgen)
        }
      case (None, _) =>
        provider.defaultSerializeNull(jgen)
    }
  }

  def createContextual(prov: SerializerProvider, property: BeanProperty): JsonSerializer[_] = {
    // Based on the version in AsArraySerializerBase
    val typeSer = valueTypeSerializer.optMap(_.forProperty(property))
    var ser: Option[JsonSerializer[_]] =
      Option(property).flatMap { p =>
        Option(p.getMember).flatMap { m =>
          Option(prov.getAnnotationIntrospector.findContentSerializer(m)).map { serDef =>
            prov.serializerInstance(m, serDef)
          }
        }
      } orElse elementSerializer
    ser = Option(findConvertingContentSerializer(prov, property, ser.orNull))
    if (ser.isEmpty) {
      if (elementType.isDefined) {
        if (hasContentTypeAnnotation(prov, property)) {
          ser = Option(prov.findValueSerializer(elementType.get, property))
        }
      }
    } else {
      ser = Option(prov.handleSecondaryContextualization(ser.get, property))
    }
    if ((ser != elementSerializer) || (property != beanProperty.orNull) || (valueTypeSerializer != typeSer)) {
      val jti = property.getAnnotation(classOf[JsonTypeInfo])
      new CustomOptionSerializer(elementType, typeSer, Option(property), ser.asInstanceOf[Option[JsonSerializer[AnyRef]]], Option(jti))
    } else this
  }

  def hasContentTypeAnnotation(provider: SerializerProvider, property: BeanProperty) = {
    Option(property).exists { p =>
      Option(provider.getAnnotationIntrospector).exists { intr =>
        Option(intr.findSerializationContentType(p.getMember, p.getType)).isDefined
      }
    }
  }

  override def isEmpty(value: Option[_]): Boolean = value.isEmpty

  override def getSchema(provider: SerializerProvider, typeHint: Type): JsonNode =
    getSchema(provider, typeHint, isOptional = true)

  override def getSchema(provider: SerializerProvider, typeHint: Type, isOptional: Boolean): JsonNode = {
    val contentSerializer = elementSerializer.getOrElse {
      val javaType = provider.constructType(typeHint)
      val componentType = javaType.containedType(0)
      provider.findTypedValueSerializer(componentType, true, beanProperty.orNull)
    }
    contentSerializer match {
      case cs: SchemaAware => cs.getSchema(provider, contentSerializer.handledType(), isOptional)
      case _               => JsonSchema.getDefaultSchemaNode
    }
  }

  override def acceptJsonFormatVisitor(wrapper: JsonFormatVisitorWrapper, javaType: JavaType) {
    val containedType = javaType.containedType(0)
    val ser = elementSerializer.getOrElse(wrapper.getProvider.findTypedValueSerializer(containedType, true, beanProperty.orNull))
    ser.acceptJsonFormatVisitor(wrapper, containedType)
  }
}

private class CustomOptionPropertyWriter(delegate: BeanPropertyWriter) extends BeanPropertyWriter(delegate) {
  override def serializeAsField(bean: AnyRef, jgen: JsonGenerator, prov: SerializerProvider) {
    (get(bean), _nullSerializer) match {
      // value is None, which we'll serialize as null, but there's no
      // null-serializer, which means it should be suppressed
      case (None, null) =>
      case _            => super.serializeAsField(bean, jgen, prov)
    }
  }
}

private object CustomOptionBeanSerializerModifier extends BeanSerializerModifier {

  override def changeProperties(config: SerializationConfig,
    beanDesc: BeanDescription,
    beanProperties: ju.List[BeanPropertyWriter]): ju.List[BeanPropertyWriter] = {

    beanProperties.asScala.transform { w =>
      if (classOf[Option[_]].isAssignableFrom(w.getPropertyType))
        new CustomOptionPropertyWriter(w)
      else
        w
    }.asJava

  }
}

private object CustomOptionSerializerResolver extends Serializers.Base {

  private val OPTION = classOf[Option[_]]

  override def findCollectionLikeSerializer(config: SerializationConfig,
    `type`: CollectionLikeType,
    beanDesc: BeanDescription,
    elementTypeSerializer: TypeSerializer,
    elementValueSerializer: JsonSerializer[AnyRef]): JsonSerializer[_] =

    if (!OPTION.isAssignableFrom(`type`.getRawClass)) null
    else new CustomOptionSerializer(Option(`type`.containedType(0)), Option(elementTypeSerializer), None, Option(elementValueSerializer), None)
}

trait CustomOptionSerializerModule extends OptionTypeModifierModule {
  this += { ctx =>
    ctx addSerializers CustomOptionSerializerResolver
    ctx addBeanSerializerModifier CustomOptionBeanSerializerModifier
  }
}

trait CustomOptionModule extends CustomOptionSerializerModule with OptionDeserializerModule

class CustomDefaultScalaModule
    extends JacksonModule
    with IteratorModule
    with EnumerationModule
    with CustomOptionModule
    with SeqModule
    with IterableModule
    with TupleModule
    with MapModule
    with SetModule
    with ScalaClassIntrospectorModule
    with UntypedObjectDeserializerModule {
  override def getModuleName = "CustomDefaultScalaModule"
}

object CustomDefaultScalaModule extends CustomDefaultScalaModule
