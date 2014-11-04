package com.dslplatform.test

import com.dslplatform.api.client.JsonSerialization
import com.dslplatform.api.patterns.{PersistableRepository, ServiceLocator}
import com.dslplatform.test.complex.{BaseRoot, EmptyRoot}
import com.dslplatform.test.concept.{emptyConcept, fieldedConcept, rootWithConcept}
import com.dslplatform.test.simple._
import com.fasterxml.jackson.annotation.{JsonProperty, JsonSubTypes, JsonTypeInfo, JsonTypeName}
import org.joda.time.{DateTime, LocalDate}
import org.specs2._
import org.specs2.specification.Step

class SerializationTest extends Specification {

  def is = s2"""
    JsonSerialization is used to serialize object for transport
      JsonSerialization resolves from a locator         ${jsonSerialization(resolve)}
      serialize a value                                 ${jsonSerialization(serializeAValue)}
      serialize an entity                               ${jsonSerialization(serializeAnEntity)}
      serialize a List of entity                        ${jsonSerialization(serializeAListOfEntities)}
      deserialize locator in an entity                  ${jsonSerialization(deserializeALocatorInAnEntity)}
      deserialize locator in a root                     ${jsonSerialization(deserializeALocatorInARoot)}
      deserialize non-optional objects in value         ${jsonSerialization(deserializeNonOptionalObjectsInAValue)}
      deserialize non-optional objects in entity        ${jsonSerialization(deserializeNonOptionalObjectsInAEntity)}
      enum serialize/deserialize                        ${jsonSerialization(enumSerializeDeserialize)}
      deserialize non-optional objects in snowflake     ${jsonSerialization(deserializeNonOptionalObjectsInASnowflake)}
      deserialize non-optional objects in event         ${jsonSerialization(deserializeNonOptionalObjectsInAnEvent)}
      double deserialization                            ${jsonSerialization(doubleDeserialization)}
      trait serialization                               ${jsonSerialization(traitSerialization)}
      trait deserialization                             ${jsonSerialization(traitDeserialization)}
      trait in snowflake deserialization                ${jsonSerialization(traitInSnowflakeDeserialization)}
      nulls                                             ${jsonSerialization(nulls)}
      optional empty mixin                              ${jsonSerialization(optionalEmptyMixin)}
      optional mixin with props                         ${jsonSerialization(optionalMixinWithProps)}
      optional mixin with signature                     ${jsonSerialization(optionalMixinWithSignature)}
      deser                                             ${jsonSerialization(deser)}

      enum serialize/deserialize from server            ${located(enumSerializeDeserializeFromServer)}"
      server trait serialization                        ${located(serverTraitSerialization)}
                                                        ${Step(located.close())}
  """

  val located = new Located
  val jsonSerialization = located.resolved[JsonSerialization]

  implicit val duration: scala.concurrent.duration.Duration = scala.concurrent.duration.Duration(10,
    "seconds")

  def resolve = { jsonSerialization: JsonSerialization =>
    jsonSerialization.isInstanceOf[JsonSerialization] must beTrue
  }

  def serializeAValue = { jsonSerialization: JsonSerialization =>
    val simpleVal = Val(1, 1.2f, "str")
    simpleVal === jsonSerialization.deserialize[Val](jsonSerialization.serialize(simpleVal).getBytes("UTF8"))
  }

  def serializeAnEntity = { jsonSerialization: JsonSerialization =>
    val simpleEnt = SimpleEntity(1, 1.2f, "str")
    simpleEnt === jsonSerialization.deserialize[SimpleEntity](jsonSerialization.serialize(simpleEnt).getBytes("UTF8"))
  }

  def serializeAListOfEntities = { jsonSerialization: JsonSerialization =>
    val simpleEntList = List(SimpleEntity(1, 1.2f, "str"), SimpleEntity(1, 1.2f, "str"), SimpleEntity(1, 1.2f, "str"))
    simpleEntList === jsonSerialization.deserializeList[SimpleEntity](
      jsonSerialization.serialize(simpleEntList).getBytes("UTF8"))
  }

  def deserializeALocatorInAnEntity = { jsonSerialization: JsonSerialization =>
    val simpleEnt = jsonSerialization.deserialize[SimpleEntity]("{}".getBytes("UTF-8"))
    val cl = classOf[SimpleEntity]
    val fld = cl.getDeclaredField("__locator")
    fld.setAccessible(true)
    fld.get(simpleEnt) !== null
  }

  def deserializeALocatorInARoot = { jsonSerialization: JsonSerialization =>
    val simpleRoot = jsonSerialization.deserialize[SimpleRoot]("{}".getBytes("UTF-8"))
    val cl = classOf[SimpleRoot]
    val fld = cl.getDeclaredField("__locator")
    fld.setAccessible(true)
    (fld.get(simpleRoot) !== null)
  }

  def deserializeNonOptionalObjectsInAValue = { jsonSerialization: JsonSerialization =>
    val dtd = jsonSerialization.deserialize[ValDTD]("{}".getBytes("UTF-8"))
    dtd.T.withMillis(0) === DateTime.now().withMillis(0)
    (dtd.D === BigDecimal(0)) &
      (dtd.DT === LocalDate.now())
  }

  def deserializeNonOptionalObjectsInAEntity = { jsonSerialization: JsonSerialization =>
    val dtd = jsonSerialization.deserialize[RootDTD]("{}".getBytes("UTF-8"))
    dtd.T.withMillis(0) === DateTime.now().withMillis(0)
    (dtd.D === BigDecimal(0)) &
      (dtd.DT === LocalDate.now())
  }

