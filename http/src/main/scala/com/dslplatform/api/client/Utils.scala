package com.dslplatform.api.client

import com.dslplatform.api.patterns.Searchable
import com.dslplatform.api.patterns.SearchableRepository

object Utils {
  protected[client] def buildArguments(
    specification: Option[String],
    limit: Option[Int],
    offset: Option[Int],
    order: Map[String, Boolean]): String = {
    val params = Seq(
      makeParam("specification", specification),
      makeParam("limit", limit.map{ _.toString }),
      makeParam("offset", offset.map{ _.toString }),
      makeParam("order", makeOrderArguments(order))).flatten
    concatenateUrlParams(params)
  }

  protected[client] def buildOlapArguments(
    dimensions: TraversableOnce[String],
    facts: TraversableOnce[String],
    limit: Option[Int],
    offset: Option[Int],
    order: Map[String, Boolean],
    specification: Option[String] = None): String = {

    if (!contains(dimensions, order) & !contains(facts, order)) throw new IllegalArgumentException("Order must be an element of dimmensions or facts!");
    Seq(
      makeParam("dimensions", dimensions),
      makeParam("facts", facts),
      makeParam("limit", limit.map{ _.toString }),
      makeParam("offset", offset.map{ _.toString }),
      makeParam("order", makeOrderArguments(order)),
      makeParam("specification", specification)).flatten match {
        case Seq() =>
          throw new IllegalArgumentException("At least one dimension or fact is required")
        case params =>
          params.mkString("?", "&", "")
      }
  }

  private def makeOrderArguments(order: Map[String, Boolean]) = order.map{ case (k, v) => (if (v) "-" else "") + k }

  private def concatenateUrlParams(params: TraversableOnce[String], withFirst: Boolean = true) =
    if (params.isEmpty) ""
    else params.mkString(if (withFirst) "?" else "&", "&", "")

  private def makeParam(param: String, args: TraversableOnce[String]) =
    if (args.isEmpty) None else Some(args.mkString(param + "=", ",", ""))

  private def contains(iterSource: TraversableOnce[String], orders: Map[String, Boolean]) =
    if (orders == null || iterSource == null) true
    else {
      val list = iterSource.toList
      orders.keySet.forall(list.contains(_))
    }

  implicit class SearchPimp[TSearchable <: Searchable](
      repository: SearchableRepository[TSearchable]) {
    /**
     * Returns an instance of {@link SearchBuilder search builder} for this repository.
     * Search builder is helper class with fluent API for building search.
     *
     * @return utility class for building a search.
     */
    def builder() = new SearchBuilder[TSearchable](repository)
  }
}
