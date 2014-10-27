package com.dslplatform.api.client

import com.dslplatform.api.patterns.AggregateDomainEvent
import com.dslplatform.api.patterns.AggregateRoot
import com.dslplatform.api.patterns.DomainEvent
import com.dslplatform.api.patterns.Identifiable
import com.dslplatform.api.patterns.Searchable
import com.dslplatform.api.patterns.Specification

import scala.reflect.ClassTag
import scala.concurrent.Future

class HttpDomainProxy(httpClient: HttpClient)
    extends DomainProxy {

  import HttpClientUtil._

  private val DomainUri = "Domain.svc"

  def find[TIdentifiable <: Identifiable: ClassTag](
      uris: TraversableOnce[String]): Future[IndexedSeq[TIdentifiable]] = {
    if (uris.isEmpty) Future successful IndexedSeq.empty[TIdentifiable]
    else
      httpClient.sendRequestForCollection[TIdentifiable](
        PUT(uris.toArray),
        DomainUri / "find" / httpClient.getDslName,
        Set(200))
  }

  def search[TSearchable <: Searchable: ClassTag](
      specification: Option[Specification[TSearchable]],
      limit: Option[Int],
      offset: Option[Int],
      order: Map[String, Boolean]): Future[IndexedSeq[TSearchable]] = {
    val parentName: String = httpClient.getDslName
    specification match {
      case Some(spec) =>
        val specClass: Class[_] = spec.getClass
        val specificationName = specClass.getSimpleName.replace("$", "")
        val urlParams: String =
          Utils.buildArguments(
            Some(specificationName),
            limit,
            offset,
            order)
        httpClient.sendRequestForCollection(
          PUT(specification),
          DomainUri / "search" / parentName + urlParams,
          Set(200))
      case _ =>
        val urlParams: String = Utils.buildArguments(None, limit, offset, order)
        httpClient.sendRequestForCollection(
          GET,
          DomainUri / "search" / parentName + urlParams,
          Set(200))
    }
  }

  def count[TSearchable <: Searchable: ClassTag](specification: Option[Specification[TSearchable]]): Future[Long] = {
    val parentName: String = httpClient.getDslName
    specification match {
      case Some(spec) =>
        val specClass: Class[_] = specification.getClass
        httpClient.sendRequest[Long](
          PUT(specification),
          DomainUri / "count" / parentName / specClass.getSimpleName.replace("$", ""),
          Set(200))
      case _ =>
        httpClient.sendRequest[Long](
          GET,
          DomainUri / "count" / parentName,
          Set(200))
    }
  }

  def submit[TEvent <: DomainEvent](domainEvent: TEvent): Future[String] = {
    val domainName: String = httpClient.getDslName(domainEvent.getClass)
    httpClient.sendRequest[String](
      POST(domainEvent), DomainUri / "submit" / domainName, Set(201))
  }

  def submit[TAggregate <: AggregateRoot: ClassTag, TEvent <: AggregateDomainEvent[TAggregate]](
      domainEvent: TEvent,
      uri: String): Future[TAggregate] = {
    val eventClazz: Class[_] = domainEvent.getClass
    val domainName: String = httpClient.getDslName
    httpClient.sendRequest[TAggregate](
      POST(domainEvent),
      DomainUri / "submit" / domainName / eventClazz.getSimpleName.replace("$", "") + "?uri=" + encode(uri),
      Set(201))
  }
}
