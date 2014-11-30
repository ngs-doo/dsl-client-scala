package com.dslplatform.test

import com.dslplatform.api.patterns.{PersistableRepository, Repository, SearchableRepository, ServiceLocator}
import com.dslplatform.test.simple.{Self, Selfy, SimpleRoot, SimpleSnow}
import org.specs2.mutable._
import org.specs2.specification.Step

class SnowflakeTest extends Specification with Common {

  override def is = sequential ^ s2"""
    Snowflake test                        ${Step(located.clean[SimpleRoot])}
      query repository                    ${located(queryRepository)}
      search over roots specification     ${located(searchOverRootsSpecification)}
      search over snows specification     ${located(searchOverSnowsSpecification)}
      self reference                      ${located(selfReference)}
                                          ${Step(located.close())}
"""

  private val numOfRoots = 13
  private val numOfNames = 4
  private val names = for (i <- 1 to 4) yield rName
  private val myName = names(0)
  private val arrSR = for (i <- 0 to numOfRoots - 1) yield SimpleRoot(rInt, rFloat, names(rInt(numOfNames)))

//  private def countOdds(e: Int*) = e.count(_ & 1 == 1)

  val located = new Located

  def queryRepository = { implicit locator: ServiceLocator =>
    await(locator.resolve[PersistableRepository[SimpleRoot]].insert(arrSR))
    val snow = await(locator.resolve[SearchableRepository[SimpleSnow]].search())
    snow.size === numOfRoots
  }

  def searchOverRootsSpecification = { implicit locator: ServiceLocator =>
    /*
          val specification = SimpleRoot.withS(myName)
          val snow = await(locator.resolve[SearchableRepository[SimpleSnow]].search(specification))

          snow.size === numOfRoots
    */
    pending
  }

  def searchOverSnowsSpecification = { implicit locator: ServiceLocator =>

    val specification = SimpleSnow.withSInSnow(myName)
    val snow = await(locator.resolve[SearchableRepository[SimpleSnow]].search(specification))
    snow.size === arrSR.filter(_.s == myName).size
  }

  def selfReference = { implicit locator: ServiceLocator =>

    val sr1 = Self().create
    val sr2 = Self(self = Some(sr1)).create
    val sr3 = Self(self = Some(sr2)).create
    val rep = locator.resolve[Repository[Selfy]]
    val selfs = await(rep.find(Seq(sr1.URI, sr2.URI, sr3.URI)))

    selfs.size === 3
    selfs(0).slim.size === 0
    selfs(0).fat.size === 0
    selfs(1).slim.size === 1
    selfs(1).fat.size === 1
    selfs(1).slim(0) == sr1.ID
    selfs(2).slim.size === 2
    selfs(2).fat.size === 2
    selfs(2).slim(0) == sr2.ID
    selfs(2).slim(1) == sr1.ID
  }
}
