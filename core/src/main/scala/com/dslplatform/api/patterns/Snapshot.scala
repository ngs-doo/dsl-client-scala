package com.dslplatform.api.patterns

import org.joda.time.DateTime

/**
 * Snapshot of some past state of an {@link AggregateRoot aggregate root}
 *
 * @param [TAggregateRoot] type of aggregate root
 */
case class Snapshot[TAggregateRoot <: AggregateRoot](
    URI: String,
    at: DateTime,
    action: String,
    value: TAggregateRoot
  ) extends Identifiable
