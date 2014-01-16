import sbt._
import Keys._

import com.typesafe.sbteclipse.plugin.EclipsePlugin._
import net.virtualvoid.sbt.graph.Plugin._
import sbtassembly.Plugin._, AssemblyKeys._

object Default {
  val settings =
    Defaults.defaultSettings ++
    eclipseSettings ++
    assemblySettings ++
    graphSettings ++
    Seq(
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
        //, "-explaintypes",
        "-Xcheckinit",
        //, "-Xfatal-warnings",
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
        "-language:implicitConversions",
        "-language:existentials"
      ),
      scalacOptions in Test ++= Seq("-Yrangepos"),
      javacOptions in doc := Seq("-source", "1.6"),
      unmanagedSourceDirectories in Compile := (scalaSource in Compile).value :: Nil,
      unmanagedSourceDirectories in Test := (scalaSource in Test).value :: Nil,
      EclipseKeys.projectFlavor := EclipseProjectFlavor.Scala,
      EclipseKeys.createSrc := EclipseCreateSrc.Default + EclipseCreateSrc.Resource
    )
}

object Dependencies {
  val scalaReflect = Def.setting { "org.scala-lang" % "scala-reflect" % scalaVersion.value }

  // JodaTime
  val jodaTime    = "joda-time" % "joda-time" % "2.3"
  val jodaConvert = "org.joda" % "joda-convert" % "1.5" % "compile"

  // Json serialization
  val jackson = "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.3.0"
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

object Projects extends Build {
  import Default._
  import Dependencies._

  lazy val core = Project(
    "core",
    file("core"),
    settings = Default.settings ++ Seq(
      name := "DSL-Client-Scala-Core",
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
    settings = Default.settings ++ Seq(
      name := "DSL-Client-Scala-HTTP",
      libraryDependencies ++= Seq(
        asyncHttpClient,
        jackson,
        paranamer,
        slf4j,
        spec2,
        junit,
        logback
      )
    )
  ) dependsOn(core)

  lazy val root = Project(
    "root",
    file("."),
    settings = Default.settings ++ Seq(
      name := "DSL-Client-Scala",
      mainClass in assembly := Some("com.dslplatform.api.client.Bootstrap"),
      jarName in assembly := "dsl-client-scala_%s-%s.jar" format(scalaVersion.value, version.value),
      excludedJars in assembly := (fullClasspath in assembly).value.filter(_.data.getName endsWith ".jar")
    )
  ) aggregate(core, http)
}
