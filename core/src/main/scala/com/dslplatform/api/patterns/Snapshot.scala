package com.dslplatform.api.patterns

import org.joda.time.DateTime

/**
 * Snapshot of some past state of an {@link AggregateRoot aggregate root}
 *
 * @param [TAggregateRoot] type of aggregate root
 */
class Snapshot[TAggregateRoot <: AggregateRoot](
    val URI: String,
    val At: DateTime,
    val Action: String,
    val Value: TAggregateRoot) extends Identifiable
