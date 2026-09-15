////////////////////////////////////////////////////////////////////////////////////
// Global / Common Stuff
//
// This build contains EVERY variant of the full-zio-stack template at once (see CLAUDE.md): each DB layer x database
// pair is its own project (see dbVariant), and each HTTP server is its own project. A generated project contains
// only one of each; its build.sbt comes from overlay/build.sbt.jinja, not from this file.

import Dependencies.*
import org.apache.commons.io.FileUtils
import org.scalajs.linker.interface.ModuleSplitStyle

ThisBuild / resolvers += Resolver.sonatypeCentralSnapshots

lazy val SCALA = "3.9.0"
Global / onChangedBuildSource := ReloadOnSourceChanges
scalaVersion                  := SCALA
Global / scalaVersion         := SCALA

import scala.concurrent.duration.*
Global / watchAntiEntropy := 1.second

// sbt-git sets ThisBuild / version but nothing in this build reads it; sbt 2's lintUnused has no other way to be
// told that is the plugin's business.
Global / excludeLintKeys += version

// sbt-native-packager wires up Debian/Rpm/Universal-docs/Universal-src scopes that this build never reads.
Global / excludeLintKeys ++= Set(
  daemonUser,
  daemonUserUid,
  daemonGroup,
  daemonGroupGid,
  executableScriptName,
  javaOptions,
  name
)

// The stLib facades were built against older scalajs-react modules than the 4.x this build uses. Declared so sbt 2's
// stricter evictionErrorLevel does not fail the build on them, while still catching anything NEW. `%%` would only
// cover the JVM artifacts, hence the explicit _sjs1_3 names.
// V.scalajsReact and client/package.json's "react" have to move together (scalajs-react 4.x = React 19).
ThisBuild / libraryDependencySchemes ++= Seq(
  "com.github.japgolly.scalajs-react" % "core_sjs1_3"         % VersionScheme.Always,
  "com.github.japgolly.scalajs-react" % "core-generic_sjs1_3" % VersionScheme.Always,
  "com.github.japgolly.scalajs-react" % "extra_sjs1_3"        % VersionScheme.Always,
  "com.github.japgolly.scalajs-react" % "facade_sjs1_3"       % VersionScheme.Always,
  "com.github.japgolly.scalajs-react" % "util_sjs1_3"         % VersionScheme.Always,
  "com.github.japgolly.scalajs-react" % "callback_sjs1_3"     % VersionScheme.Always
)

// caliban and sttp-client4 still ask for an older zio-json than this build uses.
ThisBuild / libraryDependencySchemes ++= Seq(
  "dev.zio" %% "zio-json"        % VersionScheme.Always,
  "dev.zio"  % "zio-json_sjs1_3" % VersionScheme.Always
)

//////////////////////////////////////////////////////////////////////////////////////////////////
// Shared settings
lazy val webDist = TaskKey[File]("webDist")
lazy val webDebugDist = TaskKey[File]("webDebugDist")
lazy val calibanRender = TaskKey[Unit]("calibanRender", "Writes the GraphQL schema to src/main/graphql/schema.graphql")
// Read by IntelliJ's sbt import (not by sbt itself): true keeps a project out of the IDE.
lazy val ideSkipProject = SettingKey[Boolean]("ideSkipProject").withRank(KeyRanks.Invisible)
Global / excludeLintKeys += ideSkipProject

lazy val scala3Opts = Seq(
  "-Wconf:msg=Implicit parameters should be provided with a `using` clause:s",
  "-deprecation", // Emit warning and location for usages of deprecated APIs.
  "-no-indent", // scala3
  "-old-syntax", // I hate space sensitive languages!
  "-encoding",
  "utf-8", // Specify character encoding used by source files.
  "-feature", // Emit warning and location for usages of features that should be imported explicitly.
  "-language:existentials", // Existential types (besides wildcard types) can be written and inferred
  "-language:implicitConversions",
  "-language:higherKinds", // Allow higher-kinded types
  //  "-language:strictEquality", //This is cool, but super noisy
  "-unchecked", // Enable additional warnings where generated code depends on assumptions.
//  "-Wsafe-init", //Great idea, breaks compile though.
  "-Werror", // Fail the compilation if there are any warnings.
  "-Xmax-inlines",
  "128",
  //  "-explain-types", // Explain type errors in more detail.
  //  "-explain",
  "-Yexplicit-nulls", // Make reference types non-nullable. Nullable types can be expressed with unions: e.g. String|Null.
  "-Yretain-trees" // Retain trees for debugging.,
)

enablePlugins(
  com.github.sbt.git.GitVersioning
)

