package com.dslplatform.test

import com.dslplatform.api.patterns.ServiceLocator
import com.dslplatform.test.complex.{BaseRoot, EmptyRoot}
import com.dslplatform.test.simple.SimpleRoot
import org.specs2._
import org.specs2.specification.Step

class RootTest extends Specification with Common {

  def is = s2"""
    Root Simple
      just instantiate          $justInstantiate
      persist                   ${located(persist)}
      update                    ${located(update)}
    Root Base
      just instantiate          $pending
      persist                   ${located(persistBase)}
      update                    ${located(updateBase)}
                                ${Step(located.close())}
    """

  val located = new Located

  def justInstantiate = {
    val sr = SimpleRoot(1, 2f, "3")

    sr.URI !== null
  }

  def persist = { implicit locator: ServiceLocator =>
    val sr = SimpleRoot(1, 2f, "3")
    val oldUri = sr.URI
    sr.create()

    sr.URI !== oldUri
  }

  def update = { implicit locator: ServiceLocator =>
    val sr = SimpleRoot(1, 2f, "3")
    sr.create()
    sr.i = 2
    sr.update()

    sr.URI !== null
  }

  def delete = { implicit locator: ServiceLocator =>
    val sr = SimpleRoot(1, 2f, "3")
    sr.create()
    sr.delete()
    SimpleRoot.find(sr.URI) must throwA[java.io.IOException]
  }
/*
  def justInstantiateBase = { locator: ServiceLocator =>
    BaseRoot() must throwA[IllegalArgumentException] todo - rethink this
    success
  }
*/
  def persistBase = { implicit locator: ServiceLocator =>
    val er = EmptyRoot().create()

    val br = BaseRoot(root = er)
    br.create()

    br.rootURI === er.URI
    br.URI !== null
  }

  def updateBase = { implicit locator: ServiceLocator =>
    val er1 = EmptyRoot().create()
    val er2 = EmptyRoot().create()

    val br = BaseRoot(root = er1).create()

    br.root = er2
    br.update()

    val remoteBR = BaseRoot.find(br.URI)

    remoteBR.rootURI === er2.URI
  }

  def deleteBase = { implicit locator: ServiceLocator =>
    val er = EmptyRoot().create()
    val br = BaseRoot(root = er)
    br.create()
    br.delete()
    BaseRoot.find(br.URI) must throwA[java.io.IOException]
  }
}
