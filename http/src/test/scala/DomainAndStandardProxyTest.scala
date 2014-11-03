import com.dslplatform.api.client.{ReportingProxy, DomainProxy, StandardProxy}
import com.dslplatform.api.patterns.{PersistableRepository, ServiceLocator}
import com.dslplatform.test.complex.{EmptyRoot, BaseRoot}
import com.dslplatform.test.simple._

import org.specs2._
import org.specs2.specification.Step

import scala.concurrent.Future

class DomainAndStandardProxyTest extends Specification with Common {

  def is = sequential ^ s2"""
    Standard proxy is used for CRUD operations
      StandardProxy resolves from a locator             ${standardProxy(resolve)}
      insert simple with standard proxy                 ${standardProxy(insertSimpleRootWithStandardProxy)}
      insert base with standard proxy                   ${located(insertBaseWithStandardProxy)}
      update simple with standard proxy                 ${located(updateWithStandardProxy)}
      delete                                            ${located(delete)}

    Reporting proxy
      can call history                                  ${located(canCallHistory)}

                                                        ${Step(located.clean[SimpleRoot])}
    Standard Proxy is used to pull olap calculations from the remote service.
      without Predicate                                 ${standardProxy(withoutPredicate)}
      with Predicate                                    ${standardProxy(withPredicate)}
      with roots predicate                              ${standardProxy(withRootsPredicate)}
      with Cubes Predicate                              ${standardProxy(withCubesPredicate)}
      with all parameters                               ${standardProxy(withAllParameters)}
      with all parameters, (without specification)      ${standardProxy(withoutSpecification)}

                                                        ${Step(located.clean[SimpleRoot])}
    Domain Proxy is used to find domain objects and submit events over the remote service
      find all                                          ${located(findAll)}
      with search predicate                             ${domainProxy(withSearchPredicate)}
      with limit                                        ${domainProxy(withLimit)}
      with offset                                       ${domainProxy(withOffset)}
      with order                                        ${domainProxy(withOrder)}
      with reverse order                                ${domainProxy(withReverseOrder)}
      count snowflakes                                  ${domainProxy(countSnowflakes)}
      search for snowflakes                             ${domainProxy(searchForSnowflakes)}
      search for snowflake with roots predicate         ${domainProxy(searchForSnowflakeWithRootsPredicate)}
      search for snowflake with snow predicate          ${domainProxy(searchForSnowflakeWithSnowPredicate)}
      events                                            ${domainProxy(events)}
                                                        ${Step(located.close())}
  """

  val located = new located {}
  val standardProxy = located.resolved[StandardProxy]
  val domainProxy = located.resolved[DomainProxy]

  implicit val duration: scala.concurrent.duration.FiniteDuration = scala.concurrent.duration.FiniteDuration(1,
    "seconds")

  private val toSearchFor = rString(100000)
  private val numOfRoots = 27
  private val indexedNamedSimpleRoots = for (i <- 1 to numOfRoots) yield SimpleRoot(i = i, s = toSearchFor)
  private val numberOfOddNamedSimpleRoots = indexedNamedSimpleRoots.count(_.i % 2 == 0)
  private val simpleRoots = for (i <- 1 to numOfRoots) yield SimpleRoot(rInt, rFloat, rName)

  def resolve = { standardProxy: StandardProxy =>
    standardProxy.isInstanceOf[StandardProxy] must beTrue
  }

  def insertSimpleRootWithStandardProxy = { standardProxy: StandardProxy =>
    val simpleRoots = for (i <- 1 to numOfRoots) yield SimpleRoot(rInt, rFloat, rName)
    standardProxy.persist(simpleRoots, null, null).map(_.size) must beEqualTo(numOfRoots).await(1, duration)
  }

  def insertBaseWithStandardProxy = { implicit locator: ServiceLocator =>
    val er = EmptyRoot().create()
    val baseRoots = for (i <- 1 to numOfRoots) yield BaseRoot(root = er)

    locator.resolve[StandardProxy].persist(baseRoots, null, null).map(_.size) must beEqualTo(numOfRoots).await(1,
      duration)
  }

  def updateWithStandardProxy = { locator: ServiceLocator =>
    val simpleRoots = for (i <- 1 to numOfRoots) yield SimpleRoot(rInt, rFloat, rName)
    val standardProxy: StandardProxy = locator.resolve[StandardProxy]
    val domainProxy: DomainProxy = locator.resolve[DomainProxy]
    standardProxy.persist(simpleRoots, null, null).flatMap {
      uris =>
        domainProxy.find[SimpleRoot](uris).flatMap {
          simpleRootList =>

            simpleRootList.foreach {
              simpleRoot => simpleRoot.i += 1
            }
            standardProxy.update(simpleRootList)
        }
    } must beEqualTo(()).await(1, duration)
  }