lazy val commonSettings = Seq(
  scalaVersion       := SCALA,
  git.useGitDescribe := true,
  organization       := "net.leibman",
  startYear          := Some(2024),
  organizationName   := "Roberto Leibman",
  headerLicense      := Some(HeaderLicense.MIT("2024", "Roberto Leibman", HeaderLicenseStyle.Detailed)),
  resolvers += Resolver.mavenLocal,
  scalacOptions ++= scala3Opts
)

lazy val testSettings = Seq(
  libraryDependencies ++= Seq(zioTest, zioTestSbt),
  // Testcontainers and the servers under test need their own JVM; sequential keeps container start-up predictable.
  Test / fork              := true,
  Test / parallelExecution := false
)

////////////////////////////////////////////////////////////////////////////////////
// Model: shared by the server and the client
lazy val modelJVM = model.jvm
lazy val modelJS = model.js

lazy val model = crossProject(JSPlatform, JVMPlatform)
  .enablePlugins(AutomateHeaderPlugin, com.github.sbt.git.GitVersioning)
  .settings(
    name := "full-zio-stack-model",
    commonSettings,
    libraryDependencies += zioJson
  )
  .jsSettings(
    libraryDependencies ++= scalaJavaTime
  )

//////////////////////////////////////////////////////////////////////////////////////////////////
// DB core: the DataService traits, errors, the in-memory mock, DataSource + Flyway, and the migrations of every
// database. Its tests hold the contract suites that every DB layer runs.
lazy val dbCore = project
  .in(file("db-core"))
  .enablePlugins(AutomateHeaderPlugin, com.github.sbt.git.GitVersioning)
  .dependsOn(modelJVM)
  .settings(
    name := "full-zio-stack-db-core",
    commonSettings,
    testSettings,
    libraryDependencies ++= Seq(zio, hikari, flywayCore, logbackTest),
    // The per-database test sources (src/test-<database>) belong to the dbVariant projects, not to this one.
    Test / unmanagedSourceDirectories := Seq((Test / scalaSource).value)
  )

/** One DB layer (`db-<layer>/`) compiled for one database.
  *
  * Sources: `db-<layer>/src/main/scala` (shared by every database) plus `db-<layer>/src/main-<database>/scala` (the database-specific bits: Quill
  * context, Slick profile, doobie mappings). Tests: `db-<layer>/src/test/scala` plus the database's test fixture in
  * `db-core/src/test-<database>/scala`. Every variant exposes the same `…db.DataLayer.live`, so the servers don't care which one they get. The
  * generator merges the chosen pair into a generated project's `db/` module.
  */
def dbVariant(
  layer:     String,
  database:  String,
  libraries: Seq[ModuleID]
): Project = {
  Project(s"db-$layer-$database", file(s"db-$layer") / ".variants" / database)
    .enablePlugins(AutomateHeaderPlugin, com.github.sbt.git.GitVersioning)
    .dependsOn(dbCore % "compile->compile;test->test")
    .settings(
      name := s"full-zio-stack-db-$layer-$database",
      commonSettings,
      testSettings,
      libraryDependencies ++= libraries,
      Compile / unmanagedSourceDirectories := {
        val layerDir = (ThisBuild / baseDirectory).value / s"db-$layer"
        Seq(layerDir / "src/main/scala", layerDir / s"src/main-$database/scala")
      },
      Compile / unmanagedResourceDirectories := Seq((ThisBuild / baseDirectory).value / s"db-$layer/src/main/resources"),
      Test / unmanagedSourceDirectories      := {
        val root = (ThisBuild / baseDirectory).value
        Seq(root / s"db-$layer/src/test/scala", root / s"db-core/src/test-$database/scala")
      },
      Test / unmanagedResourceDirectories := Seq((ThisBuild / baseDirectory).value / s"db-$layer/src/test/resources")
    )
}

/** The variant the servers are built against, and the only one the IDE imports: every variant of a layer shares its source directories, which
  * IntelliJ can't attribute to more than one module.
  */
lazy val defaultDbVariant = ("quill", "mariadb")

def db(
  layer:    String,
  database: String
): Project =
  dbVariant(layer, database, layerLibraries(layer) ++ databaseLibraries(database))
    .settings(ideSkipProject := (layer, database) != defaultDbVariant)

lazy val dbQuillMariadb = db("quill", "mariadb")
lazy val dbQuillMysql = db("quill", "mysql")
lazy val dbQuillPostgres = db("quill", "postgres")
lazy val dbQuillSqlite = db("quill", "sqlite")
lazy val dbDoobieMariadb = db("doobie", "mariadb")
lazy val dbDoobieMysql = db("doobie", "mysql")
lazy val dbDoobiePostgres = db("doobie", "postgres")
lazy val dbDoobieSqlite = db("doobie", "sqlite")
lazy val dbSlickMariadb = db("slick", "mariadb")
lazy val dbSlickMysql = db("slick", "mysql")
lazy val dbSlickPostgres = db("slick", "postgres")
lazy val dbSlickSqlite = db("slick", "sqlite")

