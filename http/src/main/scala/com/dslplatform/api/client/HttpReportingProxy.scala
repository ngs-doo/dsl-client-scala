package com.dslplatform.api.client

import com.dslplatform.api.patterns.AggregateRoot
import com.dslplatform.api.patterns.Cube
import com.dslplatform.api.patterns.History
import com.dslplatform.api.patterns.Identifiable
import com.dslplatform.api.patterns.Report
import com.dslplatform.api.patterns.Searchable
import com.dslplatform.api.patterns.Specification

import scala.reflect.ClassTag
import scala.concurrent.Future

class HttpReportingProxy(httpClient: HttpClient)
    extends ReportingProxy {

  import HttpClientUtil._

  private val ReportingUri = "Reporting.svc"

  def populate[TResult: ClassTag](report: Report[TResult]): Future[TResult] = {
    val domainName: String = httpClient.getDslName(report.getClass)
    httpClient.sendRequest[TResult](
      PUT(report),
      ReportingUri / "report" / domainName,
      Set(200))
  }

  def createReport[TResult](
      report: Report[TResult],
      templater: String): Future[Array[Byte]] =
    httpClient.sendRawRequest(
      PUT(report),
      ReportingUri / "report" / httpClient.getDslName(report.getClass) / templater,
      Set(201),
      Map("Accept" -> Set("application/octet-stream")))

  def olapCube[TCube <: Cube[TSearchable]: ClassTag, TSearchable <: Searchable: ClassTag](
      templater: String,
      specification: Option[Specification[TSearchable]],
      dimensions: TraversableOnce[String],
      facts: TraversableOnce[String],
      limit: Option[Int],
      offset: Option[Int],
      order: Map[String, Boolean]): Future[Array[Byte]] = {
    val cubeName = httpClient.getDslName[TCube]
    val parentName: String = httpClient.getDslName[TSearchable]
    val args: String = Utils.buildOlapArguments(dimensions, facts, limit, offset, order)
    specification match {
      case Some(spec) =>
        val specClass = spec.getClass
        val specName: String = if (parentName == cubeName) parentName + "/" else ""
        httpClient.sendRawRequest(
          PUT(specification),
          ReportingUri / "olap" / cubeName / specName + specClass.getSimpleName.replace("$", "") / templater + args,
          Set(200))
      case _ =>
        httpClient.sendRawRequest(
          GET, ReportingUri / "olap" / cubeName / templater + args, Set(200))
    }
  }

  def getHistory[TAggregate <: AggregateRoot: ClassTag](
      uris: TraversableOnce[String]): Future[IndexedSeq[History[TAggregate]]] = {
    httpClient.sendRequest[IndexedSeq[History[TAggregate]]](
      PUT(uris.toArray),
      ReportingUri / "history" / httpClient.getDslName,
      Set(200))
  }

  def findTemplater[TIdentifiable <: Identifiable: ClassTag](
      file: String,
      uri: String,
      toPdf: Boolean): Future[Array[Byte]] = {
    if (file == null) throw new IllegalArgumentException("file not specified")
    if (uri == null) throw new IllegalArgumentException("uri not specified")
    val domainName = httpClient.getDslName
    httpClient.sendRawRequest(
      GET,
      ReportingUri / "templater" / file / domainName + "?uri=" + encode(uri),
      Set(200),
      prepareHeaders(toPdf))
  }

  private def prepareHeaders(toPdf: Boolean) =
    Map("Accept" -> Set(if (toPdf) "application/pdf" else "application/octet-stream"))

  def searchTemplater[TSearchable <: Searchable: ClassTag](
      file: String,
      specification: Option[Specification[TSearchable]],
      toPdf: Boolean): Future[Array[Byte]] = {
    if (file == null || file.isEmpty) throw new IllegalArgumentException("file not specified")
    val domainName: String = httpClient.getDslName
    specification match {
      case Some(spec) =>
        val specClass: Class[_] = spec.getClass
        httpClient.sendRawRequest(
          PUT(specification),
          ReportingUri / "templater" / file / domainName / specClass.getSimpleName.replace("$", ""),
          Set(200),
          prepareHeaders(toPdf))
      case _ =>
        httpClient.sendRawRequest(
          GET,
          ReportingUri / "templater" / file / domainName,
          Set(200),
          prepareHeaders(toPdf))
    }
  }
}
