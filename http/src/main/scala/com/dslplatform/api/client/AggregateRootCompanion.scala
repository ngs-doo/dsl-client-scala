package com.dslplatform.api.client

import com.dslplatform.api.patterns.ServiceLocator
import com.dslplatform.api.patterns.Specification
import scala.concurrent.Await
import scala.concurrent.duration.Duration

abstract class AggregateRootCompanion[TRoot <: com.dslplatform.api.patterns.AggregateRoot: scala.reflect.ClassTag] {

  private def domainProxy(locator: ServiceLocator): DomainProxy = locator.resolve[DomainProxy]
  private def crudProxy(locator: ServiceLocator): CrudProxy = locator.resolve[CrudProxy]

  def find(uri: String)(implicit locator: ServiceLocator, duration: Duration): TRoot = Await.result[TRoot](crudProxy(locator).read[TRoot](uri), duration)

  def find(uris: TraversableOnce[String])(implicit locator: ServiceLocator, duration: Duration): IndexedSeq[TRoot] = Await.result(domainProxy(locator).find[TRoot](uris), duration)

  def findAll(implicit locator: ServiceLocator, duration: Duration): IndexedSeq[TRoot] = Await.result[IndexedSeq[TRoot]](domainProxy(locator).search[TRoot](), duration)

  def findAll(limit: Int, offset: Int)(implicit locator: ServiceLocator, duration: Duration): IndexedSeq[TRoot] = Await.result(domainProxy(locator).search[TRoot](limit = Some(limit), offset = Some(offset)), duration)

  def search(specification: Specification[TRoot])(implicit locator: ServiceLocator, duration: Duration): IndexedSeq[TRoot] = Await.result(domainProxy(locator).search(specification = Some(specification)), duration)

  def search(specification: Specification[TRoot], limit: Int, offset: Int)(implicit locator: ServiceLocator, duration: Duration): IndexedSeq[TRoot] = Await.result(domainProxy(locator).search(specification = Some(specification), limit = Some(limit), offset = Some(offset)), duration)

  def count(implicit locator: ServiceLocator, duration: Duration): Long = Await.result(domainProxy(locator).count[TRoot](), duration)

  def count(specification: Specification[TRoot])(implicit locator: ServiceLocator, duration: Duration): Long = Await.result(domainProxy(locator).count(specification = Some(specification)), duration)
}
