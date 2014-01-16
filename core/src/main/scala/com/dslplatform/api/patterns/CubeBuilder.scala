package com.dslplatform.api.patterns

import com.dslplatform.api.client.StandardProxy
import scala.reflect.ClassTag
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.Duration
import scala.concurrent.Await
import scala.collection.mutable.Buffer

/**
 * Utility class for building olap cube analysis.
 */
class CubeBuilder[TCube <: Cube[TSource]: ClassTag, TSource <: Searchable: ClassTag](
    private val cube: Cube[TSource],
    private var specification: Option[Specification[TSource]] = None,
    private val dimensions: Buffer[String] = Buffer.empty,
    private val facts: Buffer[String] = Buffer.empty,
    private var limit: Option[Int] = None,
    private var offset: Option[Int] = None,
    private val order: Buffer[(String, Boolean)] = Buffer.empty) {

  def where(specification: Specification[TSource]) = filter(specification)

  def filter(specification: Specification[TSource]) = {
    this.specification = Option(specification)
    this
  }

  def use(dimensionOrFact: String) = {
    if (cube.dimensions.contains(dimensionOrFact)) {
      dimensions += dimensionOrFact
    } else if (cube.facts.contains(dimensionOrFact)) {
      facts += dimensionOrFact
    } else {
      throw new IllegalArgumentException("Unknown dimension or fact: " + dimensionOrFact)
    }
    this
  }

  private def orderBy(property: String, ascending: Boolean) = {
    if (property == null || property == "")
      throw new IllegalArgumentException("property can't be empty");
    order += property -> ascending
    this
  }

  /**
   * Order result ascending using a provided property
   *
   * @param property name of domain objects property
   * @return         new copy
   */
  def ascending(property: String) = orderBy(property, true)

  /**
   * Order result descending using a provided property
   *
   * @param property name of domain objects property
   * @return         new copy
   */
  def descending(property: String) = orderBy(property, false)

  def limit(limit: Int) = take(limit)

  def take(limit: Int): this.type = {
    this.limit = Some(limit)
    this
  }

  def offset(offset: Int) = skip(offset)

  def skip(offset: Int): this.type = {
    this.offset = Some(offset)
    this
  }

  /**
   * Runs the analysis using provided configuration.
   * Result will be deserialized to TResult
   *
   * @return  sequence of specified data types
   */
  def analyze[TResult: ClassTag](
    implicit locator: ServiceLocator,
    ec: ExecutionContext,
    duration: Duration): Seq[TResult] = {
    val proxy = locator.resolve[StandardProxy]
    Await.result(
      proxy.olapCube[TCube, TSource, TResult](
        specification,
        dimensions,
        facts,
        limit,
        offset,
        order.toMap),
      duration)
  }

  /**
   * Runs the analysis using provided configuration.
   * Result will be deserialized into sequence of Map[String, Any]
   *
   * @return  analysis result
   */
  def analyzeMap(
    implicit locator: ServiceLocator,
    ec: ExecutionContext,
    duration: Duration): Seq[Map[String, Any]] = {
    val proxy = locator.resolve[StandardProxy]
    Await.result(
      proxy.olapCube[TCube, TSource, Map[String, Any]](
        specification,
        dimensions,
        facts,
        limit,
        offset,
        order.toMap),
      duration)
  }
}
