package com.dslplatform.api.client

import scala.concurrent.Future
import scala.reflect.ClassTag

/** Proxy service to remote RPC-like API.
  *
  * Remote services can be called using their name.
  */
trait ApplicationProxy {
  /** If remote service doesn't require any arguments it can be called using get method.
    * Provide class of result for deserialization.
    *
    * @tparam TResult       result type
    * @param command        remote service name
    * @param expectedStatus expected status from remote call
    * @return               future with deserialized result
    */
  def get[TResult: ClassTag](
      command: String,
      expectedStatus: Set[Int]): Future[TResult]

  /** When remote service require an argument message with serialized payload will be sent.
    * Provide class of result for deserialization.
    *
    * @tparam TResult       result type
    * @param command        remote service name
    * @param argument       remote service argument
    * @param expectedStatus expected status from remote call
    * @return               future with deserialized result
    */
  def post[TArgument, TResult: ClassTag](
      command: String,
      argument: TArgument,
      expectedStatus: Set[Int]): Future[TResult]
}
