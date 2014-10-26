package com.dslplatform.api.patterns

/** {@link DomainEvent Domain event} which should be used when there is an action
  * to be applied on a single {@link AggregateRoot aggregate root}.
  *
  * When {@link DomainEvent domain event} affects only a single aggregate, then we can use
  * specialized aggregate domain event. This event can't have side effects outside
  * aggregate, which allows it to be replayed when it's asynchronous.
  * This is useful in write intensive scenarios to minimize write load in the database,
  * but will increase read load, because reading aggregate will have to read all its
  * unapplied events and apply them during reconstruction.
  *
  * AggregateDomainEvent is defined in DSL with keyword {@code event}.
  * <pre>
  * module Todo {
  *   aggregate Task;
  *   event&lt;Task&gt; markDone;
  * }
  * </pre>
  * @tparam T aggregate root type
  */
trait AggregateDomainEvent[T <: AggregateRoot]
    extends Identifiable
