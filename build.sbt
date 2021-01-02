import sbt.Keys._
import sbt.file

val http4sVersion = "0.21.4"
val specs2Version = "4.10.3"
val slf4jVersion = "1.7.30"
val logbackVersion = "1.2.3"
val kindProjectorVersion = "0.11.0"
val enumeratumCirceVersion = "1.6.1"
val circeVersion = "0.13.0"
val ficusVersion = "1.5.0"
val akkaVersion = "2.6.10"
val doobieVersion = "0.9.0"
val pureConfigVersion = "0.12.3"
val flywayVersion = "6.3.1"
val scalaTestVersion = "3.1.1"
val scalaMockVersion = "4.4.0"
val h2Version = "1.4.200"

val `root` = project.in(file("."))
  .enablePlugins(GuardrailPlugin)
  .enablePlugins(WartRemover)
  .enablePlugins(JavaAppPackaging, DockerPlugin)
  .enablePlugins(NativeImagePlugin)
//  .enablePlugins(GraalVMNativeImagePlugin)
  .settings(organization := "nl.pragmasoft.catracker",
    scalaVersion := "2.13.3",
    testOptions += Tests.Argument(TestFrameworks.JUnit, "-v"),
    dockerBaseImage := "openjdk:11",
    dockerUpdateLatest := true,
    dockerExposedPorts ++= Seq(8081),
    packageName in Docker := "catracker",
    mainClass in Compile := Some("nl.pragmasoft.catracker.Main"),
    version in Docker := version.value,
    scalacOptions := Seq(
      "-deprecation",
      "-encoding", "UTF-8",
      "-language:higherKinds",
      "-language:postfixOps",
      "-feature",
      "-Xfatal-warnings"
    ),
//    graalVMNativeImageOptions ++= Seq(
    nativeImageOptions ++= Seq(
      "--report-unsupported-elements-at-runtime",
      "-H:+ReportExceptionStackTraces",
      "--verbose",
      "--allow-incomplete-classpath",
      "-Dio.netty.noUnsafe=true",
      "--no-fallback",
      "-H:+AddAllCharsets",
      s"-H:ConfigurationFileDirectories=${baseDirectory.in(ThisBuild).value}/graalvm-conf/",
      "--enable-all-security-services",
      "--enable-url-protocols=https",
      "-H:Log=registerResource:",
      "-H:IncludeResources=.*",
      "-H:EnableURLProtocols=http",
      "--initialize-at-build-time",
      "--initialize-at-run-time=org.h2.store.fs.FileNioMemData,org.flywaydb.core.internal.scanner.classpath.jboss.JBossVFSv3ClassPathLocationScanner,io.netty.handler.ssl.util.BouncyCastleSelfSignedCertGenerator,io.netty.handler.ssl.ReferenceCountedOpenSslClientContext,io.netty.handler.ssl.ReferenceCountedOpenSslServerContext,io.netty.handler.ssl.JdkNpnApplicationProtocolNegotiator,io.netty.handler.ssl.JdkAlpnApplicationProtocolNegotiator,io.netty.handler.ssl.util.ThreadLocalInsecureRandom,io.netty.handler.ssl.JettyNpnSslEngine,io.netty.handler.ssl.ReferenceCountedOpenSslEngine,io.netty.handler.ssl.ConscryptAlpnSslEngine,io.netty.handler.ssl.JettyAlpnSslEngine$ClientEngine,io.netty.handler.ssl.JettyAlpnSslEngine$ServerEngine,io.netty.util.internal.ObjectCleaner,io.netty.handler.ssl.util.InsecureTrustManagerFactory"
    ),
    packageOptions in(Compile, packageBin) +=
      Package.ManifestAttributes(
        "Build-Time" -> new java.util.Date().toString,
        "Build-Commit" -> git.gitHeadCommit.value.getOrElse("No Git Revision Found")
      ),
    sources in doc := Seq.empty,
    publishArtifact in packageDoc := false,
    resolvers += Resolver.bintrayRepo("cakesolutions", "maven"),
    guardrailTasks in Compile := List(
      ScalaServer(
        file("api.yaml"),
        pkg = "nl.pragmasoft.catracker.http",
        framework = "http4s",
        tracing = false)
    ),
    assemblyMergeStrategy in assembly := {
      case PathList("reference.conf") => MergeStrategy.concat
      case x =>
        val oldStrategy = (assemblyMergeStrategy in assembly).value
        oldStrategy(x)
    },
    libraryDependencies ++= Seq(
      compilerPlugin("org.typelevel" % "kind-projector_2.13.1" % kindProjectorVersion),

      "org.http4s" %% "http4s-blaze-server" % http4sVersion,
      "org.http4s" %% "http4s-blaze-client" % http4sVersion,
      "org.http4s" %% "http4s-circe" % http4sVersion,
      "org.http4s" %% "http4s-dsl" % http4sVersion,
      "org.http4s" %% "http4s-prometheus-metrics" % http4sVersion,
      "com.h2database" % "h2" % h2Version,
      "mysql" % "mysql-connector-java" % "5.1.12",
      "io.circe" %% "circe-core" % circeVersion,
      "io.circe" %% "circe-generic" % circeVersion,
      "io.circe" %% "circe-parser" % circeVersion,
      "org.flywaydb" % "flyway-core" % flywayVersion,
      "com.typesafe.akka" %% "akka-stream" % akkaVersion,
      "com.typesafe.akka" %% "akka-actor" % akkaVersion
        excludeAll ExclusionRule("org.scala-lang.modules", "scala-java8-compat_2.12"),

      "com.beachape" %% "enumeratum-circe" % enumeratumCirceVersion,
      "ch.qos.logback" % "logback-classic" % logbackVersion,
      "com.typesafe.scala-logging" %% "scala-logging" % "3.9.2",

      "com.github.pureconfig" %% "pureconfig" % pureConfigVersion,
      "com.github.pureconfig" %% "pureconfig-cats-effect" % pureConfigVersion,

      "org.tpolecat" %% "doobie-core" % doobieVersion,
      "org.tpolecat" %% "doobie-hikari" % doobieVersion,
      "org.tpolecat" %% "doobie-h2" % doobieVersion % Test,
      "org.tpolecat" %% "doobie-specs2" % doobieVersion % Test,

      "org.http4s" %% "http4s-testing" % http4sVersion % Test,
      "org.scalatest" %% "scalatest" % scalaTestVersion % Test,
      "org.scalamock" %% "scalamock" % scalaMockVersion % Test
    )
  )
