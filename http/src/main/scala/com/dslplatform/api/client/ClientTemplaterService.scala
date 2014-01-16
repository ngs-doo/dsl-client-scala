package com.dslplatform.api
package client

import scala.concurrent.Future
import scala.reflect.ClassTag
import patterns.TemplaterService
import patterns.Identifiable
import patterns.Searchable
import patterns.Specification

class ClientTemplaterService(proxy: ReportingProxy)
    extends TemplaterService {

  def populate[TIdentifiable <: Identifiable](
    file: String,
    aggregate: TIdentifiable): Future[Array[Byte]] =
    proxy.findTemplater(file, aggregate.URI, false)

  def populatePdf[TIdentifiable <: Identifiable](
    file: String,
    domainObject: TIdentifiable): Future[Array[Byte]] =
    proxy.findTemplater(file, domainObject.URI, true)

  def populate[TSearchable <: Searchable: ClassTag](
    file: String,
    specification: Option[Specification[TSearchable]]): Future[Array[Byte]] =
    proxy.searchTemplater(file, specification, false)

  def populatePdf[TSearchable <: Searchable: ClassTag](
    file: String,
    specification: Option[Specification[TSearchable]]) =
    proxy.searchTemplater(file, specification, true)
}
