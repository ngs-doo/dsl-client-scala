package com.dslplatform.api.client

import com.dslplatform.api.patterns.ServiceLocator
import com.dslplatform.api.patterns.Specification

import scala.concurrent.Await
import scala.concurrent.duration.Duration

abstract class AggregateRootCompanion[TRoot <: com.dslplatform.api.patterns.AggregateRoot: scala.reflect.ClassTag]
    extends SearchableCompanion[TRoot] {

  private def crudProxy(locator: ServiceLocator): CrudProxy = locator.resolve[CrudProxy]

  def find(uri: String)(implicit locator: ServiceLocator, duration: Duration): TRoot = Await.result[TRoot](crudProxy(locator).read[TRoot](uri), duration)

  def find(uris: TraversableOnce[String])(implicit locator: ServiceLocator, duration: Duration): IndexedSeq[TRoot] = Await.result(domainProxy(locator).find[TRoot](uris), duration)
}
