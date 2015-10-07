import ReleaseTransformations._
import UnidocKeys._
import scoverage.ScoverageSbtPlugin.ScoverageKeys.coverageExcludedPackages

lazy val buildSettings = Seq(
  organization := "io.github.finagle",
  scalaVersion := "2.11.7",
  crossScalaVersions := Seq("2.10.6", "2.11.7")
)

lazy val compilerOptions = Seq(
  "-deprecation",
  "-encoding", "UTF-8",
  "-feature",
  "-language:existentials",
  "-language:higherKinds",
  "-language:implicitConversions",
  "-unchecked",
  "-Ywarn-dead-code",
  "-Ywarn-numeric-widen",
  "-Xfuture",
  "-Xlint"
)

lazy val baseSettings = Seq(
  libraryDependencies ++= Seq(
    "com.facebook.swift" % "swift-codec" % "0.15.1",
    "com.google.inject" % "guice" % "4.0",
    "com.twitter" %% "finagle-core" % "6.29.0",
    "com.twitter" %% "finagle-thrift" % "6.29.0"
  ) ++ testDependencies.map(_ % "test"),
  scalacOptions ++= compilerOptions ++ (
    CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, 11)) => Seq("-Ywarn-unused-import")
      case _ => Seq.empty
    }
  ),
  scalacOptions in (Compile, console) := compilerOptions,
  resolvers += "Twitter's Repository" at "https://maven.twttr.com/",
  parallelExecution in Test := false
)

lazy val testDependencies = Seq(
  "junit" % "junit" % "4.10",
  "org.mockito" % "mockito-all" % "1.9.5",
  "org.scalacheck" %% "scalacheck" % "1.12.5",
  "org.scalatest" %% "scalatest" % "2.2.5"
)

lazy val allSettings = buildSettings ++ baseSettings ++ publishSettings

lazy val root = project.in(file("."))
  .settings(allSettings)
  .settings(unidocSettings ++ site.settings ++ ghpages.settings)
  .settings(
    site.addMappingsToSiteDir(mappings in (ScalaUnidoc, packageDoc), "docs"),
    git.remoteRepo := "git@github.com:finagle/finagle-swift.git"
  )
  .settings(
    initialCommands in console :=
      """
        |import com.twitter.finagle.Service
        |import com.twitter.util.Future
      """.stripMargin
  )
  .aggregate(core)
  .dependsOn(core)

lazy val core = project
  .settings(moduleName := "finagle-swift")
  .settings(allSettings)

lazy val publishSettings = Seq(
  releaseCrossBuild := true,
  releasePublishArtifactsAction := PgpKeys.publishSigned.value,
  homepage := Some(url("https://github.com/finagle/finagle-swift")),
  licenses := Seq("Apache 2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")),
  publishMavenStyle := true,
  publishArtifact in Test := false,
  pomIncludeRepository := { _ => false },
  publishTo := {
    val nexus = "https://oss.sonatype.org/"
    if (isSnapshot.value)
      Some("snapshots" at nexus + "content/repositories/snapshots")
    else
      Some("releases"  at nexus + "service/local/staging/deploy/maven2")
  },
  autoAPIMappings := true,
  apiURL := Some(url("https://finagle.github.io/finagle-swift/docs/")),
  scmInfo := Some(
    ScmInfo(
      url("https://github.com/finagle/finagle-swift"),
      "scm:git:git@github.com:finagle/finagle-swift.git"
    )
  ),
  pomExtra := (
    <developers>
      <developer>
        <id>mariusae</id>
        <name>Marius Eriksen</name>
        <url>https://twitter.com/marius</url>
      </developer>
    </developers>
  )
)

lazy val noPublishSettings = Seq(
  publish := (),
  publishLocal := (),
  publishArtifact := false
)

lazy val sharedReleaseProcess = Seq(
  releaseProcess := Seq[ReleaseStep](
    checkSnapshotDependencies,
    inquireVersions,
    runClean,
    runTest,
    setReleaseVersion,
    commitReleaseVersion,
    tagRelease,
    publishArtifacts,
    setNextVersion,
    commitNextVersion,
    ReleaseStep(action = Command.process("sonatypeReleaseAll", _)),
    pushChanges
  )
)

credentials ++= (
  for {
    username <- Option(System.getenv().get("SONATYPE_USERNAME"))
    password <- Option(System.getenv().get("SONATYPE_PASSWORD"))
  } yield Credentials(
    "Sonatype Nexus Repository Manager",
    "oss.sonatype.org",
    username,
    password
  )
).toSeq