  def delete = { locator: ServiceLocator =>
    val simpleRoots = for (i <- 1 to numOfRoots) yield SimpleRoot(rInt, rFloat, rName)
    val standardProxy: StandardProxy = locator.resolve[StandardProxy]
    val domainProxy: DomainProxy = locator.resolve[DomainProxy]
    val fInsertedUris = standardProxy.persist(simpleRoots, null, null)
    val fDeletedUris: Future[IndexedSeq[String]] = fInsertedUris.flatMap(domainProxy.find[SimpleRoot]).flatMap(
      standardProxy.persist(null, null, _))

    val fFoundRoots = fDeletedUris.flatMap { _ => fInsertedUris.flatMap(domainProxy.find[SimpleRoot])}

    fFoundRoots.map(_.isEmpty) must beTrue.await(1, duration)
  }

  def canCallHistory = { implicit locator: ServiceLocator =>
    val sr = SimpleRoot(rInt, rFloat)
    sr.create
    sr.i = rInt
    sr.update

    val reportingProxy = locator.resolve[ReportingProxy]

    val history = reportingProxy.getHistory[SimpleRoot](List(sr.URI))

    history.map {
      _.seq.size
    } must equalTo(1).await(1, duration)
  }

  def withoutPredicate = { standardProxy: StandardProxy =>
    val numOfNames = 4
    val names = for (i <- 1 to 4) yield rName
    val myName = names(0)
    val sr = for (i <- 0 to numOfRoots) yield SimpleRoot(rInt, rFloat, names(rInt(numOfNames)))

    await(standardProxy.persist(sr, null, null))

    val dimensions = SimpleCube.s :: SimpleCube.e :: Nil
    val facts = SimpleCube.max_i :: SimpleCube.min_i :: SimpleCube.min_f :: Nil
    val order = Map(SimpleCube.s -> true)

    val fCube = standardProxy.olapCube[com.dslplatform.test.simple.SimpleCube.type, SimpleRoot, Map[String, Any]](
      specification = None,
      dimensions = dimensions,
      facts = facts,
      None,
      None,
      order = order)

    await(fCube.map {
      cube =>
        val firstResult = cube.filter { m => m("s") == myName}
        val mySrs = sr.filter(_.s == myName)

        firstResult.size === 1
        val elem = firstResult.head
        (elem("max_i") === max_i(mySrs)) &
          (elem.get("min_i") must beSome(min_i(mySrs))) &
          (elem.get("min_f").get.isInstanceOf[Double] must beTrue)
    })
  }

  def withPredicate = { standardProxy: StandardProxy =>
    val numOfNames = 4
    val names = for (i <- 1 to 4) yield rName
    val myName = names(0)
    val sr = for (i <- 0 to numOfRoots) yield SimpleRoot(rInt, rFloat, names(rInt(numOfNames)))

    await(standardProxy.persist(sr, null, null))

    val specification = SimpleRoot.oddWithS(myName)
    val dimensions = SimpleCube.s :: SimpleCube.e :: Nil
    val facts = SimpleCube.max_i :: SimpleCube.min_i :: SimpleCube.min_f :: Nil

    val fCube = standardProxy.olapCube[com.dslplatform.test.simple.SimpleCube.type, SimpleRoot](specification,
      dimensions, facts)

    await(fCube.map {
      cube =>
        cube.head.get("min_f") must beSome(min_f(sr.filter(sr => sr.s == myName && sr.i % 2 == 0))) // todo - fails nearly
        (cube.size === 1) &
          (cube.head.get("max_i") must beSome(max_i(sr.filter(sr => sr.s == myName && sr.i % 2 == 0))))
    })
  }

  def withRootsPredicate = { standardProxy: StandardProxy =>
    val numOfNames = 4
    val names = for (i <- 1 to 4) yield rName
    val sr = for (i <- 0 to numOfRoots) yield SimpleRoot(rInt, rFloat, names(rInt(numOfNames)))

    await(standardProxy.persist(sr, null, null))

    val specification = SimpleRoot.odd()
    val dimensions = SimpleCube.s :: SimpleCube.e :: Nil
    val facts = SimpleCube.max_i :: SimpleCube.min_i :: SimpleCube.min_f :: Nil

    val fCube = standardProxy.olapCube[com.dslplatform.test.simple.SimpleCube.type, SimpleRoot](specification,
      dimensions, facts)

    await(fCube.map {
      _.toIndexedSeq.map {
        _("max_i").asInstanceOf[Int] % 2 === 0
      }
    })
  }

  def withCubesPredicate = { standardProxy: StandardProxy =>
    val numOfNames = 4
    val names = for (i <- 1 to 4) yield rName
    val sr = for (i <- 0 to numOfRoots) yield SimpleRoot(rInt, rFloat, names(rInt(numOfNames)))

    await(standardProxy.persist(sr, null, null))

    val specification = SimpleCube.oddInCube()
    val dimensions = SimpleCube.s :: SimpleCube.e :: Nil
    val facts = SimpleCube.max_i :: SimpleCube.min_i :: SimpleCube.min_f :: Nil

    val fCube = standardProxy.olapCube[com.dslplatform.test.simple.SimpleCube.type, SimpleRoot](specification,
      dimensions, facts)

    await(fCube.map {
      cube => cube.toIndexedSeq.map {
        _("max_i").asInstanceOf[Int] % 2 === 0
      }
    })
  }

