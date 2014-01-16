package com.dslplatform.api.patterns;

import scala.concurrent.Future

/**
 * Service for finding Identifiable domain objects.
 * Finding domain objects using their URI identity is the fastest way
 * retrieve an object from the remote server.
 *
 * @param [T] IIdentifiable domain object type
 */
trait Repository[T <: Identifiable] extends SearchableRepository[T] {

  /**
   * Returns a Seq of domain objects uniquely represented with their URIs.
   * Only found objects will be returned (Seq will be empty if no objects are found).
   *
   * @param uris sequence of unique identifiers
   * @return     future to found domain objects
   */
  def find(uris: TraversableOnce[String]): Future[IndexedSeq[T]]

  /** @see Repository#find(TraversableOnce) */
  def find(uris: String*): Future[IndexedSeq[T]]

  /**
   * Returns a domain object uniquely represented with its URI.
   * If object is not found, an exception will be thrown
   *
   * @param uri domain object identity
   * @return    future to found domain object
   */
  def find(uri: String): Future[T]
}
