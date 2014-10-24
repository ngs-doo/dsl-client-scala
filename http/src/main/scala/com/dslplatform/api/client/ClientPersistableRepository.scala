package com.dslplatform.api.client

import com.dslplatform.api.patterns.AggregateRoot
import com.dslplatform.api.patterns.PersistableRepository
import com.dslplatform.api.patterns.ServiceLocator

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.reflect.ClassTag

/** Common base implementation for {@link PersistableRepository persistable repository}.
  * It redirects calls to proxy services.
  * It shouldn't be used or resolved.
  * Instead domain model repositories should be resolved.
  *
  * DSL example:
  * {{{
  * module Todo {
  *   aggregate Task;
  * }
  * }}}
  * Scala usage:
  * {{{
  *   val locator: ServiceLocator = ...
  *   val repository = locator.resolve[PersistableRepository[Todo.Task]]
  * }}}
  *
  * @param [T] aggregate root type
  */
class ClientPersistableRepository[T <: AggregateRoot: ClassTag](locator: ServiceLocator)
    extends ClientRepository[T](locator)
    with PersistableRepository[T] {

  private implicit val executionContext: ExecutionContext = locator.resolve(classOf[ExecutionContext])

  private val standardProxy: StandardProxy = locator.resolve[StandardProxy]

  def persist(
      inserts: TraversableOnce[T],
      updates: TraversableOnce[(T, T)],
      deletes: TraversableOnce[T]): Future[IndexedSeq[String]] =
    standardProxy.persist(inserts, updates, deletes)

  def insert(inserts: TraversableOnce[T]): Future[IndexedSeq[String]] =
    standardProxy.persist[T](inserts, Nil, Nil)

  def insert(insert: T): Future[String] =
    crudProxy.create(insert).map(_.URI)

  def update(updates: TraversableOnce[T]): Future[Unit] =
    standardProxy.persist(Nil, updates.map { t => (t, t) }, Nil).map(_ => ())

  def update(update: T): Future[Unit] =
    crudProxy.update(update).map(_ => ())

  def delete(deletes: TraversableOnce[T]): Future[Unit] =
    standardProxy.persist(Nil, Map.empty, deletes).map(_ => ())

  def delete(delete: T): Future[Unit] =
    crudProxy.delete(delete.URI).map(_ => ())
}
