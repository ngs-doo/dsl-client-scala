version in ThisBuild              := "0.1.2-SNAPSHOT"

organization in ThisBuild         := "com.dslplatform"

publishTo in ThisBuild            := Some(if (version.value endsWith "SNAPSHOT") Opts.resolver.sonatypeSnapshots else Opts.resolver.sonatypeStaging)

licenses in ThisBuild             += ("BSD-style", url("http://opensource.org/licenses/BSD-3-Clause"))

startYear in ThisBuild            := Some(2013)

scmInfo in ThisBuild              := Some(ScmInfo(url("https://github.com/ngs-doo/dsl-client-scala.git"), "scm:git:https://github.com/ngs-doo/dsl-client-scala.git"))

pomExtra in ThisBuild             ~= (_ ++ {Developers.toXml})

publishMavenStyle in ThisBuild    := true

pomIncludeRepository in ThisBuild := { _ => false }

homepage in ThisBuild             := Some(url("https://dsl-platform.com/"))

packagedArtifacts                 := Map.empty

credentials in ThisBuild          ++= {
                                    val creds = Path.userHome / ".config" / "dsl-client-java" / "element.sonatype"
                                    if (creds.exists) Some(Credentials(creds)) else None
                                  }.toSeq 