import com.dslplatform.api.patterns.ServiceLocator
import org.specs2._
import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner
import com.dslplatform.test.simple.R
import org.specs2.specification.Step

@RunWith(classOf[JUnitRunner])
class ReportTest extends Specification with Common {

  def is = s2"""
    Simple report
      used to populate report data        ${located(populateResult)}
                                          ${Step(located.close())}
  """

  val located = new located {}

  def populateResult = { locator: ServiceLocator =>
    val sr = R(i = 5)
    val res = sr.populate(locator)

    res.isInstanceOf[R.Result] must beTrue
  }
}
