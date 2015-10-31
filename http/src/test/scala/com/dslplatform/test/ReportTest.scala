package com.dslplatform.test

import com.dslplatform.api.patterns.ServiceLocator
import com.dslplatform.test.simple.R
import org.specs2.mutable._
import org.specs2.specification.Step

class ReportTest extends Specification with Common {

  override def is = s2"""
    Simple report
      used to populate report data        ${located(populateResult)}
                                          ${Step(located.close())}
"""

  val located = new Located

  def populateResult = { locator: ServiceLocator =>
    val sr = R(i = 5)
    val res = sr.populate(locator)

    res.isInstanceOf[R.Result] must beTrue
  }
}
