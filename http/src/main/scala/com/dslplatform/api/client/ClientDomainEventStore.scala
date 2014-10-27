package com.dslplatform.api.client

import com.dslplatform.api.patterns.AggregateDomainEvent
import com.dslplatform.api.patterns.AggregateRoot
import com.dslplatform.api.patterns.DomainEvent
import com.dslplatform.api.patterns.DomainEventStore

import scala.concurrent.Future
import scala.reflect.ClassTag

class ClientDomainEventStore(domainProxy: DomainProxy)
    extends DomainEventStore {

  def submit[TEvent <: DomainEvent](event: TEvent): Future[String] =
    domainProxy.submit[TEvent](event)

  def submit[TAggregate <: AggregateRoot: ClassTag, TEvent <: AggregateDomainEvent[TAggregate]](
      event: TEvent,
      uri: String): Future[TAggregate] =
    domainProxy.submit[TAggregate, TEvent](event, uri)

  def submit[TAggregate <: AggregateRoot: ClassTag, TEvent <: AggregateDomainEvent[TAggregate]](
      event: TEvent,
      aggregate: TAggregate): Future[TAggregate] =
    submit[TAggregate, TEvent](event, aggregate.URI)
}
