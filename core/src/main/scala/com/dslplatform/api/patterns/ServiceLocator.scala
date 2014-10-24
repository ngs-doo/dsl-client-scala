package com.dslplatform.api.patterns

import scala.reflect.runtime.universe.TypeTag
import java.lang.reflect.Type

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
   * @param tpe   class or interface
   * @return      registered implementation
   */
  def resolve[T](tpe: Type): T

  /**
   * Resolve a service registered in the locator.
   *
   * @param clazz class or interface
   * @return      registered implementation
   */
  def resolve[T](clazz: Class[T]): T =
    resolve[T](clazz.asInstanceOf[Type])

  /**
   * Resolve a service registered in the locator.
   *
   * @tparam T Type info
   * @return  registered implementation
   */
  def resolve[T: TypeTag]: T
}
