
import org.specs2._
import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner
import com.dslplatform.test.snapshottest._
import com.dslplatform.api.patterns.{ServiceLocator, PersistableRepository}
import org.specs2.specification.Step

@RunWith(classOf[JUnitRunner])
class SnapshotTest extends Specification with Common{

  def is = s2"""
    Snapshots will ...
      Persist Snapshot              ${located(persistSnapshot)}
      Persist Snapshot Collection   ${located(persistSnapshotCollection)}
                                ${Step(located.close())}
  """

  val located = new located {}

  def persistSnapshot  = { implicit locator : ServiceLocator =>
    val name1 = rName
    val name2 = rName
    val sr = SimpleRoot(rInt, rFloat, rDouble, name1)
    sr.create
    val srr = SimpleRootReferent(Some(sr))
    srr.create
    val srClone = sr.copy()
    sr.s = name2
    sr.update

    val remoteReferent = SimpleRootReferent.find(srr.URI)

    remoteReferent.sr.get === srClone
  }

  def persistSnapshotCollection = { implicit locator : ServiceLocator =>
    val name1 = rName
    val name2 = rName

    val srs = simpleRoots(name1)
    val simplerRootRepository = locator.resolve[PersistableRepository[SimpleRoot]]
    val persistedSrs = await(simplerRootRepository.insert(srs).flatMap(simplerRootRepository.find))

    val srr = SimpleRootReferent(None, persistedSrs.toArray)
    srr.create

    persistedSrs.foreach(_.s = rName)
    await(simplerRootRepository.update(persistedSrs))

    SimpleRootReferent.find(srr.URI).srs.map{ _.s must beEqualTo(name1) }.toIndexedSeq
  }

  private val numOfRoots = 27
  private def simpleRoots(name: String) = for (i <- 1 to numOfRoots) yield SimpleRoot(rInt, rFloat, rDouble, name)
}
