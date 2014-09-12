package com.dslplatform.api.client

import com.dslplatform.api.patterns.PersistableRepository
import com.dslplatform.api.patterns.SearchableRepository
import com.dslplatform.api.patterns.ServiceLocator
import com.dslplatform.api.patterns.Repository

import java.io.InputStream
import java.io.FileInputStream

import org.slf4j.{Logger, LoggerFactory}

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
  def init(iniStream: InputStream, initialComponents: Map[Object, AnyRef]): ServiceLocator = {
    val locator = new MapServiceLocator(
      initialComponents ++
        (if (initialComponents.contains(classOf[Logger]))
          Map.empty
        else Map(classOf[Logger] -> LoggerFactory.getLogger("dsl-client-scala")))
    )
    init(iniStream, locator)
  }

  private def init(
    iniStream: InputStream,
    locator: MapServiceLocator
  ) = {
    val threadPool = if (!locator.contains[ExecutorService]) {
      val pool: ExecutorService = Executors.newCachedThreadPool()
      locator.register[ExecutorService](pool)
      pool
    } else {
      locator.resolve[ExecutorService]
    }
    if (!locator.contains[ExecutionContext])
      locator.register[ExecutionContext](ExecutionContext.fromExecutorService(threadPool))
    locator
      .register[ProjectSettings](new ProjectSettings(iniStream))
      .register[JsonSerialization]
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
  def init(iniPath: String, initialComponents: Map[Object , AnyRef] = Map.empty): ServiceLocator = {
    val iniStream: InputStream = {
      val file: java.io.File = new java.io.File(iniPath)
      if (file.exists()) {
        new FileInputStream(file)
      } else {
        getClass.getResourceAsStream(iniPath)
      }
    }
    if (iniStream == null) throw new RuntimeException(s"$iniPath was not found in the file system or the classpath.")
    try {
      init(iniStream, initialComponents)
    } finally {
      iniStream.close()
    }
  }

}
