package com.dslplatform.api.client

import com.dslplatform.api.patterns.ServiceLocator

import java.io.IOException
import org.slf4j.Logger

import com.ning.http.util.Base64
import com.ning.http.client.RequestBuilder
import com.ning.http.client.AsyncCompletionHandler
import com.ning.http.client.Response
import com.ning.http.client.AsyncHttpClient
import com.ning.http.client.AsyncHttpClientConfig

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.concurrent.Promise
import scala.reflect.ClassTag

private[client] object HttpClientUtil {

  type Headers = Map[String, Set[String]]

  def encode(uri: String) = java.net.URLEncoder.encode(uri, "UTF-8")

  implicit class implicitStringSlashString(str: String) {
    def /(sec: String) = str + "/" + sec
  }

  trait HttpMethod { def name = toString }

  trait NoBodyRequest extends HttpMethod

  trait WithBody[TArg] extends HttpMethod {
    def arg: TArg
    def body(implicit json: JsonSerialization): Array[Byte] = json.serialize(arg).getBytes()
  }

  case object GET extends NoBodyRequest
  case object DELETE extends NoBodyRequest
  case class POST[TArg](arg: TArg) extends WithBody[TArg] {
    override val name = "POST"
  }
  case class PUT[TArg](arg: TArg) extends WithBody[TArg] {
    override val name = "PUT"
  }
}

class HttpClient(
    locator: ServiceLocator,
    projectSettings: ProjectSettings,
    json: JsonSerialization,
    logger: Logger,
    executorService: java.util.concurrent.ExecutorService) {

  import HttpClientUtil._
  private val remoteUrl = Option(projectSettings.get("api-url")).getOrElse(
    throw new RuntimeException("Missing api-url from the properties.")
  )
  private val domainPrefix = Option(projectSettings.get("package-name")).getOrElse {
    logger.warn("Could not find package-name in the properties defaulting to \"\"")
    ""
  }
  private val domainPrefixLength = if (domainPrefix.length > 0) domainPrefix.length + 1 else 0
  private val basicAuth = makeBasicAuth
  private val MimeType = "application/json"
  private [client] implicit val ec = ExecutionContext.fromExecutorService(executorService)
  private val commonHeaders: Headers = Map(
    "Accept" -> Set(MimeType),
    "Content-Type" -> Set(MimeType)
  ) ++ makeBasicAuth

  private def makeBasicAuth: Headers = {
    val username = projectSettings.get("username")
    val password = projectSettings.get("project-id")

    if (username == null || password == null) Map.empty
    else {
      val token = username + ':' + password
      val basicAuth = "Basic " + new String(Base64.encode(token.getBytes("UTF-8")))
      Map("Authorization" -> Set(basicAuth))
    }
  }

  private[client] def getDslName[T](implicit ct: ClassTag[T]): String =
    getDslName(ct.runtimeClass)

  private[client] def getDslName(clazz: Class[_]): String =
    clazz.getName.substring(domainPrefixLength).replace("$", "")

  private val configBuilder = new AsyncHttpClientConfig.Builder()
  configBuilder.setExecutorService(executorService)

  private val config = configBuilder.build()
  private val ahc = new AsyncHttpClient(config)
  // ---------------------------

  if (logger.isDebugEnabled()) {
    logger.debug("""Initialized with:
    username [{}]
    api: [{}]
    pid: [{}]""",
      projectSettings.get("username"),
      projectSettings.get("api-url"),
      projectSettings.get("project-id"));
  }

  private def makeNingHeaders(additionalHeaders: Map[String, Set[String]]): java.util.Map[String, java.util.Collection[String]] = {
    val headers = new java.util.HashMap[String, java.util.Collection[String]]()

    for (h <- commonHeaders ++ additionalHeaders) {
      if (logger.isTraceEnabled) logger.trace("Added header: %s:%s" format (h._1, h._2.mkString("[", ",", "]")))
      headers.put(h._1, scala.collection.JavaConversions.asJavaCollection(h._2))
    }
    headers
  }

  private def httpResponseHandler(
    resp: Promise[Array[Byte]], expectedHeaders: Set[Int]) =
    new AsyncCompletionHandler[Unit] {

      def onCompleted(response: Response) {
        if (logger.isTraceEnabled) logger.trace("Received response status[%s] body: %s" format (response.getStatusCode(), response.getResponseBody()))
        if (expectedHeaders contains response.getStatusCode()) {
          resp success response.getResponseBodyAsBytes()
        }
        else {
          resp failure new IOException(
            "Unexpected return code: "
              + response.getStatusCode()
              + ", response: "
              + response.getResponseBody())
        }
      }

      override def onThrowable(t: Throwable) {
        resp failure t
      }
    }

  private def doRequest(
    method: String,
    optBody: Option[Array[Byte]],
    url: String,
    expectedStatus: Set[Int],
    additionalHeaders: Headers): Future[Array[Byte]] = {
    val request = new RequestBuilder()
      .setUrl(url)
      .setHeaders(makeNingHeaders(additionalHeaders))
      .setMethod(method)

    if (logger.isTraceEnabled) logger.trace("Sending request %s [%s]" format (method, url))
    optBody.foreach {
      body =>
        logger.trace("payload: {}", new String(body, "UTF-8"))
        request.setBody(body)
    }

    val promisedResponse = Promise[Array[Byte]]

    ahc.executeRequest(request.build(),
      httpResponseHandler(promisedResponse, expectedStatus))

    promisedResponse future
  }

  def sendRawRequest(
    method: HttpMethod,
    service: String,
    expectedStatus: Set[Int],
    additionalHeaders: Map[String, Set[String]] = Map.empty): Future[Array[Byte]] = {

    val url = remoteUrl + service

    val optBody = method match {
      case wb: WithBody[_] => Some(wb.body(json))
      case _               => None
    }

    doRequest(method.name, optBody, url, expectedStatus, additionalHeaders)
  }

  private[client] def sendStandardRequest(
    method: HttpMethod,
    service: String,
    expectedStatus: Set[Int],
    additionalHeaders: Map[String, Set[String]] = Map.empty): Future[IndexedSeq[String]] = {

    val url = remoteUrl + service

    val optBody = method match {
      case wb: WithBody[_] => Some(wb.body(json))
      case _               => None
    }

    doRequest(method.name, optBody, url, expectedStatus, additionalHeaders) map (json.deserializeList[String])
  }

  private[client] def sendRequest[TResult: ClassTag](
    method: HttpMethod,
    service: String,
    expectedStatus: Set[Int],
    additionalHeaders: Map[String, Set[String]] = Map.empty): Future[TResult] =
    sendRawRequest(method, service, expectedStatus, additionalHeaders) map (json.deserialize[TResult])

  private[client] def sendRequestForCollection[TResult: ClassTag](
    method: HttpMethod,
    service: String,
    expectedStatus: Set[Int],
    additionalHeaders: Map[String, Set[String]] = Map.empty): Future[IndexedSeq[TResult]] =
    sendRawRequest(method, service, expectedStatus, additionalHeaders) map (json.deserializeList[TResult])

  private[client] def sendRequestForCollection[TResult](
    returnClass: Class[_],
    method: HttpMethod,
    service: String,
    expectedStatus: Set[Int],
    additionalHeaders: Map[String, Set[String]]): Future[IndexedSeq[TResult]] =
    sendRawRequest(method, service, expectedStatus, additionalHeaders) map (json.deserializeList(returnClass, _))

  def shutdown() {
    ahc.close()
  }
}
