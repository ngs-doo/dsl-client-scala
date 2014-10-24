import sbt._
import sbt.Keys._

trait Default {
  val defaultSettings =
    Defaults.defaultSettings ++
    net.virtualvoid.sbt.graph.Plugin.graphSettings ++ Seq(
      scalaVersion := "2.11.2",
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
    )
}

trait Dependencies {
  val scalaReflect = Def.setting { "org.scala-lang" % "scala-reflect" % scalaVersion.value }
  val scalaXml = "org.scala-lang.modules" %% "scala-xml" % "1.0.2"

  // JodaTime
  val jodaTime    = "joda-time" % "joda-time" % "2.5"
  val jodaConvert = "org.joda" % "joda-convert" % "1.7" % "compile"

  // Json serialization
  val jackson = "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.4.3"

  // Logging Facade
  val slf4jApi = "org.slf4j" % "slf4j-api" % "1.7.7"

  //Asynchronous Http and WebSocket Client library
  val asyncHttpClient = "com.ning" % "async-http-client" % "1.8.13"

  // Test Facade
  val spec2 = "org.specs2" %% "specs2" % "2.4.2"
  val junit = "junit" % "junit" % "4.11"

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
      )
    )
  ) dependsOn (core)

  def aggregatedCompile =  ScopeFilter(inProjects(core, http), inConfigurations(Compile))

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
    libraryDependencies                       := libraryDependencies.all(aggregatedCompile).value.flatten
  )

  lazy val root = (project in file(".")) settings ((defaultSettings ++ rootSettings): _*)
}
