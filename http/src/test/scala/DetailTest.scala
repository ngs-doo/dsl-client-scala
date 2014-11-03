import org.specs2._
import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner
import com.dslplatform.test.detailtest._
import com.dslplatform.api.patterns.{ServiceLocator, PersistableRepository}
import org.specs2.specification.Step

@RunWith(classOf[JUnitRunner])
class DetailTest extends Specification with Common {

  def is = sequential ^ s2"""
    Detail are elements grouped by association with reference to it.
      make a group                    ${located(makeAGroup)}
      add elements to the group       ${located(addElementsToTheGroup)}
      assert elements int the group   ${located(assertElementsInTheGroup)}
                                      ${Step(located.close())}
  """

  private val located = new located {}

  private val myNodeName = rString
  private val numOfLeafs = 27

  def makeAGroup = { implicit locator: ServiceLocator =>
    val mygroup = Node(myNodeName).create()
    mygroup.URI !== null
  }

  def addElementsToTheGroup = { implicit locator: ServiceLocator =>
    val myNode = Node.find(myNodeName)
    val otherGroup = Node(rName, Some(myNode)).create()
    val someElements = for (i <- 0 to numOfLeafs - 1) yield Leaf(node = Some(myNode))
    await(locator.resolve[PersistableRepository[Leaf]].insert(someElements))
    success
  }

  def assertElementsInTheGroup = { implicit locator: ServiceLocator =>
    val myNode = Node.find(myNodeName)
    (myNode.others.size === 1) &
      (myNode.leafs.size === numOfLeafs) &
      (myNode.leafsURI.size === numOfLeafs)
  }
}
