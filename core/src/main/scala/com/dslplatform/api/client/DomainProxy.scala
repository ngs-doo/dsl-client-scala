package com.dslplatform.api.client

import com.dslplatform.api.patterns.AggregateDomainEvent
import com.dslplatform.api.patterns.AggregateRoot
import com.dslplatform.api.patterns.DomainEvent
import com.dslplatform.api.patterns.Identifiable
import com.dslplatform.api.patterns.Searchable
import com.dslplatform.api.patterns.Specification

import scala.reflect.ClassTag
import scala.concurrent.Future

/**
 * Proxy service to remote REST-like API for basic domain operations
 * such as searching, counting and event sourcing.
 * <p>
 * It is preferred to use domain patterns instead of this proxy service.
 */
trait DomainProxy {
  /**
   * Returns a IndexedSeq of domain objects uniquely represented with their URIs.
   * Only found objects will be returned (IndexedSeq will be empty if no objects are found).
   *
   * @param manifest domain object class
   * @param uris     sequence of unique identifiers
   * @return         future to found domain objects
   */
  def find[TSearchable <: Identifiable: ClassTag](
      uris: TraversableOnce[String]): Future[IndexedSeq[TSearchable]]

  /**
   * Returns a IndexedSeq of domain objects satisfying optional {@link Specification specification}
   * with up to optional <code>limit</code> results.
   * Optional <code>offset</code> can be used to skip initial results.
   * Optional <code>order</code> should be given as a IndexedSeq of pairs of
   * <code>{@literal <String, Boolean>}</code>
   * where first is a property name and second is whether it should be sorted
   * ascending over this property.
   *
   * @param specification search predicate
   * @param limit         maximum number of results
   * @param offset        number of results to be skipped
   * @param order         custom ordering
   * @return              future to domain objects which satisfy search predicate
   */
  def search[TSearchable <: Searchable: ClassTag](
      specification: Option[Specification[TSearchable]] = None,
      limit: Option[Int] = None,
      offset: Option[Int] = None,
      order: Map[String, Boolean] = Map.empty): Future[IndexedSeq[TSearchable]]

  /**
   * Helper method for searching domain objects.
   * Returns a IndexedSeq of domain objects satisfying {@link Specification specification}
   *
   * @param specification search predicate
   * @return              future to domain objects which satisfy search predicate
   */
  def search[TSearchable <: Searchable: ClassTag](
      specification: Specification[TSearchable]): Future[IndexedSeq[TSearchable]] =
    search(Some(specification))

  /**
   * Helper method for searching domain objects.
   * Returns a IndexedSeq of domain objects satisfying {@link Specification specification}
   * with up to <code>limit</code> results.
   *
   * @param specification search predicate
   * @param limit search  maximum number of results
   * @return              future to domain objects which satisfy search predicate
   */
  def search[TSearchable <: Searchable: ClassTag](
      specification: Specification[TSearchable],
      limit: Int): Future[IndexedSeq[TSearchable]] =
    search(Some(specification), Some(limit))

  /**
   * Returns a number of elements satisfying optionally provided specification.
   *
   * @param specification search predicate
   * @return              future to number of domain objects which satisfy specification
   */
  def count[TSearchable <: Searchable: ClassTag](
      specification: Option[Specification[TSearchable]] = None): Future[Long]

  /**
   * Helper method for counting domain objects.
   * Returns a number of elements satisfying provided specification.
   *
   * @param specification search predicate
   * @return              future to number of domain objects which satisfy specification
   */
  def count[TSearchable <: Searchable: ClassTag](
      specification: Specification[TSearchable]): Future[Long] =
    count(Some(specification))

  /**
   * Send domain event to the server. Server will return identity under which it was stored.
   * Events can't be modified once they are submitted. Only new events can be created.
   *
   * @param domainEvent event to raise
   * @return            future containing string value of event URI
   */
  def submit[TEvent <: DomainEvent](domainEvent: TEvent): Future[String]

  /**
   * Apply domain event to a single aggregate. Server will return modified aggregate root.
   * Events can't be modified once they are submitted. Only new events can be created.
   *
   * @param domainEvent event to apply
   * @param uri         aggregate root uri
   * @return            future containing modified aggregate root
   */
  def submit[TAggregate <: AggregateRoot: ClassTag, TEvent <: AggregateDomainEvent[TAggregate]](
      domainEvent: TEvent,
      uri: String): Future[TAggregate]
}
