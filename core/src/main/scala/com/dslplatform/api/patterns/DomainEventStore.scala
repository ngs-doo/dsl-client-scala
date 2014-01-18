package com.dslplatform.api.patterns;

import scala.concurrent.Future
import scala.reflect.ClassTag

/**
 * Service for submitting domain events to the application server.
 * <p>
 * It should be used when Future is a preferred way of interacting with the remote server.
 */
trait DomainEventStore {

  /**
   * Send domain event to the server. Server will return identity under which it was stored.
   * Events can't be modified once they are submitted. Only new events can be created.
   *
   * @param event event to raise
   * @return      future containing string value of event URI
   */
  def submit[T <: DomainEvent](event: T): Future[String]

  /**
   * Apply domain event to a single aggregate. Server will return modified aggregate root.
   * Events can't be modified once they are submitted. Only new events can be created.
   *
   * @param event event to apply
   * @param uri   aggregate root uri
   * @return      future containing modified aggregate root
   */
  def submit[TAggregate <: AggregateRoot : ClassTag, TEvent <: AggregateDomainEvent[TAggregate]](
      event: TEvent,
      uri: String): Future[TAggregate]

  /**
   * Helper method for sending domain event to the server. Server will return modified aggregate root.
   * Events can't be modified once they are submitted. Only new events can be created.
   *
   * @param event     event to apply
   * @param aggregate aggregate root instance
   * @return          future containing modified aggregate root
   */
  def submit[TAggregate <: AggregateRoot: ClassTag, TEvent <: AggregateDomainEvent[TAggregate]](
      event: TEvent,
      aggregate: TAggregate): Future[TAggregate];
}
