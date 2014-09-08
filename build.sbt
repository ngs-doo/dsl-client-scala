version               := "0.9.0"

name                  := "dsl-client-scala"

organization          := "com.dslplatform"

publishTo             := Some(if (version.value endsWith "SNAPSHOT") Opts.resolver.sonatypeSnapshots else Opts.resolver.sonatypeStaging)

licenses              += ("BSD-style", url("http://opensource.org/licenses/BSD-3-Clause"))

startYear             := Some(2013)

scmInfo               := Some(ScmInfo(url("https://github.com/ngs-doo/dsl-client-scala.git"), "scm:git:https://github.com/ngs-doo/dsl-client-scala.git"))

pomExtra              ~= (_ ++ {Developers.toXml})

publishMavenStyle     := true

pomIncludeRepository  := { _ => false }

homepage              := Some(url("https://dsl-platform.com/"))

credentials           += Credentials(Path.userHome / ".config" / "sonatype" / "element")