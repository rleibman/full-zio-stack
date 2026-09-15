import sbt.*

/** Every library version in one place. Shared by all variants: a generated project gets this file unchanged and its build.sbt only picks modules from
  * it.
  */
object Dependencies {

  object V {

    val caliban = "3.1.5"
    val doobie = "1.0.0-RC12"
    val flyway = "13.7.0"
    val hikari = "7.1.0"
    val logback = "1.6.3"
    val mariadb = "3.5.10"
    val mysql = "26.7.0"
    val postgres = "42.7.13"
    val quill = "4.8.6"
    val scalablytypedRuntime = "2.4.2"
    val scalaJavaTime = "2.7.0"
    val scalajsDom = "2.8.1"
    val slick = "3.6.1"
    val sqlite = "3.53.4.0"
    // 4.x is written for React 19 (client/package.json and stLib/package.json).
    val scalajsReact = "4.0.0"
    val sttpClient4 = "4.0.26"
    val testcontainers = "0.44.1"
    val zio = "2.1.26"
    val zioConfig = "4.1.0"
    val zioHttp = "3.11.5"
    val zioInteropCats = "23.1.0.13"
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

  // Database: per database. The keys are the template's `database` answers.
  val databaseLibraries: Map[String, Seq[ModuleID]] = Map(
    "mariadb" -> Seq(
      "org.mariadb.jdbc" % "mariadb-java-client"          % V.mariadb,
      "org.flywaydb"     % "flyway-mysql"                 % V.flyway,
      "com.dimafeng"    %% "testcontainers-scala-mariadb" % V.testcontainers % Test
    ),
    "mysql" -> Seq(
      "com.mysql"     % "mysql-connector-j"          % V.mysql,
      "org.flywaydb"  % "flyway-mysql"               % V.flyway,
      "com.dimafeng" %% "testcontainers-scala-mysql" % V.testcontainers % Test
    ),
    "postgres" -> Seq(
      "org.postgresql" % "postgresql"                      % V.postgres,
      "org.flywaydb"   % "flyway-database-postgresql"      % V.flyway,
      "com.dimafeng"  %% "testcontainers-scala-postgresql" % V.testcontainers % Test
    ),
    // flyway-core handles SQLite on its own, and the tests use a temporary file instead of a container.
    "sqlite" -> Seq("org.xerial" % "sqlite-jdbc" % V.sqlite)
  )

  // Database: per DB layer. The keys are the template's `db_layer` answers.
  val layerLibraries: Map[String, Seq[ModuleID]] = Map(
    "quill"  -> Seq("io.getquill" %% "quill-jdbc-zio" % V.quill),
    "doobie" -> Seq(
      "org.tpolecat" %% "doobie-core"      % V.doobie,
      "dev.zio"      %% "zio-interop-cats" % V.zioInteropCats
    ),
    "slick" -> Seq("com.typesafe.slick" %% "slick" % V.slick)
  )

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
