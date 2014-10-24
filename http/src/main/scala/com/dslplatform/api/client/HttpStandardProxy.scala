package com.dslplatform.api.client

import com.dslplatform.api.patterns.AggregateRoot
import com.dslplatform.api.patterns.Cube
import com.dslplatform.api.patterns.Searchable
import com.dslplatform.api.patterns.Specification

import scala.concurrent.Future
import scala.reflect.ClassTag

class HttpStandardProxy(
    httpClient: HttpClient,
    json: JsonSerialization
    ) extends StandardProxy {

  import HttpClientUtil._

  implicit val ec = httpClient.ec

  private case class PersistArg(RootName: String, ToInsert: String, ToUpdate: String, ToDelete: String)

  private case class Pair[T <: AggregateRoot](Key: T, Value: T)

  private val StandardUri    = "Commands.svc"
  private val ApplicationUri = "RestApplication.svc"

  def persist[TAggregate <: AggregateRoot : ClassTag](
      inserts: TraversableOnce[TAggregate],
      updates: TraversableOnce[(TAggregate, TAggregate)],
      deletes: TraversableOnce[TAggregate]): scala.concurrent.Future[IndexedSeq[String]] = {

    val toInsert = if (inserts != null && inserts.nonEmpty) json.serialize(inserts.toArray) else null
    val toUpdate = if (updates != null && updates.nonEmpty) json.serialize(updates.map(t => Pair(t._1, t._2)).toArray) else null
    val toDelete = if (deletes != null && deletes.nonEmpty) json.serialize(deletes.toArray) else null

    if (toInsert == null && toUpdate == null && toDelete == null) Future successful IndexedSeq.empty[String]
    else
      httpClient.sendStandardRequest(
        POST(PersistArg(httpClient.getDslName[TAggregate], toInsert, toUpdate, toDelete)),
        ApplicationUri / "PersistAggregateRoot",
        Set(200, 201))
  }

  def update[TAggregate <: AggregateRoot : ClassTag](
      updates: TraversableOnce[TAggregate]): Future[Unit] =
    persist(updates = updates.map(t => (t, t))).map(_ => ())

  def delete[TAggregate <: AggregateRoot : ClassTag](
      deletes: TraversableOnce[TAggregate]): Future[Unit] =
    persist(deletes = deletes).map(_ => ())

  def olapCube[TCube <: Cube[TSearchable] : ClassTag, TSearchable <: Searchable : ClassTag, TResult: ClassTag](
      specification: Option[Specification[TSearchable]],
      dimensions: TraversableOnce[String],
      facts: TraversableOnce[String],
      limit: Option[Int],
      offset: Option[Int],
      order: Map[String, Boolean]): Future[IndexedSeq[TResult]] = {
    val cubeName = httpClient.getDslName[TCube]
    specification match {
      case Some(spec) =>
        val specClazz = spec.getClass
        val parentName = httpClient.getDslName(specClazz.getEnclosingClass)
        val specName: String = if (parentName != cubeName) parentName + "%2B" else ""
        val specificationName = specName + specClazz.getSimpleName.replace("$", "")
        val url: String =
          StandardUri / "olap" / cubeName +
              Utils.buildOlapArguments(dimensions, facts, limit, offset, order, Some(specificationName))
        httpClient.sendRequestForCollection[TResult](PUT(specification), url, Set(200, 201))
      case _ =>
        val url: String = StandardUri / "olap" / cubeName + Utils.buildOlapArguments(dimensions, facts, limit, offset, order)
        httpClient.sendRequestForCollection[TResult](GET, url, Set(200, 201))
    }
  }

  def execute[TArgument, TResult: ClassTag](
      command: String, argument: TArgument): Future[TResult] =
    httpClient.sendRequest[TResult](POST[TArgument](argument), StandardUri / "execute" / command, Set(200, 201))
}