lazy val dbVariants: Seq[ProjectReference] = Seq(
  dbQuillMariadb,
  dbQuillMysql,
  dbQuillPostgres,
  dbQuillSqlite,
  dbDoobieMariadb,
  dbDoobieMysql,
  dbDoobiePostgres,
  dbDoobieSqlite,
  dbSlickMariadb,
  dbSlickMysql,
  dbSlickPostgres,
  dbSlickSqlite
)

//////////////////////////////////////////////////////////////////////////////////////////////////
// Server core: config, the GraphQL API and service layer, and the layer wiring. Independent of the HTTP server.
// Depends on the default DB variant so the app runs; the others are proven by their own contract tests.
lazy val serverCore = project
  .in(file("server-core"))
  .enablePlugins(AutomateHeaderPlugin, com.github.sbt.git.GitVersioning, BuildInfoPlugin)
  .dependsOn(modelJVM, dbCore % "compile->compile;test->test", dbQuillMariadb)
  .settings(
    name             := "full-zio-stack-server-core",
    buildInfoPackage := "net.leibman.fullziostack.server",
    commonSettings,
    testSettings,
    libraryDependencies ++= Seq(zio, calibanCore) ++ zioConfig ++ logging,
    calibanRender := Def.uncached {
      Def.taskDyn {
        val schemaFile = baseDirectory.value / "src" / "main" / "graphql" / "schema.graphql"
        (Compile / runMain).toTask(s" net.leibman.fullziostack.graphql.RenderSchema $schemaFile")
      }.value
    },
    // SchemaSpec compares the committed schema with the current API.
    Test / javaOptions += s"-Dschema.file=${baseDirectory.value / "src" / "main" / "graphql" / "schema.graphql"}"
  )

//////////////////////////////////////////////////////////////////////////////////////////////////
// HTTP servers. Each provides …server.Main.
lazy val serverZiohttp = project
  .in(file("server-ziohttp"))
  .enablePlugins(
    AutomateHeaderPlugin,
    com.github.sbt.git.GitVersioning,
    LinuxPlugin,
    JavaServerAppPackaging,
    SystemloaderPlugin,
    SystemdPlugin
  )
  .dependsOn(serverCore % "compile->compile;test->test")
  .settings(
    name := "full-zio-stack-server",
    commonSettings,
    testSettings,
    libraryDependencies ++= Seq(zioHttp, calibanQuick),
    // Run from the repository root, so the relative staticContentDir (dist/ or debugDist/) resolves.
    run / fork          := true,
    run / baseDirectory := (ThisBuild / baseDirectory).value
  )

////////////////////////////////////////////////////////////////////////////////////
// Web
/** Links with Scala.js, bundles with `vite build`, then lays the result out beside the static assets.
  *
  * sbt drives vite, not the reverse: @scala-js/vite-plugin-scalajs resolves the linker output by spawning `sbt print fastLinkJSOutput`, which from
  * inside an sbt task means sbt re-entering itself for a path the caller already holds. The paths go over in the environment instead -- see
  * client/vite.config.js.
  */
def viteDistImpl(
  viteRoot:      File,
  scalaJSOutput: File,
  assets:        File,
  stagingDir:    File,
  outputFolder:  File,
  mode:          String,
  log:           Logger
): File = {
  import scala.sys.process.*

  if (!(viteRoot / "node_modules").exists()) {
    log.info(s"node_modules missing, running `npm install` in $viteRoot")
    val installed = Process("npm" :: "install" :: Nil, viteRoot).!
    if (installed != 0) sys.error(s"npm install failed in $viteRoot (exit code $installed)")
  }

  val env = Seq(
    "SCALAJS_OUTPUT_DIR" -> scalaJSOutput.getAbsolutePath,
    "VITE_OUT_DIR"       -> stagingDir.getAbsolutePath
  )
  log.info(s"vite build --mode $mode (scala.js output: $scalaJSOutput)")
  val built = Process("npx" :: "vite" :: "build" :: "--mode" :: mode :: Nil, viteRoot, env *).!
  if (built != 0) sys.error(s"vite build failed in $viteRoot (exit code $built)")

  // Clear ONLY the bundler's output area; the static copy below re-adds anything of ours under assets/.
  val bundleAssets = outputFolder / "assets"
  if (bundleAssets.exists()) FileUtils.deleteDirectory(bundleAssets)
  outputFolder.mkdirs()
  // index.html comes from vite's output (it carries the hashed <script>), so skip any source copy of it.
  if (assets.exists()) {
    assets.listFiles().foreach { f =>
      if (f.getName != "index.html") {
        if (f.isDirectory) FileUtils.copyDirectory(f, outputFolder / f.getName, true)
        else FileUtils.copyFile(f, outputFolder / f.getName)
      }
    }
  }
  FileUtils.copyDirectory(stagingDir, outputFolder, true)
  outputFolder
}

