import sbt.*

/** Every library version in one place. Shared by all variants: a generated project gets this file unchanged and its build.sbt only picks modules from
  * it.
  */
object Dependencies {

  object V {

    val caliban = "3.1.5"
    val flyway = "13.7.0"
    val hikari = "7.1.0"
    val logback = "1.6.3"
    val mariadb = "3.5.10"
    val quill = "4.8.6"
    val scalablytypedRuntime = "2.4.2"
    val scalaJavaTime = "2.7.0"
    val scalajsDom = "2.8.1"
    // 4.x is written for React 19 (client/package.json and stLib/package.json).
    val scalajsReact = "4.0.0"
    val sttpClient4 = "4.0.26"
    val testcontainers = "0.44.1"
    val zio = "2.1.26"
    val zioConfig = "4.1.0"
    val zioHttp = "3.11.5"
    val zioJson = "1.1.0"
    val zioLogging = "2.5.3"

  }

  // ZIO core
  val zio = "dev.zio"        %% "zio"          % V.zio
  val zioJson = "dev.zio"    %% "zio-json"     % V.zioJson
  val zioTest = "dev.zio"    %% "zio-test"     % V.zio % Test
  val zioTestSbt = "dev.zio" %% "zio-test-sbt" % V.zio % Test

  // Config and logging (server)
  val zioConfig = Seq(
    "dev.zio" %% "zio-config"          % V.zioConfig,
    "dev.zio" %% "zio-config-magnolia" % V.zioConfig,
    "dev.zio" %% "zio-config-typesafe" % V.zioConfig
  )
  val logbackTest = "ch.qos.logback" % "logback-classic" % V.logback % Test
  val logging = Seq(
    "dev.zio"       %% "zio-logging-slf4j2" % V.zioLogging,
    "ch.qos.logback" % "logback-classic"    % V.logback
  )

  // Database: shared
  val hikari = "com.zaxxer"       % "HikariCP"    % V.hikari
  val flywayCore = "org.flywaydb" % "flyway-core" % V.flyway

  // Database: per database
  val mariadbDriver = "org.mariadb.jdbc"      % "mariadb-java-client"          % V.mariadb
  val flywayMysql = "org.flywaydb"            % "flyway-mysql"                 % V.flyway
  val testcontainersMariadb = "com.dimafeng" %% "testcontainers-scala-mariadb" % V.testcontainers % Test

  // Database: per DB layer
  val quill = "io.getquill" %% "quill-jdbc-zio" % V.quill

  // GraphQL
  val calibanCore = "com.github.ghostdogpr" %% "caliban" % V.caliban
  // caliban-zio-http stopped at 2.x; caliban-quick (QuickAdapter) is its replacement.
  val calibanQuick = "com.github.ghostdogpr"  %% "caliban-quick"  % V.caliban
  val calibanClient = "com.github.ghostdogpr" %% "caliban-client" % V.caliban

  // HTTP servers
  val zioHttp = "dev.zio" %% "zio-http" % V.zioHttp

  // Scala.js client. In a Scala.js project sbt 2's `%%` resolves the _sjs1_3 artifacts.
  val scalaJavaTime = Seq(
    "io.github.cquiroz" %% "scala-java-time"      % V.scalaJavaTime,
    "io.github.cquiroz" %% "scala-java-time-tzdb" % V.scalaJavaTime
  )
  val clientLibraries = Seq(
    "com.github.ghostdogpr"             %% "caliban-client"        % V.caliban,
    "com.softwaremill.sttp.client4"     %% "core"                  % V.sttpClient4,
    "org.scala-js"                      %% "scalajs-dom"           % V.scalajsDom,
    "com.olvind"                        %% "scalablytyped-runtime" % V.scalablytypedRuntime,
    "com.github.japgolly.scalajs-react" %% "core"                  % V.scalajsReact,
    "com.github.japgolly.scalajs-react" %% "extra"                 % V.scalajsReact
  )

}
