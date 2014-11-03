import com.dslplatform.api.patterns.ServiceLocator
import com.dslplatform.test.simple.TrivialEvent
import org.specs2._
import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner
import org.specs2.specification.Step

@RunWith(classOf[JUnitRunner])
class EventSubmitTest extends Specification with Common {

  def is = s2"""
    Event Simple
      just instantiate    $justInstantiate
      submit event        ${located(submit)}
                          ${Step(located.close())}
  """

  val located = new located {}

  def justInstantiate = {
    val sr = TrivialEvent()
    sr.URI !== null
  }

  def submit = { implicit locator: ServiceLocator =>
    val sr = TrivialEvent()
    val a = sr.submit()
    a !== null
  }
}
