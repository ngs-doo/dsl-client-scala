package com.dslplatform.api.client

import com.dslplatform.api.patterns.Searchable
import com.dslplatform.api.patterns.ServiceLocator
import com.dslplatform.api.patterns.Specification

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.reflect.ClassTag

abstract class SearchableCompanion[TSearchable <: Searchable: ClassTag] {
  protected def domainProxy(locator: ServiceLocator): DomainProxy = locator.resolve[DomainProxy]

  def search(specification: Specification[TSearchable])(implicit locator: ServiceLocator, duration: Duration): IndexedSeq[TSearchable] =
    Await.result(domainProxy(locator).search(specification = Option(specification)), duration)

  def search(specification: Specification[TSearchable], limit: Int, offset: Int)(implicit locator: ServiceLocator, duration: Duration): IndexedSeq[TSearchable] =
    Await.result(domainProxy(locator).search(specification = Option(specification), limit = Some(limit), offset = Some(offset)), duration)

  def count(implicit locator: ServiceLocator, duration: Duration): Long =
    Await.result(domainProxy(locator).count[TSearchable](), duration)

  def count(specification: Specification[TSearchable])(implicit locator: ServiceLocator, duration: Duration): Long =
    Await.result(domainProxy(locator).count(specification = Option(specification)), duration)

  def findAll(implicit locator: ServiceLocator, duration: Duration): IndexedSeq[TSearchable] =
    Await.result[IndexedSeq[TSearchable]](domainProxy(locator).search[TSearchable](), duration)

  def findAll(limit: Int, offset: Int)(implicit locator: ServiceLocator, duration: Duration): IndexedSeq[TSearchable] =
    Await.result(domainProxy(locator).search[TSearchable](limit = Some(limit), offset = Some(offset)), duration)
}
