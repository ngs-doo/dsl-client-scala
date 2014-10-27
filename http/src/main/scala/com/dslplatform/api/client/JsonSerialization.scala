package com.dslplatform.api.client

import scala.io.Source
import scala.reflect.ClassTag
import scala.xml.Elem
import scala.xml.parsing.ConstructingParser
import org.joda.time.DateTime
import org.joda.time.LocalDate
import org.joda.time.format.DateTimeFormat
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.Version
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.InjectableValues
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.module.scala.ser.CustomDefaultScalaModule
import com.dslplatform.api.patterns.ServiceLocator
import com.fasterxml.jackson.databind.JavaType

class JsonSerialization(locator: ServiceLocator) {

  // ---- Date -------------------------------------------------------------------

  private val dateFormat = DateTimeFormat.forPattern("y-MM-dd'T00:00:00")

  private val dateSerializer = new JsonSerializer[LocalDate] {
    override def serialize(value: LocalDate, generator: JsonGenerator, x: SerializerProvider) =
      generator.writeString(dateFormat.print(value))
  }

  private val dateDeserializer = new JsonDeserializer[LocalDate] {
    override def deserialize(parser: JsonParser, context: DeserializationContext) =
      dateFormat.parseLocalDate(parser.getValueAsString)
  }

  // ---- TimeStamp --------------------------------------------------------------

  private val timestampSerializer = new JsonSerializer[DateTime] {
    override def serialize(value: DateTime, generator: JsonGenerator, x: SerializerProvider) =
      generator.writeString(value.toString)
  }

  private val timestampDeserializer = new JsonDeserializer[DateTime] {
    override def deserialize(parser: JsonParser, context: DeserializationContext) =
      new DateTime(parser.getValueAsString)
  }

  // ---- BigDecimal-------------------------------------------------------------------------

  private val bigDecimalSerializer = new JsonSerializer[BigDecimal] {
    override def serialize(value: BigDecimal, generator: JsonGenerator, x: SerializerProvider) =
      generator.writeString(value.toString)
  }

  private val bigDecimalDeserializer = new JsonDeserializer[BigDecimal] {
    override def deserialize(parser: JsonParser, context: DeserializationContext) =
      BigDecimal(parser.getValueAsString)
  }

  // ---- Elem -------------------------------------------------------------------

  private val elemSerializer = new JsonSerializer[Elem] {
    override def serialize(value: Elem, generator: JsonGenerator, x: SerializerProvider) =
      generator.writeString(value.toString())
  }

  private val elemDeserializer = new JsonDeserializer[Elem] {
    override def deserialize(parser: JsonParser, context: DeserializationContext) =
      ConstructingParser
        .fromSource(Source.fromString(parser.getValueAsString), true)
        .document.docElem.asInstanceOf[Elem]
  }

  // ----serializationModule -----------------------------------------------------

  private val version = new Version(0, 0, 0, "SNAPSHOT", "com.dslplatform", "dsl-client-scala")

  private val serializationModule =
    new SimpleModule("SerializationModule", version)
      .addSerializer(classOf[LocalDate], dateSerializer)
      .addSerializer(classOf[DateTime], timestampSerializer)
      .addSerializer(classOf[BigDecimal], bigDecimalSerializer)
      .addSerializer(classOf[Elem], elemSerializer)

  private val serializationMapper =
    new ObjectMapper()
      .registerModule(CustomDefaultScalaModule)
      .setSerializationInclusion(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
      .registerModule(serializationModule)
      //.configure(MapperFeature.AUTO_DETECT_GETTERS, false)
      //.configure(MapperFeature.AUTO_DETECT_FIELDS, false)
      //.configure(MapperFeature.AUTO_DETECT_IS_GETTERS, false)
      //.configure(MapperFeature.AUTO_DETECT_SETTERS, false)

  // ----deserializationModule----------------------------------------------------

  private val deserializationModule =
    new SimpleModule("DeserializationModule", version)
      .addDeserializer(classOf[LocalDate], dateDeserializer)
      .addDeserializer(classOf[DateTime], timestampDeserializer)
      .addDeserializer(classOf[BigDecimal], bigDecimalDeserializer)
      .addDeserializer(classOf[Elem], elemDeserializer)

  private val deserializationMapper = {
    val mapper = new ObjectMapper()
    mapper.registerModule(DefaultScalaModule)
      .registerModule(deserializationModule)
      .setInjectableValues(new InjectableValues.Std addValue ("__locator", locator))
      .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
  }

  private val typeFactory = deserializationMapper.getTypeFactory

  // -----------------------------------------------------------------------

  def serialize[T](data: T): String = serializationMapper.writeValueAsString(data)

  private def getClassTag[T: ClassTag]: ClassTag[T] = implicitly[ClassTag[T]]

  def deserialize[TResult: ClassTag](data: Array[Byte]): TResult =
    deserializationMapper.readValue[TResult](data, getClassTag[TResult].runtimeClass.asInstanceOf[Class[TResult]])

  def deserialize[TResult](data: Array[Byte], buildType: JavaType): TResult =
    deserializationMapper.readValue(data, buildType)

  def deserializeList[TResult: ClassTag](data: Array[Byte]): IndexedSeq[TResult] =
    deserializationMapper.readValue(data, buildCollectionType[TResult])

  def deserializeList[TResult](clazz: Class[_], data: Array[Byte]): IndexedSeq[TResult] =
    deserializationMapper.readValue(data, buildCollectionType(clazz))

  private def buildCollectionType[TResult: ClassTag] =
    typeFactory.constructCollectionLikeType(classOf[IndexedSeq[_]],
      getClassTag[TResult].runtimeClass.asInstanceOf[Class[TResult]])

  private def buildCollectionType(clazz: Class[_]) =
    typeFactory.constructCollectionLikeType(classOf[IndexedSeq[_]], clazz)
}
