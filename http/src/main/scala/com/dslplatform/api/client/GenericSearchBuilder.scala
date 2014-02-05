package com.dslplatform.api.client

import com.dslplatform.api.patterns.Searchable
import com.dslplatform.api.patterns.ServiceLocator

import scala.collection.mutable.{ Map => MMap }
import scala.collection.mutable.Buffer

import scala.concurrent.Future
import scala.reflect.ClassTag

/**
 * In case when specification is not defined on the server,
 * client side generic search builder can be used.
 * It should be used for testing and in rare cases when server can't be updated.
 * [p]
 * It is preferable to use server side specification.
 *
 * @param [T] type of domain: Any
 */

class GenericSearchBuilder[TSearchable <: Searchable: ClassTag] {
  case class FilterPair(Key: Int, Value: Any)

  private val filters: MMap[String, Buffer[FilterPair]] = MMap()
  private var limit: Option[Int] = None
  private var offset: Option[Int] = None
  private val order: MMap[String, Boolean] = MMap()

  private val EQUALS: Int = 0
  private val NOT_EQUALS: Int = 1
  private val LESS_THEN: Int = 2
  private val LESS_THEN_OR_EQUAL: Int = 3
  private val GREATER_THEN: Int = 4
  private val GREATER_THEN_OR_EQUAL: Int = 5
  private val VALUE_IN: Int = 6
  private val NOT_VALUE_IN: Int = 7
  private val IN_VALUE: Int = 8
  private val NOT_IN_VALUE: Int = 9
  private val STARTS_WITH_VALUE: Int = 10
  private val STARTS_WITH_CASE_INSENSITIVE_VALUE: Int = 11
  private val NOT_STARTS_WITH_VALUE: Int = 12
  private val NOT_STARTS_WITH_CASE_INSENSITIVE_VALUE: Int = 13
  private val VALUE_STARTS_WITH: Int = 14
  private val VALUE_STARTS_WITH_CASE_INSENSITIVE: Int = 15
  private val NOT_VALUE_STARTS_WITH: Int = 16
  private val NOT_VALUE_STARTS_WITH_CASE_INSENSITIVE: Int = 17

  /**
   * Limit the number of results which will be performed.
   *
   * @param limit maximum number of results
   * @return      itself
   */
  def take(limitArg: Int): GenericSearchBuilder[TSearchable] = limit(limitArg)

  /**
   * Limit the number of results which will be performed.
   *
   * @param limit maximum number of results
   * @return      itself
   */
  def limit(limitArg: Int): this.type = {
    limit = Some(limitArg)
    this
  }

  /**
   * Skip initial number of results.
   *
   * @param offset number of skipped results
   * @return       itself
   */
  def skip(offsetArg: Int): GenericSearchBuilder[TSearchable] = offset(offsetArg)
  /**
   * Skip initial number of results.
   *
   * @param offset number of skipped results
   * @return       itself
   */
  def offset(offsetArg: Int): this.type = {
    offset = Some(offsetArg)
    this
  }

  /**
   * Ask server to provide domain: Anys which satisfy defined conditions
   * in requested order if custom order was provided.
   * Limit and offset will be applied on results if provided.
   *
   * @return future to list of found domain: Any
   */
  def search()(implicit locator: ServiceLocator): Future[IndexedSeq[TSearchable]] = {

    val urlParams = Utils.buildArguments(None, limit, offset, order.toMap)
    val httpClient = locator.resolve[HttpClient]
    val domainName: String = httpClient.getDslName[TSearchable]

    httpClient.sendRequestForCollection[TSearchable](
      HttpClientUtil.PUT(filters),
      "Domain.svc/search-generic/" + domainName + urlParams,
      Set(200));
  }

  def orderBy(property: String, direction: Boolean): this.type = {
    require(property ne null, "property can't be null")
    require(property != "", "property can't be empty")

    order += (property -> direction)
    this
  }

