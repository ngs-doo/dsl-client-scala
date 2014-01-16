package com.dslplatform.api.patterns;

import scala.reflect.runtime.universe._

/**
 * Service for resolving other services.
 * One locator per project should be used.
 * <p>
 * When multiple projects are used, locator must be passed around
 * to resolve appropriate service.
 * <p>
 * Custom classes can be resolved if their dependencies can be satisfied.
 */
trait ServiceLocator {
  /**
   * Resolve a service registered in the locator.
   *
   * @param clazz class or interface
   * @return      registered implementation
   */
  def resolve[T](clazz: Class[T]): T

  /**
   * Resolve a service registered in the locator
   *
   * @param T	Type info
   * @return	registered implementation
   */
  def resolve[T: TypeTag]: T
}