// Published locally from the standalone stLib/ build: cd stLib && npm install && sbt publishLocal
val stlibVersion = "3.0.0"

lazy val client = project
  .dependsOn(modelJS)
  .settings(commonSettings)
  .enablePlugins(
    AutomateHeaderPlugin,
    com.github.sbt.git.GitVersioning,
    ScalaJSPlugin,
    CalibanPlugin
  )
  .settings(
    name := "full-zio-stack-web",
    // The GraphQL client is generated on compile from the server's committed schema (see calibanRender).
    Compile / caliban / calibanSources := (serverCore / baseDirectory).value / "src" / "main" / "graphql",
    Compile / caliban / calibanSettings += calibanSetting((serverCore / baseDirectory).value / "src" / "main" / "graphql" / "schema.graphql")(
      _.clientName("FullZIOStackClient").packageName("net.leibman.fullziostack.client.api")
    ),
    libraryDependencies ++= clientLibraries ++ scalaJavaTime ++ Seq(
      // Hand-suffixed: this jar comes from ~/.ivy2/local, where coursier cross-versions the module *directory*
      // to full-zio-stack-stlib_sjs1_3 but derives the *jar* name as full-zio-stack-stlib_3.jar, which does not
      // exist.
      "net.leibman" % "full-zio-stack-stlib_sjs1_3" % stlibVersion
    ),
    dependencyOverrides ++= Seq(
      "com.github.japgolly.scalajs-react" %% "core"  % V.scalajsReact,
      "com.github.japgolly.scalajs-react" %% "extra" % V.scalajsReact
    ),
    Compile / unmanagedSourceDirectories := Seq((Compile / scalaSource).value),
    Test / unmanagedSourceDirectories    := Seq((Test / scalaSource).value),
    // scalajs-react's StBuildingComponent is `inline`, so its body -- including a call to the deprecated
    // scala.scalajs.runtime.linkingInfo -- is reported at every one of our call sites. Nothing here can fix it.
    // Narrowly matched so any other deprecation still warns.
    scalacOptions += "-Wconf:msg=linkingInfo in package scala.scalajs.runtime is deprecated:s",
    // ES modules, the only module kind vite consumes directly -- this replaces what the bundler plugin set up.
    // SmallModulesFor keeps application code in many small chunks so an incremental fastLinkJS rewrites little.
    scalaJSLinkerConfig ~= {
      _.withModuleKind(ModuleKind.ESModule)
        .withModuleSplitStyle(ModuleSplitStyle.SmallModulesFor(List("net.leibman.fullziostack.client")))
        .withSourceMap(true)
    },
    Global / scalaJSStage                     := FastOptStage,
    Compile / scalaJSUseMainModuleInitializer := true,
    Test / scalaJSUseMainModuleInitializer    := false,
    // webDebugDist: readable stack traces (unminified, mapped back to .scala, React's development build)
    webDebugDist := Def.uncached {
      viteDistImpl(
        viteRoot = baseDirectory.value,
        scalaJSOutput = (Compile / fastLinkJSOutput).value,
        assets = baseDirectory.value / "src" / "main" / "web",
        stagingDir = target.value / "vite" / "debugDist",
        outputFolder = (ThisBuild / baseDirectory).value / "debugDist",
        mode = "development",
        log = streams.value.log
      )
    },
    // webDist: minified, but keeps the source map so production stack traces stay decipherable
    webDist := Def.uncached {
      viteDistImpl(
        viteRoot = baseDirectory.value,
        scalaJSOutput = (Compile / fullLinkJSOutput).value,
        assets = baseDirectory.value / "src" / "main" / "web",
        stagingDir = target.value / "vite" / "dist",
        outputFolder = (ThisBuild / baseDirectory).value / "dist",
        mode = "production",
        log = streams.value.log
      )
    }
  )

//////////////////////////////////////////////////////////////////////////////////////////////////
// Root project
lazy val root = project
  .in(file("."))
  .aggregate((Seq[ProjectReference](modelJVM, modelJS, dbCore, serverCore, serverZiohttp, client) ++ dbVariants) *)
  .settings(
    name           := "full-zio-stack",
    publish / skip := true,
    version        := "0.1.0",
    headerLicense  := None
  )
