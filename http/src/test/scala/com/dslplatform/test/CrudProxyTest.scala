package com.dslplatform.test

import com.dslplatform.api.client.CrudProxy
import com.dslplatform.test.complex.{BaseRoot, EmptyRoot}
import com.dslplatform.test.simple.SimpleRoot
import org.specs2.mutable._
import org.specs2.specification.Step

class CrudProxyTest extends Specification with Common {

  override def is = s2"""
    Crud Proxy is used to persist single instance of domain object.
      persist             ${crudProxy(persist)}
      read                ${crudProxy(read)}
      persist             ${crudProxy(delete)}

    Crud Proxy persists BaseRoot.
      persist             ${crudProxy(persistBase)}
      read                ${crudProxy(readBase)}
      persist             ${crudProxy(deleteBase)}
                          ${Step(located.close())}
"""

  val located = new Located

  val crudProxy = located.resolved[CrudProxy]

  def persist = { crudProxy: CrudProxy =>
    val name = rName()
    val simpleRoot = SimpleRoot(s = name)
    val remoteRoot = await(crudProxy.create(simpleRoot))

    remoteRoot.s === name
  }

  def read = { crudProxy: CrudProxy =>
    val simpleRoot = SimpleRoot(s = rName())
    val remoteCreatedRoot = await(crudProxy.create(simpleRoot))
    val remoteReadRoot = await(crudProxy.read[SimpleRoot](remoteCreatedRoot.URI))

    simpleRoot.s === remoteReadRoot.s
  }

  def delete = { crudProxy: CrudProxy =>
    val simpleRoot = await(crudProxy.create(SimpleRoot(s = rName())))
    crudProxy.read[SimpleRoot](simpleRoot.URI).map(_.s) must beEqualTo(simpleRoot.s).await
  }

  def persistBase = { crudProxy: CrudProxy =>
    val emptyRoot = await(crudProxy.create(EmptyRoot()))
    val baseRoot = BaseRoot(root = emptyRoot)
    val remoteRoot = await(crudProxy.create(baseRoot))

    remoteRoot.URI !== null
  }

  def readBase = { crudProxy: CrudProxy =>
    pending
  }

  def deleteBase = { crudProxy: CrudProxy =>
    val simpleRoot = await(crudProxy.create(SimpleRoot(s = rName())))

    crudProxy.read[SimpleRoot](simpleRoot.URI).map(_.s) must beEqualTo(simpleRoot.s).await
  }
}
