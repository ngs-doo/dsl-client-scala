package com.dslplatform.test

import scala.reflect.ClassTag
import scala.reflect.runtime.universe

import org.specs2.mutable._
import org.specs2.specification.Step

import com.dslplatform.api.client.ReportingProxy
import com.dslplatform.api.patterns.AggregateRoot
import com.dslplatform.api.patterns.PersistableRepository
import com.dslplatform.api.patterns.ServiceLocator
import com.dslplatform.test.simple.SimpleRoot

class HistoryTest extends Specification with Common {
  override def is = s2"""
    History test
      Single CRUD (via active record)      ${located(singleCrudViaActiveRecord)}
      Multiple CRUD (via repositories)     ${located(multipleCrudViaRepositories)}
                                           ${Step(located.close())}
    """

  val located = new Located

  def singleCrudViaActiveRecord = { implicit locator: ServiceLocator =>
    val reportingProxy = locator.resolve[ReportingProxy]

    def getHistory[T <: AggregateRoot: ClassTag](root: T) =
      await(reportingProxy.getHistory[T](Seq(root.URI))).head

    // CREATE / INSERT
    val sr1 = SimpleRoot()
    sr1.i = 1
    sr1.create()

    locally {
      val h1 = getHistory(sr1)
      h1.snapshots.size === 1
      h1.snapshots(0).action === "INSERT"
      h1.snapshots(0).value.i === sr1.i
    }

    // READ / FIND
    val sr2 = SimpleRoot.find(sr1.URI)

    // UPDATE
    sr2.i = 2
    sr2.update()

    locally {
      val h2 = getHistory(sr2)
      h2.snapshots.size === 2
      h2.snapshots(0).action === "INSERT"
      h2.snapshots(0).value.i === sr1.i
      h2.snapshots(1).action === "UPDATE"
      h2.snapshots(1).value.i === sr2.i
    }

    // DELETE
    val sr3 = SimpleRoot.find(sr2.URI)
    sr3.delete()

    locally {
      val h3 = getHistory(sr3)
      h3.snapshots.size === 3
      h3.snapshots(0).action === "INSERT"
      h3.snapshots(0).value.i === sr1.i
      h3.snapshots(1).action === "UPDATE"
      h3.snapshots(1).value.i === sr2.i
      h3.snapshots(2).action === "DELETE"
      h3.snapshots(2).value.i === sr3.i
    }
  }

  def multipleCrudViaRepositories = { implicit locator: ServiceLocator =>
    val repository = locator.resolve[PersistableRepository[SimpleRoot]]
    val reportingProxy = locator.resolve[ReportingProxy]

    // CREATE / INSERT
    val srs1 = (1 to 10) map { i => SimpleRoot(i = 100 + i) }
    val uris = await(repository.insert(srs1))

    locally {
      val h1s = await(reportingProxy.getHistory[SimpleRoot](uris))
      h1s.size === srs1.size
      for ((h1, sr1) <- h1s zip srs1) {
        h1.snapshots.size === 1
        h1.snapshots(0).action === "INSERT"
        h1.snapshots(0).value.i === sr1.i
      }
    }

    // READ / FIND
    val srs2 = await(repository.find(uris))

    // UPDATE
    srs2 foreach { _.i += 100 }
    await(repository.update(srs2))

    locally {
      val h2s = await(reportingProxy.getHistory[SimpleRoot](uris))
      h2s.size === srs2.size
      for (((h2, sr1), sr2) <- h2s zip srs1 zip srs2) {
        h2.snapshots.size === 2
        h2.snapshots(0).action === "INSERT"
        h2.snapshots(0).value.i === sr1.i
        h2.snapshots(1).action === "UPDATE"
        h2.snapshots(1).value.i === sr2.i
      }
    }

    // DELETE
    val srs3 = await(repository.find(uris))
    await(repository.delete(srs3))

    locally {
      val h2s = await(reportingProxy.getHistory[SimpleRoot](uris))
      h2s.size === srs3.size
      for ((((h3, sr1), sr2), sr3) <- h2s zip srs1 zip srs2 zip srs3) {
        h3.snapshots.size === 3
        h3.snapshots(0).action === "INSERT"
        h3.snapshots(0).value.i === sr1.i
        h3.snapshots(1).action === "UPDATE"
        h3.snapshots(1).value.i === sr2.i
        h3.snapshots(2).action === "DELETE"
        h3.snapshots(2).value.i === sr3.i
      }
    }

    success
  }
}
