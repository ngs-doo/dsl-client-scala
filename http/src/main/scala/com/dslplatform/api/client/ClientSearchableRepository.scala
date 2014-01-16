package com.dslplatform.api.client

import com.dslplatform.api.patterns.Searchable
import com.dslplatform.api.patterns.SearchableRepository
import com.dslplatform.api.patterns.Specification
import scala.concurrent.Future
import com.dslplatform.api.patterns.ServiceLocator
import com.dslplatform.api.patterns.SearchBuilder
import scala.reflect.ClassTag
/**
 * Common base implementation for {@link SearchableRepository searchable repository}.
 * It redirects calls to proxy services.
 * It shouldn't be used or resolved.
 * Instead domain model repositories should be resolved.
 * <p>
 * DSL example:
 * <blockquote><pre>
 * module Todo {
 *   sql TaskInfo 'SELECT name, description FROM task' {
 *     string name;
 *     string description;
 *   }
 * }
 * </pre></blockquote>
 * Scala usage:
 * <pre>
 * ServiceLocator locator = ...
 * val repository = locator.resolve[SearchableRepository[Todo.TaskInfo]]
 * </pre>
 * @param [T] domain object type
 */
class ClientSearchableRepository[TSearchable <: Searchable: ClassTag](locator: ServiceLocator)
    extends SearchableRepository[TSearchable] {

  protected val domainProxy: DomainProxy = locator.resolve[DomainProxy]

  def search(
    specification: Option[Specification[TSearchable]],
    limit: Option[Int],
    offset: Option[Int],
    order: Map[String, Boolean]): Future[IndexedSeq[TSearchable]] =
    domainProxy.search(specification, limit, offset, order)

  def count(specification: Option[Specification[TSearchable]]): Future[Long] =
    domainProxy.count(specification)
}
