package com.dslplatform.api.patterns

/** Collection of {@link AggregateRoot aggregate root} snapshots.
  * Snapshot is created whenever aggregate is created, modified or deleted if
  * history concept is enabled.
  *
  * DSL example:
  * {{{
  * module Blog {
  *   aggregate Post {
  *     String content;
  *     history;
  *   }
  * }
  * }}}
  * @tparam T aggregate root type
  */
case class History[T <: AggregateRoot](
    snapshots: IndexedSeq[Snapshot[T]]
  ) extends Identifiable {

  def URI: String = snapshots(0).URI
}
