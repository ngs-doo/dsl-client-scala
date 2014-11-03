import sbt._
import sbt.Keys._

trait Default {
  val defaultSettings =
    Defaults.coreDefaultSettings ++
    net.virtualvoid.sbt.graph.Plugin.graphSettings ++ Seq(
      scalaVersion := "2.11.4",
      scalacOptions := Seq(
        "-deprecation",
        "-encoding", "UTF-8",
        "-feature",
        "-language:existentials",
        "-language:implicitConversions",
        "-language:postfixOps",
        "-language:reflectiveCalls",
        "-optimise",
        "-unchecked",
        "-Xcheckinit",
        "-Xlint",
        "-Xmax-classfile-name", "72",
        "-Xno-forwarders",
        "-Xverify",
        "-Yclosure-elim",
        "-Yconst-opt",
        "-Ydead-code",
        "-Yinline-warnings",
        "-Yinline",
        "-Yrepl-sync",
        "-Ywarn-adapted-args",
        "-Ywarn-dead-code",
        "-Ywarn-inaccessible",
        "-Ywarn-infer-any",
        "-Ywarn-nullary-override",
        "-Ywarn-nullary-unit",
        "-Ywarn-numeric-widen",
        "-Ywarn-unused"
      ),
      scalacOptions in Test ++= Seq("-Yrangepos")
//      scalacOptions in (Compile, doc) := Seq("-diagrams", "-diagrams-debug")
    )
}

trait Dependencies {
  val scalaReflect = Def.setting { "org.scala-lang" % "scala-reflect" % scalaVersion.value }
  val scalaXml = "org.scala-lang.modules" %% "scala-xml" % "1.0.2"

  // JodaTime
  val jodaTime    = "joda-time" % "joda-time"    % "2.5"
  val jodaConvert = "org.joda"  % "joda-convert" % "1.7" % "compile"

  // Json serialization
  val jackson = "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.4.3"

  // Logging Facade
  val slf4jApi = "org.slf4j" % "slf4j-api" % "1.7.7"

  //Asynchronous Http and WebSocket Client library
  val asyncHttpClient = "com.ning" % "async-http-client" % "1.8.13"

  // Test Facade
  val spec2 = "org.specs2" %% "specs2" % "2.4.2"

  // Logging for testing
  val logback = "ch.qos.logback" % "logback-classic" % "1.1.2"
}

object Projects
    extends Build
    with Default
    with Dependencies {

  lazy val core = Project(
    "core",
    file("core"),
    settings = defaultSettings ++ Seq(
      name := "DSL Client Scala Core",
      libraryDependencies ++= Seq(
        jodaTime,
        jodaConvert,
        scalaReflect.value
      )
    )
  )

  lazy val http = Project(
    "http",
    file("http"),
    settings = defaultSettings ++ Seq(
      name := "DSL Client Scala HTTP",
      libraryDependencies ++= Seq(
        asyncHttpClient,
        jackson,
        slf4jApi,
        scalaXml,
        spec2 % "test",
        logback % "test"
      ),
      unmanagedJars in Test += Revenj.testLibTask.value,
      testOptions in Test ++= Seq(
        Tests.Setup(Revenj.setup.value),
        Tests.Cleanup(Revenj.shutdown.value)
      )
    )
  ) dependsOn core

  def aggregatedCompile = ScopeFilter(inProjects(core, http), inConfigurations(Compile))

  def aggregatedTest = ScopeFilter(inProjects(core, http), inConfigurations(Test))

  def rootSettings = Seq(
    sources in Compile                        := sources.all(aggregatedCompile).value.flatten,
    unmanagedSources in Compile               := unmanagedSources.all(aggregatedCompile).value.flatten,
    unmanagedSourceDirectories in Compile     := unmanagedSourceDirectories.all(aggregatedCompile).value.flatten,
    unmanagedResourceDirectories in Compile   := unmanagedResourceDirectories.all(aggregatedCompile).value.flatten,
    sources in Test                           := sources.all(aggregatedTest).value.flatten,
    unmanagedSources in Test                  := unmanagedSources.all(aggregatedTest).value.flatten,
    unmanagedSourceDirectories in Test        := unmanagedSourceDirectories.all(aggregatedTest).value.flatten,
    unmanagedResourceDirectories in Test      := unmanagedResourceDirectories.all(aggregatedTest).value.flatten,
    libraryDependencies                       := libraryDependencies.all(aggregatedCompile).value.flatten,
    unmanagedJars in Test                     := unmanagedJars.all(aggregatedTest).value.flatten,
    testOptions in Test                       ++= (testOptions in (http, Test)).value,
    parallelExecution in ThisBuild            := false
  )

  lazy val root = (project in file(".")) settings (defaultSettings ++ rootSettings: _*)
}