  def withAllParameters = { standardProxy: StandardProxy =>
    val numOfNames = 4
    val names = for (i <- 1 to 4) yield rName
    val sr = for (i <- 0 to numOfRoots) yield SimpleRoot(rInt, rFloat, names(rInt(numOfNames)))

    await(standardProxy.persist(sr, null, null))

    val specification = SimpleCube.oddInCube()
    val dimensions = SimpleCube.s :: SimpleCube.e :: Nil
    val facts = SimpleCube.max_i :: SimpleCube.min_i :: SimpleCube.min_f :: Nil
    val limit = 1
    val offset = 1
    val order = Map("s" -> true)

    val fCube = standardProxy.olapCube[com.dslplatform.test.simple.SimpleCube.type, SimpleRoot, Map[String, Any]](
      Some(specification), dimensions, facts, Some(limit), Some(offset), order)

    await(fCube.map {
      cube => cube.size === 1
    })
  }

  def withoutSpecification = { standardProxy: StandardProxy =>
    val numOfNames = 4
    val names = for (i <- 1 to 4) yield rName
    val sr = for (i <- 0 to numOfRoots) yield SimpleRoot(rInt, rFloat, names(rInt(numOfNames)))

    await(standardProxy.persist(sr, null, null))

    val dimensions = SimpleCube.s :: SimpleCube.e :: Nil
    val facts = SimpleCube.max_i :: SimpleCube.min_i :: SimpleCube.min_f :: Nil
    val limit = 1
    val offset = 1
    val order = Map("s" -> true)

    val fCube = standardProxy.olapCube[com.dslplatform.test.simple.SimpleCube.type, SimpleRoot, Map[String, Any]](None,
      dimensions, facts, Some(limit), Some(offset), order)

    await(fCube.map {
      cube => cube.size === 1
    })
  }

  def executeCommand = {
    pending
  }

  def findAll = { locator: ServiceLocator =>
    val futureSRuris = locator.resolve[PersistableRepository[SimpleRoot]].insert(indexedNamedSimpleRoots ++ simpleRoots)
    val domainProxy = locator.resolve[DomainProxy]
    val foundRoots = futureSRuris.flatMap(_ => domainProxy.search[SimpleRoot]())

    foundRoots.map(_.size) must beGreaterThanOrEqualTo(numOfRoots).await
  }

  def withSearchPredicate = { domainProxy: DomainProxy =>
    domainProxy.search(SimpleRoot.oddWithS(toSearchFor)).map(_.size) must beEqualTo(numberOfOddNamedSimpleRoots).await
  }

  def withLimit = { domainProxy: DomainProxy =>
    domainProxy.search(SimpleRoot.oddWithS(toSearchFor), limit = 5).map(_.size) must beEqualTo(5).await
  }

  def withOffset = { domainProxy: DomainProxy =>
    val offset = 5
    domainProxy.search(Some(SimpleRoot.oddWithS(toSearchFor)), offset = Some(offset)).map(_.size) must beEqualTo(
      numberOfOddNamedSimpleRoots - offset).await
  }

  def withOrder = { domainProxy: DomainProxy =>
    await(
      domainProxy.search(Some(SimpleRoot.odd()), order = Map(("s", true))).map(
        remote => min_s(remote) === remote.last.s))
  }

  def withReverseOrder = { domainProxy: DomainProxy =>
    await(
      domainProxy.search(Some(SimpleRoot.odd()), order = Map(("s", false))).map(
        remote => min_s(remote) === remote.head.s))
  }

  def countSnowflakes = { domainProxy: DomainProxy =>
    await(domainProxy.count[SimpleSnow](None).map {
      _ === (numOfRoots * 2)
    })
  }

  def searchForSnowflakes = { domainProxy: DomainProxy =>
    await(domainProxy.search[SimpleSnow]().map {
      _.size === numOfRoots * 2
    })
  }

  def searchForSnowflakeWithRootsPredicate = { domainProxy: DomainProxy =>
    /*
    val specification: Specification[SimpleRoot] = SimpleRoot.oddWithS(s = toSearchFor)
    domainProxy.search[SimpleSnow](specification).map{_.size} must beEqualTo(numOfRoots).await()
    */
    pending //Does not compile!
  }

  def searchForSnowflakeWithSnowPredicate = { domainProxy: DomainProxy =>
    val specification = SimpleSnow.oddWithSInSnow(s = toSearchFor)
    await(domainProxy.search[SimpleSnow](specification).map {
      _.size === numberOfOddNamedSimpleRoots
    })
  }

  def events = { domainProxy: DomainProxy =>
    pending
  }

  def max_i(sr: Traversable[SimpleRoot]) = sr.maxBy(_.i).i
  def min_i(sr: Traversable[SimpleRoot]) = sr.minBy(_.i).i
  def min_s(sr: Traversable[SimpleRoot]) = sr.minBy(_.s).s
  def min_f(sr: Traversable[SimpleRoot]) = sr.minBy(_.f).f
}
