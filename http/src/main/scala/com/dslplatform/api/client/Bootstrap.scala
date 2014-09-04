package com.dslplatform.api.client

import com.dslplatform.api.patterns.PersistableRepository
import com.dslplatform.api.patterns.SearchableRepository
import com.dslplatform.api.patterns.ServiceLocator
import com.dslplatform.api.patterns.Repository

import java.io.InputStream
import java.io.FileInputStream

import org.slf4j.LoggerFactory

import java.util.concurrent.Executors
import java.util.concurrent.ExecutorService
import scala.concurrent.ExecutionContext

/**
 * DSL client Java initialization.
 * Initialize {@link ServiceLocator locator} with services and
 * communication configuration, such as remote url and authentication.
 */
object Bootstrap {
  private var staticLocator: ServiceLocator = null

  /**
   * Static service locator which was initialized.
   * In case of multiple projects and locator, you should avoid this method
   * and provide instances of locator yourself.
   *
   * @deprecated avoid calling static service locator
   * @return     last service locator which was initialized
   */
  def getLocator(): ServiceLocator = {
    if (staticLocator == null)
      throw new RuntimeException("Bootstrap has not been initialized, call Bootstrap.init");

    staticLocator
  }

  /**
   * Initialize service locator using provided project.ini stream.
   *
   * @param iniStream    stream for project.ini
   * @return             initialized service locator
   * @throws IOException in case of failure to read stream
   */
  def init(iniStream: InputStream): ServiceLocator = {
    val threadPool = Executors.newCachedThreadPool()
    val locator =
      new MapServiceLocator(LoggerFactory.getLogger("dsl-client-http"))
        .register[ProjectSettings](new ProjectSettings(iniStream))
        .register[JsonSerialization]
        .register[ExecutorService](threadPool)
        .register[ExecutionContext](ExecutionContext.fromExecutorService(threadPool))
        .register[HttpClient]
        .register[ApplicationProxy, HttpApplicationProxy]
        .register[CrudProxy, HttpCrudProxy]
        .register[DomainProxy, HttpDomainProxy]
        .register[StandardProxy, HttpStandardProxy]
        .register[ReportingProxy, HttpReportingProxy]
        .register(classOf[Repository[_]], classOf[ClientRepository[_]])
        .register(classOf[SearchableRepository[_]], classOf[ClientSearchableRepository[_]])
        .register(classOf[PersistableRepository[_]], classOf[ClientPersistableRepository[_]])

    staticLocator = locator
    locator
  }

  /**
   * Initialize service locator using provided dsl-project.ini path.
   *
   * @param iniPath      path to project.ini
   * @return             initialized service locator
   * @throws IOException in case of failure to read project.ini
   */
  def init(iniPath: String): ServiceLocator = {
    val iniStream: InputStream = new FileInputStream(iniPath)
    try {
      init(iniStream)
    } finally {
      iniStream.close()
    }
  }
}
