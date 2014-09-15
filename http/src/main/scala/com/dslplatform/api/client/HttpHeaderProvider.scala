package com.dslplatform.api.client

trait HttpHeaderProvider {
  def getHeaders: Map[String, String]
}
