package com.dslplatform.api.client

import com.dslplatform.api.patterns.AggregateRoot
import com.dslplatform.api.patterns.PersistableRepository
import com.dslplatform.api.patterns.ServiceLocator

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.reflect.ClassTag

/**
 * Common base implementation for {@link PersistableRepository persistable repository}.
 * It redirects calls to proxy services.
 * It shouldn't be used or resolved.
 * Instead domain model repositories should be resolved.
 *
 * <p>
 * DSL example:
 * <blockquote><pre>
 *
 * module Todo {
 *   aggregate Task;
 * }
 * </blockquote></pre>
 * Java usage:
 * <pre>
 * IServiceLocator locator;
 * PersistableRepository&lt;Todo.Task&gt; repository = locator.resolve(Todo.TaskRepository.class);
 * </pre>
 *
 * @param [T] aggregate root type
 */
class ClientPersistableRepository[T <: AggregateRoot: ClassTag](locator: ServiceLocator)
    extends ClientRepository[T](locator)
    with PersistableRepository[T] {

  private implicit val executionContext: ExecutionContext = locator.resolve(classOf[ExecutionContext])

  private val standardProxy: StandardProxy = locator.resolve[StandardProxy]
  /**
   * Generated class will provide class manifest and locator
   *
   * @param manifest domain object type
   * @param locator  context in which domain object lives
   */

  def persist(
    inserts: TraversableOnce[T],
    updates: TraversableOnce[(T, T)],
    deletes: TraversableOnce[T]): Future[IndexedSeq[String]] =
    standardProxy.persist(inserts, updates, deletes)

  def insert(inserts: T*): Future[IndexedSeq[String]] = insert(inserts)

  def insert(inserts: TraversableOnce[T]): Future[IndexedSeq[String]] =
    standardProxy.persist[T](inserts, Nil, Nil)

  def insert(insert: T): Future[String] =
    crudProxy.create(insert).map(_.URI)

  def update(updates: TraversableOnce[T]): Future[_] =
    standardProxy.persist(Nil, updates.map { t => (t, t) }, Nil)

  def update(updates: T*): Future[_] = update(updates)

  def update(update: T): Future[T] = crudProxy.update(update)

  def delete(deletes: TraversableOnce[T]): Future[IndexedSeq[String]] =
    standardProxy.persist(Nil, Map.empty, deletes)

  def delete(deletes: T*): Future[IndexedSeq[String]] = delete(deletes)

  def delete(delete: T): Future[_] = crudProxy.delete(delete.URI)
}
