package com.dslplatform.test

import com.dslplatform.api.patterns.ServiceLocator
import com.dslplatform.test.simple.TrivialEvent
import org.specs2._
import org.specs2.specification.Step

class EventSubmitTest extends Specification with Common {

  def is = s2"""
    Event Simple
      just instantiate    $justInstantiate
      submit event        ${located(submit)}
                          ${Step(located.close())}
  """

  val located = new Located

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
