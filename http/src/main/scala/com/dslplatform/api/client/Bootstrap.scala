package com.dslplatform.api.client

import java.util.{ Collections, Properties }

import com.dslplatform.api.patterns.PersistableRepository
import com.dslplatform.api.patterns.SearchableRepository
import com.dslplatform.api.patterns.ServiceLocator
import com.dslplatform.api.patterns.Repository

import java.io.{ InputStream, FileInputStream }

import org.slf4j.{ Logger, LoggerFactory }

import java.util.concurrent.{ TimeUnit, AbstractExecutorService, Executors, ExecutorService }
import scala.concurrent.{ ExecutionContextExecutorService, ExecutionContext }

/** DSL client initialization.
  * Initialize {@link ServiceLocator locator} with services and
  * communication configuration, such as remote url and authentication.
  */
object Bootstrap {
  private var staticLocator: ServiceLocator = null

  /** Static service locator which was initialized.
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
  /** Initialize service locator using provided dsl-project.props stream.
    *
    * @param inputStream       stream for project.props
    * @return                  initialized service locator
    * @throws IOException      in case of failure to read stream
    */
  def init(inputStream: InputStream): ServiceLocator = {
    val properties = new Properties()
    properties.load(inputStream)
    init(properties)
  }

  /** Initialize service locator using provided dsl-project.props stream.
    *
    * @param properties        properties
    * @return                  initialized service locator
    * @throws IOException      in case of failure to read stream
    */
  def init(properties: Properties): ServiceLocator = init(properties, Map.empty[Object, AnyRef])

  /** Initialize service locator using provided dsl-project.props stream.
    *
    * @param properties        properties
    * @param initialComponents Map of initial components Logger, ExecutionContext
    *                          or HttpHeaderProvider
    * @return                  initialized service locator
    * @throws IOException      in case of failure to read stream
    */
  def init(properties: Properties, initialComponents: Map[Object, AnyRef]): ServiceLocator = {

    val locator = new MapServiceLocator(
      initialComponents ++
        (if (initialComponents.contains(classOf[Logger]))
          Map.empty
        else Map(classOf[Logger] -> LoggerFactory.getLogger("dsl-client-scala")))
    )
    locator.register[Properties](properties)

    (initialComponents.get(classOf[ExecutionContext]), initialComponents.get(classOf[ExecutorService])) match {
      case (Some(ec), None) =>
        val ecc = ec.asInstanceOf[ExecutionContext]
        val threadPool = new AbstractExecutorService with ExecutionContextExecutorService {
          override def prepare(): ExecutionContext = ecc
          override def isShutdown = false
          override def isTerminated = false
          override def shutdown() = ()
          override def shutdownNow() = Collections.emptyList[Runnable]
          override def execute(runnable: Runnable): Unit = ecc execute runnable
          override def reportFailure(t: Throwable): Unit = ecc reportFailure t
          override def awaitTermination(length: Long, unit: TimeUnit): Boolean = false
        }
        locator.register[ExecutorService](threadPool)
      case (None, Some(tp)) =>
        locator.register[ExecutionContext](ExecutionContext.fromExecutorService(tp.asInstanceOf[ExecutorService]))
      case _ =>
        val tp = Executors.newCachedThreadPool()
        locator.register[ExecutorService](tp)
        locator.register[ExecutionContext](ExecutionContext.fromExecutorService(tp))
    }
    if (!initialComponents.contains(classOf[HttpHeaderProvider])) {
      locator.register[HttpHeaderProvider](new SettingsHeaderProvider(properties))
    }
    locator
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

  /** Initialize service locator using provided dsl-project.props path.
    *
    * @param propertiesPath      path to dsl-project.props
    * @return                    initialized service locator
    * @throws IOException in case of failure to read project.ini
    */
  def init(propertiesPath: String, initialComponents: Map[Object, AnyRef] = Map.empty[Object, AnyRef]): ServiceLocator = {
    val inputStream: InputStream = {
      val file: java.io.File = new java.io.File(propertiesPath)
      if (file.exists()) {
        new FileInputStream(file)
      } else {
        getClass.getResourceAsStream(propertiesPath)
      }
    }
    if (inputStream == null) throw new RuntimeException(s"$propertiesPath was not found in the file system or the classpath.")
    try {
      val properties = new Properties()
      properties.load(inputStream)
      init(properties, initialComponents)
    } finally {
      inputStream.close()
    }
  }
}
