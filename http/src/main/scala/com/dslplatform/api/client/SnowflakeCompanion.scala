package com.dslplatform.api.client

import com.dslplatform.api.patterns.Snowflake
import com.dslplatform.api.patterns.ServiceLocator
import com.dslplatform.api.patterns.Specification
import com.dslplatform.api.patterns.AggregateRoot
import scala.reflect.ClassTag
import scala.concurrent.duration.Duration
import scala.concurrent.Await

abstract class SnowflakeCompanion[TSnowflake <: Snowflake[TAggregate]: ClassTag, TAggregate <: AggregateRoot: ClassTag]
    extends IdentifiableCompanion[TSnowflake] {
  import HttpClientUtil._

  protected def client(locator: ServiceLocator): HttpClient = locator.resolve[HttpClient]

  def searchWith(
      specification: Specification[TAggregate],
      limit: Option[Int] = None,
      offset: Option[Int] = None,
      order: Map[String, Boolean] = Map.empty)(
        implicit locator: ServiceLocator,
        duration: Duration): IndexedSeq[TSnowflake] = {

    require(specification ne null, "specification not provided")

    val httpClient = client(locator)
    val snowflakeName: String = httpClient.getDslName[TSnowflake]
    val entityName: String = httpClient.getDslName[TAggregate]
    val specClass: Class[_] = specification.getClass
    val specificationName = entityName + "%2B" + specClass.getSimpleName.replace("$", "")
    val urlParams: String =
      Utils.buildArguments(
        Some(specificationName),
        limit,
        offset,
        order)
    val result =
      httpClient.sendRequestForCollection[TSnowflake](
        PUT(specification),
        "Domain.svc" / "search" / snowflakeName + urlParams,
        Set(200))
    Await.result(result, duration)
  }
}
