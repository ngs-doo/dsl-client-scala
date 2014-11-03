import org.specs2._
import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner
import com.dslplatform.test.simple.SimpleRoot
import com.dslplatform.api.patterns.{ServiceLocator, PersistableRepository, SearchableRepository, Repository}
import com.dslplatform.test.simple.SimpleSnow
import com.dslplatform.test.simple.Self
import com.dslplatform.test.simple.Selfy
import org.specs2.specification.Step

@RunWith(classOf[JUnitRunner])
class SnowflakeTest extends Specification with Common {

  def is = sequential ^ s2"""
    SimpleSnowflake
                                          ${Step(located.clean[SimpleRoot])}
      query repository                    ${located(queryRepository)}
      search over roots specification     ${located(searchOverRootsSpecification)}
      search over snows specification     ${located(searchOverSnowsSpecification)}
      self reference                      ${located(selfReference)}
                                          ${Step(located.close())}
    """

  private val numOfRoots = 13
  private val numOfNames = 4
  private val names = for (i <- 1 to 4) yield rName
  private val myName = names(0)
  private val arrSR = for (i <- 0 to numOfRoots - 1) yield SimpleRoot(rInt, rFloat, names(rInt(numOfNames)))

  private def countOdds(e: Int*) = e.count(_ % 2 == 0)

  val located = new located {}

  def queryRepository = { implicit locator: ServiceLocator =>
    await(locator.resolve[PersistableRepository[SimpleRoot]].insert(arrSR))
    val snow = await(locator.resolve[SearchableRepository[SimpleSnow]].search())
    snow.size === numOfRoots
  }

  def searchOverRootsSpecification = { implicit locator: ServiceLocator =>
    /*
          val specification = SimpleRoot.withS(myName)
          val snow = await(locator.resolve[SearchableRepository[SimpleSnow]].search(specification))

          snow.size === numOfRoots
    */
    failure
  }

  def searchOverSnowsSpecification = { implicit locator: ServiceLocator =>

    val specification = SimpleSnow.withSInSnow(myName)
    val snow = await(locator.resolve[SearchableRepository[SimpleSnow]].search(specification))
    snow.size === arrSR.filter(_.s == myName).size
  }

  def selfReference = { implicit locator: ServiceLocator =>

    val sr1 = Self().create
    val sr2 = Self(self = Some(sr1)).create
    val sr3 = Self(self = Some(sr2)).create
    val rep = locator.resolve[Repository[Selfy]]
    val selfs = await(rep.find(Seq(sr1.URI, sr2.URI, sr3.URI)))

    selfs.size === 3
    selfs(0).slim.size === 0
    selfs(0).fat.size === 0
    selfs(1).slim.size === 1
    selfs(1).fat.size === 1
    selfs(1).slim(0) == sr1.ID
    selfs(2).slim.size === 2
    selfs(2).fat.size === 2
    selfs(2).slim(0) == sr2.ID
    selfs(2).slim(1) == sr1.ID
  }
}