  /**
   * Order results ascending by specified property.
   *
   * @param property name of property
   * @return         itself
   */
  def ascending(property: String): this.type = orderBy(property, true)

  /**
   * Order results descending by specified property.
   *
   * @param property name of property
   * @return         itself
   */
  def descending(property: String) = orderBy(property, false)

  private def filter(property: String, id: Int, value: Any) = {
    require(property ne null, "property can't be null")
    require(property != "", "property can't be empty")

    val toAddTo: Buffer[FilterPair] = filters.get(property) match {
      case Some(pairs) => pairs
      case None =>
        val buffer = Buffer.empty[FilterPair]
        filters.put(property, buffer)
        buffer
    }

    toAddTo += FilterPair(id, value)
    this
  }

  /**
   * Define equal (=) condition for specification.
   * Server will return only results that satisfy this and every other specified condition.
   *
   * @param property name of property to compare
   * @param value    check equality with provided value
   * @return         itself
   */
  def equal(property: String, value: Any) = filter(property, EQUALS, value);

  /**
   * Define not equal (!=) condition for specification.
   * Server will return only results that satisfy this and every other specified condition.
   *
   * @param property name of property to compare
   * @param value    check equality with provided value
   * @return         itself
   */
  def nonEqual(property: String, value: Any) = filter(property, NOT_EQUALS, value)

  /**
   * Define less then (<) condition for specification.
   * Server will return only results that satisfy this and every other specified condition.
   *
   * @param property name of property to compare
   * @param value    check ordering with provided value
   * @return         itself
   */
  def lessThen(property: String, value: Any) = filter(property, LESS_THEN, value)

  /**
   * Define less then or equal (<=) condition for specification.
   * Server will return only results that satisfy this and every other specified condition.
   *
   * @param property name of property to compare
   * @param value    check ordering and equality with provided value
   * @return         itself
   */
  def lessThenOrEqual(property: String, value: Any) = filter(property, LESS_THEN_OR_EQUAL, value);

  /**
   * Define greater then (>) condition for specification.
   * Server will return only results that satisfy this and every other specified condition.
   *
   * @param property name of property to compare
   * @param value    check ordering with provided value
   * @return         itself
   */
  def greaterThen(property: String, value: Any) = filter(property, GREATER_THEN, value)

  /**
   * Define greater then or equal (>=) condition for specification.
   * Server will return only results that satisfy this and every other specified condition.
   *
   * @param property name of property to compare
   * @param value    check ordering and equality with provided value
   * @return         itself
   */
  def greaterThenOrEqual(property: String, value: Any) = filter(property, GREATER_THEN_OR_EQUAL, value)

  /**
   * Define in ( value in collection property ) condition for specification.
   * Server will return only results that satisfy this and every other specified condition.
   *
   * @param property name of property to check
   * @param value    check collection for provided value
   * @return         itself
   */
  def in(property: String, value: Any) = filter(property, VALUE_IN, value)

  /**
   * Define not in ( not value in collection property ) condition for specification.
   * Server will return only results that satisfy this and every other specified condition.
   *
   * @param property name of property to check
   * @param value    check collection for provided value
   * @return         itself
   */
  def notIn(property: String, value: Any) = filter(property, NOT_VALUE_IN, value);

  /**
   * Define in [ property in collection value ] condition for specification.
   * Server will return only results that satisfy this and every other specified condition.
   *
   * @param property name of collection property to check
   * @param value    check if property is in provided collection value
   * @return         itself
   */
  def inValue(property: String, value: Any) = filter(property, IN_VALUE, value);

  /**
   * Define in [ not property in collection value ] condition for specification.
   * Server will return only results that satisfy this and every other specified condition.
   *
   * @param property name of collection property to check
   * @param value    check if property is not in provided collection value
   * @return         itself
   */
  def notInValue(property: String, value: Any) = filter(property, NOT_IN_VALUE, value)

