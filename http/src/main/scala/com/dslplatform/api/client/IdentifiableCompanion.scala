package com.dslplatform.api.client

import com.dslplatform.api.patterns.ServiceLocator
import com.dslplatform.api.patterns.Specification
import com.dslplatform.api.patterns.Identifiable

import scala.concurrent.Await
import scala.concurrent.duration.Duration

abstract class IdentifiableCompanion[T <: Identifiable: scala.reflect.ClassTag]
    extends SearchableCompanion[T] {

  private def crudProxy(locator: ServiceLocator): CrudProxy = locator.resolve[CrudProxy]

  def find(uri: String)(implicit locator: ServiceLocator, duration: Duration): T =
    Await.result(crudProxy(locator).read[T](uri), duration)

  def find(uris: String*)(implicit locator: ServiceLocator, duration: Duration): IndexedSeq[T] =
    Await.result(domainProxy(locator).find[T](uris), duration)

  def find(uris: TraversableOnce[String])(implicit locator: ServiceLocator, duration: Duration): IndexedSeq[T] =
    Await.result(domainProxy(locator).find[T](uris), duration)
}
