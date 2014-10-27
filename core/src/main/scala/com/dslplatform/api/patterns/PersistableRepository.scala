package com.dslplatform.api.patterns

import scala.concurrent.Future

/** Service for doing CRUD operations.
  * It can be used for applying changes on {@link AggregateRoot aggregate root}
  * to the remote server.
  *
  * It should be used when Future is a preferred way of interacting with the remote server
  * or bulk operations are required.
  *
  * @tparam T type of {@link AggregateRoot aggregate root}
  */
trait PersistableRepository[T <: AggregateRoot]
    extends Repository[T] {

  /** Apply local changes to the remote server.
    *
    * @param inserts new aggregate roots
    * @param updates pairs for updating old aggregate to new state
    * @param deletes aggregate roots which will be deleted
    * @return       future uris of newly created aggregates
    */
  def persist(
    inserts: TraversableOnce[T],
    updates: TraversableOnce[(T, T)],
    deletes: TraversableOnce[T]): Future[IndexedSeq[String]]

  /** Bulk insert.
    * Create multiple new {@link AggregateRoot aggregates}.
    *
    * @param inserts new aggregate roots
    * @return       future uris of created aggregate roots
    */
  def insert(inserts: TraversableOnce[T]): Future[IndexedSeq[String]]

  /** Insert a single {@link AggregateRoot aggregate}.
    *
    * @param insert new aggregate root
    * @return       future uri of created aggregate root
    */
  def insert(insert: T): Future[String]

  /** Bulk update.
    * Changing state of multiple {@link AggregateRoot aggregates}.
    *
    * @param updates sequence of aggregate roots to update
    * @return       future for error checking
    */
  def update(updates: TraversableOnce[T]): Future[Unit]

  /** Changing state of an aggregate root.
    *
    * @param update aggregate root to update
    * @return       future for error checking
    */
  def update(update: T): Future[Unit]

  /** Bulk delete.
    * Remote multiple {@link AggregateRoot aggregates}.
    *
    * @param deletes aggregate roots to delete
    * @return       future for error checking
    */
  def delete(deletes: TraversableOnce[T]): Future[Unit]

  /** Deleting an {@link AggregateRoot aggregate}.
    *
    * @param delete aggregate root to delete
    * @return       future for error checking
    */
  def delete(delete: T): Future[Unit]
}
