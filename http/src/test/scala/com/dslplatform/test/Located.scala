package com.dslplatform.test

import com.dslplatform.api.client.HttpClient
import com.dslplatform.api.patterns.{AggregateRoot, PersistableRepository, ServiceLocator}
import org.specs2.specification.{Outside, Scope}

import scala.concurrent.duration._
import scala.concurrent.{Await, Awaitable, ExecutionContext}
import scala.reflect.runtime.universe.TypeTag

trait WithLocator {
  val locator = com.dslplatform.api.client.Bootstrap.init("/test-project.props")
}

class Located extends Outside[ServiceLocator] with Scope with WithLocator with Close with Clean {
  def outside: ServiceLocator = locator

  def resolved[D: TypeTag] = new Outside[D] {
    override def outside: D = locator.resolve[D]
  }
}

trait Close { this: WithLocator =>
  def close() = locator.resolve[HttpClient].shutdown()
}

trait Clean  { this: WithLocator =>
  def clean[T <: AggregateRoot: TypeTag] = {
    val repository = locator.resolve[PersistableRepository[T]]
    Await.result(repository.search().map(all => repository.delete(all))(locator.resolve[ExecutionContext]), Duration(10, SECONDS))
  }
}

trait Common {
  implicit val duration10 = Duration(10, SECONDS)

  def await[R](awaitable: Awaitable[R]) = Await.result(awaitable, duration10)

  def simpleCleanRepository[T <: AggregateRoot: TypeTag](locator: ServiceLocator) = {
    val repository = locator.resolve[PersistableRepository[T]]

    await(repository.search().map(all => repository.delete(all))(locator.resolve[ExecutionContext]))
  }

  def rName = "name " + rString
  def rString: String = rString(100000)
  def rString(x: Int ) = rInt(x).toString
  def rInt: Int = rInt(100000)
  def rInt(x: Int) = scala.util.Random.nextInt(x)
  def rFloat = scala.util.Random.nextFloat()
  def rDouble = scala.util.Random.nextDouble()
}
