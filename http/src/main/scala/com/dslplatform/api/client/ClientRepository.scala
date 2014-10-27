package com.dslplatform.api.client

import com.dslplatform.api.patterns.Identifiable
import com.dslplatform.api.patterns.Repository
import com.dslplatform.api.patterns.ServiceLocator

import scala.concurrent.Future
import scala.reflect.ClassTag

/** Common base implementation for {@link Repository repository}.
  * It redirects calls to proxy services.
  * It shouldn't be used or resolved.
  * Instead domain model repositories should be resolved.
  *
  * DSL example:
  * <pre>
  * module Todo {
  *   aggregate Task;
  *   snowflake&lt;Task&gt; TaskList;
  * }
  * </pre>
  *
  * Usage:
  * {{{
  *   val locator: ServiceLocator = ...
  *   val repostiory = locator.resolve[Repository[Todo.TaskList]]
  * }}}
  * @tparam TIdentifiable domain object type
  */
class ClientRepository[TIdentifiable <: Identifiable: ClassTag](
    locator: ServiceLocator)
    extends ClientSearchableRepository[TIdentifiable](locator)
    with Repository[TIdentifiable] {

  protected val crudProxy: CrudProxy = locator.resolve[CrudProxy]

  def find(uris: TraversableOnce[String]): Future[IndexedSeq[TIdentifiable]] =
    domainProxy.find(uris)

  def find(uri: String): Future[TIdentifiable] =
    crudProxy.read(uri)
}
