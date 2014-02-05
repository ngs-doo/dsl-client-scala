import sbt._
import Keys._

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
      crossScalaVersions := Seq("2.10.4-RC1"),
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
        "-Ywarn-all",
        "-Yinline-warnings",
        "-feature",
        "-language:postfixOps",
        "-language:reflectiveCalls",
        "-language:implicitConversions",
        "-language:existentials"
      ),
      scalacOptions in Test ++= Seq("-Yrangepos"),
      javacOptions in doc := Seq("-source", "1.6"),
      unmanagedSourceDirectories in Compile := (scalaSource in Compile).value :: Nil,
      unmanagedResourceDirectories in Compile := Nil,
      unmanagedSourceDirectories in Test := Nil,
      unmanagedResourceDirectories in Test := Nil,
      EclipseKeys.executionEnvironment := Some(EclipseExecutionEnvironment.JavaSE16),
      EclipseKeys.eclipseOutput := Some(".target")
    )
}

trait Dependencies {
  val scalaReflect = Def.setting { "org.scala-lang" % "scala-reflect" % scalaVersion.value }

  // JodaTime
  val jodaTime    = "joda-time" % "joda-time" % "2.3"
  val jodaConvert = "org.joda" % "joda-convert" % "1.5" % "compile"

  // Json serialization
  val jackson         = "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.3.1"
  val jacksonDatabind = "com.fasterxml.jackson.core" % "jackson-databind" % "2.3.1"

  // do not remove! fix for Array[Byte] deserialization
  val paranamer = "com.thoughtworks.paranamer" % "paranamer" % "2.6"

  // Logging Facade
  val slf4j = "org.slf4j" % "slf4j-api" % "1.7.5"

  //Asynchronous Http and WebSocket Client library
  val asyncHttpClient = "com.ning" % "async-http-client" % "1.7.23"

  // Apache commons
  val commonsIo    = "commons-io" % "commons-io" % "2.4"
  val commonsCodec = "commons-codec" % "commons-codec" % "1.9"

  // Test Facade
  val spec2 = "org.specs2" %% "specs2" % "2.3.7" % "test"
  val junit = "junit" % "junit" % "4.11" % "test"

  // Logging for testing
  val logback = "ch.qos.logback" % "logback-classic" % "1.0.13" % "test"
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
        paranamer,
        slf4j
      )
    )
  ) dependsOn(core)

  lazy val test = Project(
    "test",
    file("test"),
    settings = defaultSettings ++ Seq(
      name := "DSL Client Scala Test",
      libraryDependencies ++= Seq(
        spec2,
        junit,
        logback
      ),
      unmanagedSourceDirectories in Compile := Seq(
        sourceDirectory.value / "generated" / "scala"
      ),
      unmanagedResourceDirectories in Compile := Seq(
        sourceDirectory.value / "generated" / "resources",
        sourceDirectory.value / "main" / "resources"
      ),
      unmanagedSourceDirectories in Test := Seq(
        (scalaSource in Test).value
      ),
      EclipseKeys.createSrc := EclipseCreateSrc.Default + EclipseCreateSrc.Resource
    )
  ) dependsOn(core, http)

  lazy val root = Project(
    "root",
    file("."),
    settings = defaultSettings ++ Seq(
      name := "DSL Client Scala"
    )
  ) aggregate(core, http, test)
}
