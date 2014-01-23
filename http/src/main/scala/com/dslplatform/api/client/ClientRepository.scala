package com.dslplatform.api.client

import com.dslplatform.api.patterns.Identifiable
import com.dslplatform.api.patterns.Repository
import com.dslplatform.api.patterns.ServiceLocator

import scala.concurrent.Future
import scala.reflect.ClassTag
/**
 * Common base implementation for {@link Repository repository}.
 * It redirects calls to proxy services.
 * It shouldn't be used or resolved.
 * Instead domain model repositories should be resolved.
 * <p>
 * DSL example:
 * <blockquote><pre>
 * module Todo {
 *   aggregate Task;
 *   snowflake&lt;Task&gt; TaskList;
 * }
 * </pre></blockquote>
 * Java usage:
 * <pre>
 * IServiceLocator locator;
 * Repository&lt;Todo.TaskList&gt; repository = locator.resolve(Todo.TaskListRepository.class);
 * </pre>
 * @param [T] domain object type
 */
class ClientRepository[TIdentifiable <: Identifiable: ClassTag](
  locator: ServiceLocator)
    extends ClientSearchableRepository[TIdentifiable](locator)
    with Repository[TIdentifiable] {

  protected val crudProxy: CrudProxy = locator.resolve[CrudProxy]
  /**
   * Generated class will provide class manifest and locator
   *
   * @param manifest domain object type
   * @param locator  context in which domain object lives
   */

  def find(uris: TraversableOnce[String]): Future[IndexedSeq[TIdentifiable]] = domainProxy.find(uris)

  def find(uris: String*): Future[IndexedSeq[TIdentifiable]] = find(uris)

  def find(uri: String): Future[TIdentifiable] = crudProxy.read(uri)
}