  /**
   * Define startsWith [ property.startsWith(value) ] condition for specification.
   * Case sensitive comparison will be performed.
   * Server will return only results that satisfy this and every other specified condition.
   *
   * @param property name of property to check
   * @param value    comparison value
   * @return         itself
   */
  def startsWith(property: String, value: String) = filter(property, STARTS_WITH_VALUE, value)

  /**
   * Define startsWith and case sensitivity [ property.startsWith(value, case sensitivity) ] condition for specification.
   * Server will return only results that satisfy this and every other specified condition.
   *
   * @param property   name of property to check
   * @param value      comparison value
   * @param ignoreCase should string comparison ignore casing
   * @return           itself
   */
  def startsWith(property: String, value: String, ignoreCase: Boolean) =
    if (ignoreCase) filter(property, STARTS_WITH_CASE_INSENSITIVE_VALUE, value)
    else filter(property, STARTS_WITH_VALUE, value)

  /**
   * Define !startsWith [ not property.startsWith(value) ] condition for specification.
   * Case sensitive comparison will be performed.
   * Server will return only results that satisfy this and every other specified condition.
   *
   * @param property name of property to check
   * @param value    comparison value
   * @return         itself
   */
  def doesntStartsWith(property: String, value: String) = filter(property, NOT_STARTS_WITH_VALUE, value)

  /**
   * Define !startsWith and case sensitivity [ not property.startsWith(value, case sensitivity) ] condition for specification.
   * Server will return only results that satisfy this and every other specified condition.
   *
   * @param property   name of property to check
   * @param value      comparison value
   * @param ignoreCase should string comparison ignore casing
   * @return           itself
   */
  def doesntStartsWith(property: String, value: String, ignoreCase: Boolean) = {
    if (ignoreCase) filter(property, NOT_STARTS_WITH_CASE_INSENSITIVE_VALUE, value)
    else filter(property, NOT_STARTS_WITH_VALUE, value)

    /**
     * Define startsWith [ value.startsWith(property) ] condition for specification.
     * Case sensitive comparison will be performed.
     * Server will return only results that satisfy this and every other specified condition.
     *
     * @param property name of property to check
     * @param value    comparison value
     * @return         itself
     */
    def valueStartsWith(property: String, value: String) = filter(property, VALUE_STARTS_WITH, value);
  }

  /**
   * Define startsWith and case sensitivity [ value.startsWith(property, case sensitivity) ] condition for specification.
   * Server will return only results that satisfy this and every other specified condition.
   *
   * @param property   name of property to check
   * @param value      comparison value
   * @param ignoreCase should string comparison ignore casing
   * @return           itself
   */
  def valueStartsWith(property: String, value: String, ignoreCase: Boolean) =
    if (ignoreCase) filter(property, VALUE_STARTS_WITH_CASE_INSENSITIVE, value)
    else filter(property, VALUE_STARTS_WITH, value)

  /**
   * Define !startsWith [ not value.startsWith(property) ] condition for specification.
   * Case sensitive comparison will be performed.
   * Server will return only results that satisfy this and every other specified condition.
   *
   * @param property name of property to check
   * @param value    comparison value
   * @return         itself
   */
  def valueDoesntStartsWith(property: String, value: String) = filter(property, NOT_VALUE_STARTS_WITH, value)

  /**
   * Define !startsWith and case sensitivity [ not value.startsWith(property, case sensitivity) ] condition for specification.
   * Server will return only results that satisfy this and every other specified condition.
   *
   * @param property   name of property to check
   * @param value      comparison value
   * @param ignoreCase should string comparison ignore casing
   * @return           itself
   */
  def valueDoesntStartsWith(property: String, value: String, ignoreCase: Boolean) =
    if (ignoreCase) filter(property, NOT_VALUE_STARTS_WITH_CASE_INSENSITIVE, value)
    else filter(property, NOT_VALUE_STARTS_WITH, value)
}
