package com.dslplatform.api.client

import com.dslplatform.api.patterns.AggregateRoot
import com.dslplatform.api.patterns.Identifiable

import scala.concurrent.Future
import scala.reflect.ClassTag

/**
 * Proxy service to remote CRUD REST-like API.
 * Single aggregate root instance can be used.
 * New object instance will be returned when doing modifications.
 * Use {@link StandardProxy standard proxy} if response is not required from the server.
 * <p>
 * It is preferred to use domain patterns instead of this proxy service.
 */
trait CrudProxy {
  /**
   * Get domain object from remote server using provided identity.
   * If domain object is not found an exception will be thrown.
   *
   * @param uri      domain object identity
   * @return         future to found domain object
   */
  def read[TIdentifiable <: Identifiable: ClassTag](uri: String): Future[TIdentifiable]

  /**
   * Create new aggregate root on the remote server.
   * Created object will be returned with its identity
   * and all calculated properties evaluated.
   *
   * @param aggregate new aggregate root
   * @return          future to aggregate root with new identity
   */
  def create[TAggregateRoot <: AggregateRoot: ClassTag](aggregate: TAggregateRoot): Future[TAggregateRoot]

  /**
   * Modify existing aggregate root on the remote server.
   * Aggregate root will be saved and all calculated properties evaluated.
   *
   * @param aggregate modified aggregate root
   * @return          future to aggregate root with updated attributes
   */
  def update[TAggregateRoot <: AggregateRoot: ClassTag](aggregate: TAggregateRoot): Future[TAggregateRoot]

  /**
   * Delete existing aggregate root from the remote server.
   * If possible, aggregate root will be deleted and it's instance
   * will be provided.
   *
   * @param uri      aggregate root identity
   * @return         future to deleted aggregate root instance
   */
  def delete[TAggregateRoot <: AggregateRoot: ClassTag](uri: String): Future[TAggregateRoot]
}