  def enumSerializeDeserialize = { jsonSerialization: JsonSerialization =>
    val simpleVal = Val(e = E.B)
    simpleVal === jsonSerialization.deserialize[Val](jsonSerialization.serialize(simpleVal).getBytes("UTF8"))
  }

  def enumSerializeDeserializeFromServer = { implicit locator: ServiceLocator => //404
    val root = SimpleRoot(e = E.B)
    val srRepo = locator.resolve[PersistableRepository[SimpleRoot]]
    srRepo.insert(root)
    (root.e === E.B) & {
      val remote = SimpleRoot.find(root.URI)
      remote.e === E.B
    }
  }

  def deserializeNonOptionalObjectsInASnowflake = { jsonSerialization: JsonSerialization =>
    val dtd = jsonSerialization.deserialize[snowDTD]("{}".getBytes("UTF-8"))
    dtd.T.withMillis(0) === DateTime.now().withMillis(0)
    dtd.D === BigDecimal(0)
    dtd.DT === LocalDate.now()
  }

  def deserializeNonOptionalObjectsInAnEvent = { jsonSerialization: JsonSerialization =>
    val dtd = jsonSerialization.deserialize[EveDTD]("{}".getBytes("UTF-8"))
    dtd.T.withMillis(0) === DateTime.now().withMillis(0)
    dtd.D === BigDecimal(0)
    dtd.DT === LocalDate.now()
  }

  def doubleDeserialization = { jsonSerialization: JsonSerialization =>
    val json = """[{"ID":1742,"empty":{},"URI":"1742"},{"ID":1743,"empty":{},"URI":"1743"}]"""
    val roots = jsonSerialization.deserializeList[BaseRoot](classOf[BaseRoot], json.getBytes("UTF-8"))
    roots.size === 2
  }

  def traitSerialization = { jsonSerialization: JsonSerialization =>
    val cl2 = clone1(m = V(url = java.net.URI.create("http://dsl-platform.com")))
    val json = jsonSerialization.serialize(cl2)
    (json.contains("$type") must beTrue) &
      (json.contains("http://dsl-platform.com") must beTrue)
  }

  def serverTraitSerialization = { implicit locator: ServiceLocator => // 404
    val cl2_new = clone1(m = V(url = java.net.URI.create("http://dsl-platform.com")))
    val repository: PersistableRepository[clone1] = locator.resolve[PersistableRepository[clone1]]
    repository.insert(cl2_new).map { uri => clone1.find(uri).m.isInstanceOf[V]} must beTrue.await
  }

  def traitDeserialization = { jsonSerialization: JsonSerialization =>
    val cl1 = jsonSerialization.deserialize[clone1](
      """{"m":{"$type":"simple.V","url":"http://dsl-platform.com"},"ID":0,"URI":"09710407-891b-4eef-9ba3-9abe210b69f1"}""".getBytes(
        "UTF-8"))
    cl1.m.isInstanceOf[com.dslplatform.test.simple.V] must beTrue
  }

  def traitInSnowflakeDeserialization = { jsonSerialization: JsonSerialization =>
    val sn1 = jsonSerialization.deserialize[snow1](
      """{"m":{"$type":"simple.V","url":"http://dsl-platform.com"},"URI":"09710407-891b-4eef-9ba3-9abe210b69f1"}""".getBytes(
        "UTF-8"))
    sn1.m.isInstanceOf[com.dslplatform.test.simple.V] must beTrue
  }

  def nulls = { jsonSerialization: JsonSerialization =>
    val oc = jsonSerialization.serialize(BaseRoot(root = EmptyRoot()))
    oc.contains("null") must beFalse
  }

  def optionalEmptyMixin = { jsonSerialization: JsonSerialization =>
    val t = jsonSerialization.serialize(rootWithConcept(c = Some(emptyConcept())))
    t.contains("$type") must beTrue
  }

  def optionalMixinWithProps = { jsonSerialization: JsonSerialization =>
    val inst = com.dslplatform.test.concept.rootWithConcept(c = Some(fieldedConcept("abc", "def")))
    val t = jsonSerialization.serialize(inst)
    (t.contains("$type") must beTrue) & {
      val d = jsonSerialization.deserialize[rootWithConcept](t.getBytes("UTF-8"))
      d.c === inst.c
    }
  }

  def optionalMixinWithSignature = { jsonSerialization: JsonSerialization =>
    val t1 = jsonSerialization.serialize(new TestMe(KnownImplementation()))
    t1.contains("$type") must beTrue
  }

  def deser = { jsonSerialization: JsonSerialization =>
    val t1 = jsonSerialization.deserialize[TestMe](
      "{\"signature\":{\"$type\":\"my.Implementation\"}}".getBytes("UTF-8"))
    t1.signature === Some(KnownImplementation())
  }


  @JsonSubTypes(Array(new JsonSubTypes.Type(classOf[KnownImplementation])))
  trait BaseSignature

  @JsonTypeName("my.Implementation")
  case class KnownImplementation() extends BaseSignature

  class TestMe(
    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "$type")
    @JsonProperty("signature")
    __signature: BaseSignature) {
    private var _signature: Option[BaseSignature] = Option(__signature)

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "$type")
    @JsonProperty("signature")
    def signature = _signature

    def signature_=(value: Option[BaseSignature]) {
      _signature = value
    }
  }

}
