package com.dslplatform.api.client

import java.util.Properties

import com.ning.http.util.Base64

class SettingsHeaderProvider(properties: Properties) extends HttpHeaderProvider {

  private val headers: Map[String, String] = Option(properties.getProperty("auth")).map {
    auth => Map("Authorization" -> ("Basic " + auth))
  }.orElse {
    Option(properties.getProperty("username")).flatMap {
      username =>
        Option(properties.getProperty("project-id")).map {
          projectId =>
            val token = username + ':' + projectId
            val basicAuth = "Basic " + new String(Base64.encode(token.getBytes("UTF-8")))
            Map[String, String]("Authorization" -> basicAuth)
        }
    }
  }.getOrElse(Map.empty[String, String])

  override def getHeaders: Map[String, String] = headers
}
