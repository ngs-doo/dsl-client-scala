package com.dslplatform.api.client

import com.dslplatform.api.patterns.AggregateRoot
import com.dslplatform.api.patterns.Cube
import com.dslplatform.api.patterns.Searchable
import com.dslplatform.api.patterns.Specification

import scala.concurrent.Future
import scala.reflect.ClassTag

/**
 * Proxy service to various domain operations such as bulk persistence,
 * data analysis and remote service calls.
 * <p>
 * It is preferred to use domain patterns instead of this proxy service.
 */
trait StandardProxy {
  /**
   * Apply local changes to the remote server.
   *
   * @param inserts new aggregate roots
   * @param updates pairs for updating old aggregate to new state
   * @param deletes aggregate roots which will be deleted
   * @return        future uris of newly created aggregates
   */
  def persist[TAggregate <: AggregateRoot : ClassTag](
      inserts: TraversableOnce[TAggregate] = Nil,
      updates: TraversableOnce[(TAggregate, TAggregate)] = Nil,
      deletes: TraversableOnce[TAggregate] = Nil): Future[IndexedSeq[String]]

  /**
   * Helper method for persist.
   * Apply local changes to the remote server.
   *
   * @param inserts new aggregate roots
   * @return        future uris of newly created aggregates
   */
  def insert[TAggregate <: AggregateRoot : ClassTag](
      inserts: TraversableOnce[TAggregate]): Future[IndexedSeq[String]] =
    persist(inserts)

  /**
   * Helper method for persist.
   * Apply local changes to the remote server.
   *
   * @param updates aggregate roots to update
   * @return        empty future which completes when done.
   */
  def update[TAggregate <: AggregateRoot : ClassTag](
      updates: TraversableOnce[TAggregate]): Future[_] =
    persist(updates = updates.map(t => (t, t)))

  /**
   * Helper method for persist.
   * Apply local changes to the remote server.
   *
   * @param deletes aggregate roots to update
   * @return        empty future which completes when done.
   */
  def delete[TAggregate <: AggregateRoot : ClassTag](
      deletes: TraversableOnce[TAggregate]): Future[_] =
    persist(deletes = deletes)

  /**
   * Perform data analysis on specified data source.
   * Data source is filtered using provided specification.
   * Analysis is performed by grouping data by dimensions
   * and aggregating information using specified facts.
   *
   * @param specification filter data source
   * @param dimensions    group by dimensions
   * @param facts         analyze using facts
   * @param order         custom order for result
   * @return              future with deserialized collection from analysis result
   */
  def olapCube[TCube <: Cube[TSearchable] : ClassTag, TSearchable <: Searchable : ClassTag, TResult: ClassTag](
      specification: Option[Specification[TSearchable]] = None,
      dimensions: TraversableOnce[String] = Nil,
      facts: TraversableOnce[String] = Nil,
      limit: Option[Int] = None,
      offset: Option[Int] = None,
      order: Map[String, Boolean] = Map.empty): Future[IndexedSeq[TResult]]

  /**
   * Helper method for Olap cube.
   * Perform data analysis on specified data source.
   * Data source is filtered using provided specification.
   * Analysis is performed by grouping data by dimensions
   * and aggregating information using specified facts.
   *
   * @param specification filter data source
   * @param dimensions    group by dimensions
   * @param facts         analyze using facts
   * @return              future with deserialized collection from analysis result
   */
  def olapCube[TCube <: Cube[TSearchable] : ClassTag, TSearchable <: Searchable : ClassTag](
      specification: Specification[TSearchable],
      dimensions: TraversableOnce[String],
      facts: TraversableOnce[String]): Future[IndexedSeq[Map[String, Any]]] =
    olapCube[TCube, TSearchable, Map[String, Any]](Some(specification), dimensions, facts)

  /**
   * Execute remote service (server implementation for IServerService<TArgument, TResult>)
   * Send message with serialized argument to remote service and deserialize response.
   *
   * @param manifest deserialize result into provided type
   * @param command  remote service name
   * @param argument remote service argument
   * @return         future with deserialized result
   */
  def execute[TArgument, TResult: ClassTag](
      command: String,
      argument: TArgument): Future[TResult]
}
