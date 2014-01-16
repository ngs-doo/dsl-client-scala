package com.dslplatform.api.patterns;

import scala.concurrent.Future
import scala.collection.mutable.Buffer

/**
 * Utility class for building a search over a {@link SearchableRepository searchable repository}.
 * Search can be performed using more fluent API,
 * by providing specification limit, offset and custom order
 * Constructor for SearchBuilder which requires a repository to perform
 * a search.
 * @param [TSearchable] domain object type.
 */
class SearchBuilder[TSearchable <: Searchable](
    repository: SearchableRepository[TSearchable],
    private var specification: Option[Specification[TSearchable]] = None,
    private var limit: Option[Int] = None,
    private var offset: Option[Int] = None,
    order: Buffer[(String, Boolean)] = Buffer.empty) {

  /**
   * Provide {@link Specification[TSearchable] search predicate} for filtering results.
   *
   * @param specification search predicate
   * @return              itself
   */

  def where(specification: Specification[TSearchable]) =
    filter(specification)

  /**
   * Provide {@link Specification[TSearchable] search predicate} for filtering results.
   *
   * @param specification search predicate
   * @return              itself
   */
  def filter(specification: Specification[TSearchable]) = {
    this.specification = Option(specification)
    this
  }

  /**
   * Define a maximum number of results
   *
   * @param limit maximum number of results
   * @return      itself
   */
  def limit(limit: Int) = take(limit)

  /**
   * Define a maximum number of results.
   *
   * @param limit maximum number of results
   * @return      itself
   */
  def take(limit: Int): this.type = {
    this.limit = Some(limit)
    this
  }

  /**
   * Define a number of results to be skipped.
   *
   * @param offset number of results to be skipped
   * @return       itself
   */
  def offset(offset: Int) = skip(offset)

  /**
   * Define a number of results to be skipped.
   *
   * @param offset number of results to be skipped
   * @return       itself
   */
  def skip(offset: Int): this.type = {
    this.offset = Some(offset)
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
   * @return         itself
   */
  def ascending(property: String) = orderBy(property, true)

  /**
   * Order result descending using a provided property
   *
   * @param property name of domain objects property
   * @return         itself
   */
  def descending(property: String) = orderBy(property, false)

  /**
   * Returns a Seq of domain objects which satisfy
   * {@link Specification specification} if it was set, otherwise all of them.
   * Parameters can be previously set to <code>limit</code> results,
   * skip <code>offset</code> of initial results and <code>order</code>
   * by some of this domain objects properties.
   *
   * @return  future value of the resulting sequence
   */
  def search(): Future[Seq[TSearchable]] =
    repository search (specification, limit, offset, order.toList.toMap)
}
