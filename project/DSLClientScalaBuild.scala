import sbt._
import sbt.Keys._

import com.typesafe.sbteclipse.plugin.EclipsePlugin._
import net.virtualvoid.sbt.graph.Plugin._

trait Default {
  val defaultSettings =
    Defaults.defaultSettings ++
    eclipseSettings ++
    graphSettings ++ Seq(
      javaHome := sys.env.get("JDK16_HOME").map(file(_)),
      javacOptions := Seq(
        "-deprecation",
        "-encoding", "UTF-8",
        "-Xlint:unchecked",
        "-source", "1.6",
        "-target", "1.6"
      ),
      crossScalaVersions := Seq("2.10.4", "2.11.2"),
      scalaVersion := crossScalaVersions.value.last,
      scalacOptions := Seq(
        "-unchecked",
        "-deprecation",
        "-optimise",
        "-encoding", "UTF-8",
        "-Xcheckinit",
        "-Yclosure-elim",
        "-Ydead-code",
        "-Yinline",
        "-Xmax-classfile-name", "72",
        "-Yrepl-sync",
        "-Xlint",
        "-Xverify",
        "-Yinline-warnings",
        "-feature",
        "-language:postfixOps",
        "-language:reflectiveCalls",
        "-language:implicitConversions",
        "-language:existentials"
      ),
      scalacOptions in Test ++= Seq("-Yrangepos"),
      javacOptions in doc := Seq("-source", "1.6"),
      unmanagedSourceDirectories in Compile := Seq((scalaSource in Compile).value),
      unmanagedSourceDirectories in Test := Seq((scalaSource in Test).value),
      EclipseKeys.executionEnvironment := Some(EclipseExecutionEnvironment.JavaSE16),
      EclipseKeys.eclipseOutput := Some(".target")
    )
}

trait Dependencies {
  val scalaReflect = Def.setting { "org.scala-lang" % "scala-reflect" % scalaVersion.value }
  val scalaXML = "org.scala-lang.modules" %% "scala-xml" % "1.0.2"
  // JodaTime
  val jodaTime    = "joda-time" % "joda-time" % "2.4"
  val jodaConvert = "org.joda" % "joda-convert" % "1.7" % "compile"

  // Json serialization
  val jackson         = "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.4.2"
  val jacksonDatabind = "com.fasterxml.jackson.core" % "jackson-databind" % "2.4.2"

  // Logging Facade
  val slf4j = "org.slf4j" % "slf4j-api" % "1.7.7"

  //Asynchronous Http and WebSocket Client library
  val asyncHttpClient = "com.ning" % "async-http-client" % "1.8.13"

  // Test Facade
  val spec2 = "org.specs2" %% "specs2" % "2.4.2"
  val junit = "junit" % "junit" % "4.11"

  // Logging for testing
  val logback = "ch.qos.logback" % "logback-classic" % "1.0.13"
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
        jacksonDatabind,
        slf4j,
        spec2 % "test",
        logback % "test"
      ) ++ (CrossVersion.partialVersion(scalaVersion.value) match {
        case Some((2, x)) if x > 10 =>
          Seq(scalaXML)
        case _ => Seq.empty
      }
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

  val root = (project in file(".")) settings ((defaultSettings ++ rootSettings): _*)
}
