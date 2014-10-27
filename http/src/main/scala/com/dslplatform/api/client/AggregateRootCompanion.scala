package com.dslplatform.api.client

import com.dslplatform.api.patterns.{ ServiceLocator, AggregateRoot }
import scala.concurrent.duration.Duration
import scala.concurrent.{ Future, ExecutionContext, Await }

/** @tparam TRoot type of an Aggregate Root.
  */
abstract class AggregateRootCompanion[TRoot <: AggregateRoot: scala.reflect.ClassTag]
    extends IdentifiableCompanion[TRoot] {
  def create(t: TRoot)(implicit locator: ServiceLocator, duration: Duration) = Await.result(locator.resolve[CrudProxy].create(t), duration)

  def create(t: TraversableOnce[TRoot])(implicit locator: ServiceLocator, duration: Duration): IndexedSeq[TRoot] = {
    if (t.isEmpty) IndexedSeq.empty[TRoot]
    else {
      implicit val ec = locator.resolve[ExecutionContext]
      val result: Future[IndexedSeq[TRoot]] = locator.resolve[StandardProxy].insert(t).flatMap(locator.resolve[DomainProxy].find(_))
      Await.result(result, duration)
    }
  }

  def update(t: TRoot)(implicit locator: ServiceLocator, duration: Duration) = Await.result(locator.resolve[CrudProxy].update(t), duration)

  def update(t: TraversableOnce[TRoot])(implicit locator: ServiceLocator, duration: Duration): IndexedSeq[TRoot] = {
    if (t.isEmpty) IndexedSeq.empty[TRoot]
    else {
      implicit val ec = locator.resolve[ExecutionContext]
      val result: Future[IndexedSeq[TRoot]] = locator.resolve[StandardProxy].update(t).flatMap(_ => locator.resolve[DomainProxy].find(t.map(_.URI)))
      Await.result(result, duration)
    }
  }

  def delete(t: TRoot)(implicit locator: ServiceLocator, duration: Duration) = Await.result(locator.resolve[CrudProxy].delete(t.URI), duration)

  def delete(t: TraversableOnce[TRoot])(implicit locator: ServiceLocator, duration: Duration): IndexedSeq[TRoot] = {
    if (t.nonEmpty) {
      implicit val ec = locator.resolve[ExecutionContext]
      val result = locator.resolve[StandardProxy].update(t).flatMap(_ => locator.resolve[DomainProxy].find(t.map(_.URI)))
      Await.result(result, duration)
    }
    IndexedSeq.empty[TRoot]
  }
}