object Revenj {

  private val scalaclienttest = "scalaclienttest"
  private val packageName = "com.dslplatform.test"

  private var revenjProcess: Option[Process] = None

  private val credentials = System.getProperty("user.home") + "/.config/dsl-platform/test.credentials"

  private val testLib: Def.Initialize[File] = Def.setting {
    baseDirectory.value / "test-lib" / "scala-client.jar"
  }

  private val revenjExe: Def.Initialize[File] = Def.setting {
    baseDirectory.value / "revenj" / "Revenj.Http.exe"
  }

  private val testDll: Def.Initialize[File] = Def.setting {
    baseDirectory.value / "revenj" / "test.dll"
  }

  val testLibTask: Def.Initialize[Task[File]] = Def.taskDyn {
    makeScalaClientTestJar.value
    Def.task { testLib.value }
  }

  private def makeRevenj = Def.task {
    val base = baseDirectory.value
    val basePath = base.getAbsolutePath
    if (!testDll.value.exists()) {
      com.dslplatform.compiler.client.Main.main(Array(
        s"-revenj=${basePath}/revenj/test.dll",
        s"-dependency:revenj=${basePath}/revenj",
        s"-namespace=$packageName",
        s"-db=localhost:5432/$scalaclienttest?user=$scalaclienttest&password=$scalaclienttest",
        s"-dsl=${basePath}/src/test/resources/dsl",
        "-download",
        "-no-colors",
        "-apply",
        s"-properties=$credentials"))
      IO.copyFile(
        base / "test-lib" / "Revenj.Http.exe.config",
        base / "revenj" / "Revenj.Http.exe.config"
      )
    }
  }

  private def makeScalaClientTestJarCall(f: File) =
    com.dslplatform.compiler.client.Main.main(Array(
      s"-scala_client=${f.getAbsolutePath}",
      s"-namespace=$packageName",
      "-no-colors",
      "-dsl=http/src/test/resources/dsl",
      "-download",
      "-active-record",
      s"-properties=$credentials"))

  private def makeScalaClientTestJar = Def.task {
    val testLibFile: File = testLib.value
    if (!testLibFile.getParentFile.exists()) testLibFile.getParentFile.mkdir()
    if (!testLibFile.exists()) makeScalaClientTestJarCall(testLibFile)
  }

  private def startRevenj = Def.task {
    scala.util.Try {
      if (sys.props("os.name").toLowerCase(java.util.Locale.ENGLISH).contains("windows")) {
        Seq(revenjExe.value.getPath).run
      }
      else {
        Seq("mono", revenjExe.value.getPath).run
      }
    }
  }

  def setup: Def.Initialize[Task[() => Unit]] = Def.taskDyn {
    makeScalaClientTestJar.value
    makeRevenj.value
    Def.taskDyn {
      revenjProcess = startRevenj.value.toOption
      Def.task { () => () }
    }
  }

  def shutdown: Def.Initialize[Task[() => Unit]] = Def.task {
    () =>
      revenjProcess.map(_.destroy).getOrElse()
  }
}