import org.specs2._
import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner
import com.dslplatform.test.simple.SimpleRoot
import com.dslplatform.api.patterns.{ServiceLocator, PersistableRepository}
import com.dslplatform.test.complex.EmptyRoot
import com.dslplatform.test.complex.BaseRoot
import org.specs2.specification.Step

@RunWith(classOf[JUnitRunner])
class RootRepositoryTest extends Specification with Common {

  def is = sequential ^ s2"""
    Simple root repository
      insert 1                            ${located(insert1)}
      insert 2                            ${located(insert2)}
      insert 20                           ${located(insert20)}
      persist base                        ${located(persistBase)}
      find all, delete all (Simple)       ${located(findAllDeleteAllSimple)}
      find all, delete all (Base)         ${located(findAllDeleteAllBase)}
                                          ${Step(located.close())}
  """

  val located = new located {}

  def insert1 = { locator: ServiceLocator =>
    val sr = SimpleRoot(1, 2f, "3")

    locator.resolve[PersistableRepository[SimpleRoot]].insert(sr) should not(beEqualTo(null)).await(1, duration10)
  }

  def insert2 = { locator: ServiceLocator =>
    val srs = SimpleRoot(1, 2f, "3") :: SimpleRoot(4, 5f, "6") :: Nil

    val fsruri = locator.resolve[PersistableRepository[SimpleRoot]].insert(srs)

    fsruri must not(beEqualTo(null)).await(1, duration10)
    fsruri.map {
      _.size
    } must beEqualTo(2).await(1, duration10)
  }

  def insert20 = { locator: ServiceLocator =>
    val numOfRoots = 20
    val simpleRoots = for (i <- 1 to numOfRoots) yield SimpleRoot(i, i, i.toString)

    locator.resolve[PersistableRepository[SimpleRoot]].insert(simpleRoots).map(_.size) must beEqualTo(numOfRoots).await
  }

  def persistBase = { implicit locator: ServiceLocator =>
    val numOfRoots = 13
    val baseRoots = for (i <- 1 to numOfRoots) yield BaseRoot(root = EmptyRoot().create())
    locator.resolve[PersistableRepository[BaseRoot]].insert(baseRoots).map(_.size) must beEqualTo(numOfRoots).await
  }

  def findAllDeleteAllSimple = { locator: ServiceLocator =>
    val simpleRootRepository = locator.resolve[PersistableRepository[SimpleRoot]]
    await(
      simpleRootRepository.search().flatMap {
        roots =>
          simpleRootRepository.delete(roots).flatMap {
            _ =>
              simpleRootRepository.search().map {
                roots => roots should be size 0
              }
          }
      })
  }

  def findAllDeleteAllBase = { locator: ServiceLocator =>
    val baseRootRepository = locator.resolve[PersistableRepository[BaseRoot]]
    await(
      baseRootRepository.search().flatMap {
        roots =>
          baseRootRepository.delete(roots).flatMap {
            _ =>
              baseRootRepository.search().map {
                roots => roots should be size 0
              }
          }
      })
  }

}
