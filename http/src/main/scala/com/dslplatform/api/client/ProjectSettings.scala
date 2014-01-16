package com.dslplatform.api.client

import java.util.Properties
import java.io.InputStream
/**
 * Project.ini key->value pairs
 *  Stream to project.ini file
 *
 * @param iniStream    project.ini stream
 * @throws IOException in case of error reading stream
 */
class ProjectSettings(iniStream: InputStream) {
  val properties: Properties = new Properties()
  properties.load(iniStream)

  /**
   * get value for provided property in project.ini
   * property = value
   *
   * @param property key
   * @return         found value
   */
  def get(property: String): String = {
    properties.getProperty(property)
  }
}
