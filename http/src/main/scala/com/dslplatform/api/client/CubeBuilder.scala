package com.dslplatform.api.client

import scala.reflect.ClassTag
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.Duration
import scala.concurrent.Await
import scala.collection.mutable.Buffer
import com.dslplatform.api.patterns.Cube
import com.dslplatform.api.patterns.Searchable
import com.dslplatform.api.patterns.ServiceLocator
import com.dslplatform.api.patterns.Specification
import scala.reflect.runtime.universe

/**
 * Utility class for building olap cube analysis.
 */
class CubeBuilder[TCube <: Cube[TSource]: ClassTag, TSource <: Searchable: ClassTag](
    cube: Cube[TSource]) {

  private var specification: Option[Specification[TSource]] = None
  private val dimensions: Buffer[String] = Buffer.empty
  private val facts: Buffer[String] = Buffer.empty
  private var limit: Option[Int] = None
  private var offset: Option[Int] = None
  private val order: Buffer[(String, Boolean)] = Buffer.empty

  /**
   * Restrict analysis on data subset
   *
   * @param specification use provided specification to filter data used for analysis
   * @return              self
   */
  def where(specification: Specification[TSource]) = filter(specification)
  /**
   * Restrict analysis on data subset
   *
   * @param specification use provided specification to filter data used for analysis
   * @return              self
   */
  def filter(specification: Specification[TSource]) = {
    this.specification = Option(specification)
    this
  }
  /**
   * Add dimension or fact to the result
   *
   * @param dimensionOrFact dimension or fact which will be shown in result
   * @return                self
   */
  def use(dimensionOrFact: String) = {
    require(dimensionOrFact ne null, "null value provided for dimension or fact")
    require(dimensionOrFact.length != 0, "empty value provided for dimension or fact")

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
   * Order result ascending using a provided property or path
   *
   * @param property name of domain objects property or path
   * @return         self
   */
  def ascending(property: String) = orderBy(property, true)

  /**
   * Order result descending using a provided property or path
   *
   * @param property name of domain objects property or path
   * @return         self
   */
  def descending(property: String) = orderBy(property, false)

  /**
   * Limit total number of results to provided value
   *
   * @param limit maximum number of results
   * @return      self
   */
  def limit(limit: Int) = take(limit)
  /**
   * Limit total number of results to provided value
   *
   * @param limit maximum number of results
   * @return      self
   */
  def take(limit: Int): this.type = {
    this.limit = Some(limit)
    this
  }
  /**
   * Skip specified number of initial results
   *
   * @param offset number of results to skip
   * @return       self
   */
  def offset(offset: Int) = skip(offset)
  /**
   * Skip specified number of initial results
   *
   * @param offset number of results to skip
   * @return       self
   */
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

    require(locator ne null, "locator not provided")
    require(ec ne null, "execution context not provided")
    require(duration ne null, "duration not provided")

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

    require(locator ne null, "locator not provided")
    require(ec ne null, "execution context not provided")
    require(duration ne null, "duration not provided")

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
