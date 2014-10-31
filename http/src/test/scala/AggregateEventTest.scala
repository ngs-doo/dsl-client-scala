import com.dslplatform.api.patterns.ServiceLocator
import org.specs2._
import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner
import com.dslplatform.test.simple.SimpleRoot
import org.specs2.specification.Step

@RunWith(classOf[JUnitRunner])
class AggregateEventTest extends Specification {

  def is = s2"""Aggregate Event simple
    instantiate event on persisted root   ${located(instantiateEventOnPersistedRoot)}
    just instantiate                      $justInstantiate
    submit event                          ${located(submitEvent)}
                                          ${Step(located.close())}
  """

  val located = new located {}

  implicit val duration: scala.concurrent.duration.FiniteDuration = scala.concurrent.duration.FiniteDuration(1,
    "seconds")

  def instantiateEventOnPersistedRoot = { implicit locator : ServiceLocator =>
    val uri = SimpleRoot().create()
    pending
  }

  def justInstantiate = {
    pending
  }

  def submitEvent = { implicit locator : ServiceLocator =>
    val uri = SimpleRoot().create()
    pending
  }
}
