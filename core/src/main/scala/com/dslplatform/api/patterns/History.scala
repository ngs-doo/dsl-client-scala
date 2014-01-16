package com.dslplatform.api.patterns

/**
 * Collection of {@link AggregateRoot aggregate root} snapshots.
 * Snapshot is created whenever aggregate is created, modified or deleted if
 * history concept is enabled.
 * <p>
 * DSL example:
 * <blockquote><pre>
 * module Blog {
 *   aggregate Post {
 *     string content;
 *     history;
 *   }
 * }
 * </pre></blockquote>
 * @param <T> aggregate root type
 */
class History[T <: AggregateRoot](snapshots: IndexedSeq[Snapshot[T]])
  extends Identifiable {
  def URI: String = snapshots(0).URI
}
