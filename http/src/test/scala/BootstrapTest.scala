import com.dslplatform.api.client.{ClientPersistableRepository, HttpClient}
import com.dslplatform.api.patterns.PersistableRepository
import org.slf4j.{Logger, LoggerFactory}
import org.specs2._

class BootstrapTest extends Specification {

  def is = s2"""
  This specification checks the dependency management
    resolve default                           $defaults
    resolve provided logger                   $initial
    resolve a repository                      $resolveRepository
  """

  def defaults = {
    val locator = com.dslplatform.api.client.Bootstrap.init("/test-project.props")
    try {
      locator.resolve[HttpClient].isInstanceOf[HttpClient] must beTrue
    } finally {
      locator.resolve[HttpClient].shutdown()
    }
  }

  def initial = {
    val logger = LoggerFactory.getLogger("test-logger")
    val initialComponents: Map[Object, AnyRef] = Map(classOf[Logger] -> logger)
    val locator = com.dslplatform.api.client.Bootstrap.init("/test-project.props", initialComponents)
    try {
      locator.resolve[Logger] mustEqual(logger)
    } finally {
      locator.resolve[HttpClient].shutdown()
    }
  }

  def resolveRepository = {
    val locator = com.dslplatform.api.client.Bootstrap.init("/test-project.props")
    try {
      locator.resolve[PersistableRepository[M.Agg]].isInstanceOf[ClientPersistableRepository[_]] must beTrue
    } finally {
      locator.resolve[HttpClient].shutdown()
    }
  }
}
