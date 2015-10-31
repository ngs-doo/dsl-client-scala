package com.dslplatform.test

import java.io.ByteArrayInputStream

import com.dslplatform.api.client.{HttpClient, HttpHeaderProvider, SettingsHeaderProvider}
import org.specs2.mutable._

class AuthHeaderTest extends Specification {

  override def is = s2"""
    Header Provider is resolved from the ServiceLocator
      provide with auth header                    $auth
      provide with project id                     $pid
      provide custom                              $custom
"""

  private val withAuthHeader =
    """
      |auth=someAuth
      |api-url=https://dsl-platform.com/test
      |package-name=model
    """.stripMargin

  private val withPidHeader =
    """
      |username=user
      |project-id=0-0-0-0-0
      |api-url=https://dsl-platform.com/test
      |package-name=model
    """.stripMargin

  def auth = {
    val properties = new java.util.Properties()
    properties.load(new ByteArrayInputStream(withAuthHeader.getBytes("UTF-8")))
    val locator = com.dslplatform.api.client.Bootstrap.init(properties)
    try {
      locator.resolve[SettingsHeaderProvider].getHeaders("Authorization").contains("someAuth") must beTrue
    } finally {
      locator.resolve[HttpClient].shutdown()
    }
  }

  def pid = {
    val properties = new java.util.Properties()
    properties.load(new ByteArrayInputStream(withPidHeader.getBytes("UTF-8")))
    val locator = com.dslplatform.api.client.Bootstrap.init(properties)
    try {
      locator.resolve[SettingsHeaderProvider].getHeaders.get("Authorization") must beSome
    } finally {
      locator.resolve[HttpClient].shutdown()
    }
  }

  def custom = {
    val locator = com.dslplatform.api.client.Bootstrap.init("/test-project.props",
      Map[Object, AnyRef](classOf[HttpHeaderProvider] -> new HttpHeaderProvider {
        override def getHeaders: Map[String, String] = Map("Do" -> "More")
      }))
    try {
      locator.resolve[HttpHeaderProvider].getHeaders("Do") === "More"
    } finally {
      locator.resolve[HttpClient].shutdown()
    }
  }
}
