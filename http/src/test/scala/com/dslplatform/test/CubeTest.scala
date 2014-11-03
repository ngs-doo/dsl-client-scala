package com.dslplatform.test

import com.dslplatform.api.patterns.{PersistableRepository, ServiceLocator}
import com.dslplatform.test.simple._
import org.specs2._
import org.specs2.specification.Step

class CubeTest extends Specification with Common {

  def is = sequential ^ s2"""
  Cube simple
                                          ${Step(located.clean[SimpleRoot])}
    analyze with string dimension         ${located(analyzeWithStringDimension)}
    analyze with enumeration dimension    ${located(analyzeWithEnumerationDimension)}
    analyze with both dimension           ${located(analyzeWithBothDimension)}
    analyze with specification            ${located(analyzeWithSpecification)}
    analyze with all parameters           ${located(analyzeWithAllParameters)}
    analyze with all but specification    ${located(analyzeWithAllButSpecification)}
    with builder                          ${located(withBuilder)}
                                          ${Step(located.close())}
  """

  private val located = new Located

  private val name1 = rName
  private val name2 = rName
  private val name3 = rName

  private val int1 = rInt
  private val int2 = rInt
  private val int3 = rInt

  private val numOfRoot = 13
  private val numOfNames = 4
  private val namesC = for (i <- 1 to 4) yield rName
  private val myNameC = namesC(0)
  private val srarr = for (i <- 0 to numOfRoot) yield SimpleRoot(rInt, rFloat, namesC(rInt(numOfNames)), E.C)

  private val computedMaxStr = Seq(int1, int3).max
  private val computedMinStr = Seq(int1, int3).min
  private val computedMaxEnumB = Seq(int1, int2, int3).max
  private val computedMinEnumB = Seq(int1, int2, int3).min
  private val computedMaxEnumA = Seq(int2, int3).max
  private val computedMinEnumA = Seq(int2, int3).min

  private val computedMaxEnumC = srarr.map {
    _.i
  }.max
  private val computedMinEnumC = srarr.map {
    _.i
  }.min

  private val specification = SimpleRoot.withS(name1)
  private val srs = srarr ++ (
    SimpleRoot(int1, 2f, name1, E.B) ::
      SimpleRoot(int2, 2f, name2, E.B) ::
      SimpleRoot(int3, 2f, name3, E.B) ::
      SimpleRoot(int2, 2f, name2, E.A) ::
      SimpleRoot(int3, 2f, name1, E.A) :: Nil)

  def analyzeWithStringDimension = { implicit locator: ServiceLocator =>
    val simpleRootRepository = locator.resolve[PersistableRepository[SimpleRoot]]

    await(simpleRootRepository.insert(srs))

    val simpleCubeResult = SimpleCube.analyzeMap(SimpleCube.s :: Nil, SimpleCube.max_i :: SimpleCube.min_i :: Nil)

    simpleCubeResult.map {
      _.size
    } must beGreaterThan(2).await
    val result = await(simpleCubeResult.map {
      _.find(_.get("s") == (Some(name1))).head
    })
    result.get("max_i") must beSome(computedMaxStr)
    result.get("min_i") must beSome(computedMinStr)
  }

  def analyzeWithEnumerationDimension = { implicit locator: ServiceLocator =>
    val simpleCubeResult = SimpleCube.analyze[Map[String, String]](SimpleCube.e :: Nil,
      SimpleCube.max_i :: SimpleCube.min_f :: Nil)

    simpleCubeResult.map {
      _.size
    } must beEqualTo(3).await
    simpleCubeResult.map {
      _.find(_.get("e") == (Some("B"))).head.get("max_i")
    } must beEqualTo(Some(computedMaxEnumB)).await(1, duration10)
  }

  def analyzeWithBothDimension = { implicit locator: ServiceLocator =>
    val scr = await(SimpleCube.analyze[Map[String, String]](SimpleCube.e :: SimpleCube.s :: Nil,
      SimpleCube.max_i :: SimpleCube.min_i :: SimpleCube.min_f :: Nil))

    scr.find(el => el("s") == name1 && el("e") == "B").head.get("max_i") must beSome(int1)
    scr.find(el => el("s") == name2 && el("e") == "B").head.get("max_i") must beSome(int2)
    scr.find(el => el("s") == name3 && el("e") == "B").head.get("max_i") must beSome(int3)
  }

  def analyzeWithSpecification = { implicit locator: ServiceLocator =>
    val order = Map.empty

    val fcube = SimpleCube.analyzeMap(
      dimensions = SimpleCube.e ::
        SimpleCube.s :: Nil,
      facts = SimpleCube.max_i ::
        SimpleCube.min_i ::
        SimpleCube.min_f :: Nil,
      order = Map.empty[String, Boolean],
      specification = Some(specification))
    val scr = await(fcube)

    scr.size === 2
  }

  def analyzeWithAllParameters = { implicit locator: ServiceLocator =>
    val fCube = SimpleCube.analyzeMap(
      dimensions = SimpleCube.e ::
        SimpleCube.s :: Nil,
      facts = SimpleCube.max_i ::
        SimpleCube.min_i ::
        SimpleCube.min_f :: Nil,
      limit = Some(1),
      offset = Some(1),
      order = Map.empty[String, Boolean],
      specification = Some(specification))
    val scr = await(fCube)

    scr.size === 1
  }

  def analyzeWithAllButSpecification = { implicit locator: ServiceLocator =>
    val fCube = SimpleCube.analyzeMap(
      dimensions = SimpleCube.e ::
        SimpleCube.s :: Nil,
      facts = SimpleCube.max_i ::
        SimpleCube.min_i ::
        SimpleCube.min_f :: Nil,
      limit = Some(1),
      offset = Some(1),
      order = Map.empty[String, Boolean],
      specification = None)
    val scr = await(fCube)

    scr.size === 1
  }

  def withBuilder = { implicit locator: ServiceLocator =>
    val builder = SimpleCube.builder
    builder.use(SimpleCube.s).use(SimpleCube.max_i)
    val simpleCubeResult = builder.analyzeMap

    simpleCubeResult.size must beGreaterThan(2)
    // where s == 1 => max_i == 1
    simpleCubeResult.find(_.get("s") == Some(name1)).get("max_i") must beEqualTo(computedMaxStr)
  }
}
