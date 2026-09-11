////////////////////////////////////////////////////////////////////////////////////
// Global / Common Stuff

import org.apache.commons.io.FileUtils
import org.scalajs.linker.interface.ModuleSplitStyle

lazy val buildTime: SettingKey[String] = SettingKey[String]("buildTime", "time of build").withRank(KeyRanks.Invisible)

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
  name,
)

// The stLib facades and scalacss 1.0.0 were built against older scalajs-react modules than the 4.x this build
// uses. Declared so sbt 2's stricter evictionErrorLevel does not fail the build on them, while still catching
// anything NEW. `%%` would only cover the JVM artifacts, hence the explicit _sjs1_3 names.
// scalajsReactVersion and client/package.json's "react" have to move together (scalajs-react 4.x = React 19).
ThisBuild / libraryDependencySchemes ++= Seq(
  "com.github.japgolly.scalajs-react" % "core_sjs1_3"         % VersionScheme.Always,
  "com.github.japgolly.scalajs-react" % "core-generic_sjs1_3" % VersionScheme.Always,
  "com.github.japgolly.scalajs-react" % "extra_sjs1_3"        % VersionScheme.Always,
  "com.github.japgolly.scalajs-react" % "facade_sjs1_3"       % VersionScheme.Always,
  "com.github.japgolly.scalajs-react" % "util_sjs1_3"         % VersionScheme.Always,
  "com.github.japgolly.scalajs-react" % "callback_sjs1_3"     % VersionScheme.Always,
)

// caliban and sttp-client4 still ask for an older zio-json than this build uses.
ThisBuild / libraryDependencySchemes ++= Seq(
  "dev.zio" %% "zio-json"        % VersionScheme.Always,
  "dev.zio"  % "zio-json_sjs1_3" % VersionScheme.Always,
)

//////////////////////////////////////////////////////////////////////////////////////////////////
// Shared settings
lazy val start = TaskKey[Unit]("start")
lazy val webDist = TaskKey[File]("webDist")
lazy val webDebugDist = TaskKey[File]("webDebugDist")

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
  "-Yretain-trees", // Retain trees for debugging.,
)

enablePlugins(
  com.github.sbt.git.GitVersioning
)

val betterFilesVersion = "3.9.2"
val calibanVersion = "3.1.5"
val commonsCodecVersion = "1.22.1"
val courierVersion = "4.0.0-RC1"
val izumiReflectVersion = "3.0.10"
val jsoniterVersion = "2.40.1"
val justSemverCoreVersion = "1.3.0"
val jwtCirceVersion = "11.0.4"
val logbackVersion = "1.6.3"
val mariadbVersion = "3.5.10"
val quillVersion = "4.8.6"
val scalablytypedRuntimeVersion = "2.4.2"
val scalacssVersion = "1.0.0"
val scalaJavaTimeVersion = "2.7.0"
val scalajsDomVersion = "2.8.1"
// 4.x is written for React 19 (client/package.json and stLib/package.json).
val scalajsReactVersion = "4.0.0"
val scalatagsVersion = "0.13.1"
val scalaXmlVersion = "2.5.0"
// Published locally from the standalone stLib/ build: cd stLib && npm install && sbt publishLocal
val stlibVersion = "2.0.0"
val sttpClient4Version = "4.0.26"
val testContainerVersion = "0.44.1"
val zioCacheVersion = "0.3.0"
val zioConfigVersion = "4.1.0"
val zioHttpVersion = "3.11.5"
val zioJsonVersion = "1.1.0"
val zioLoggingSlf4j2Version = "2.5.3"
val zioNioVersion = "2.0.2"
val zioPreludeVersion = "1.0.0-RC48"
val zioSchemaVersion = "1.9.0"
val zioVersion = "2.1.26"

lazy val commonSettings = Seq(
  scalaVersion       := SCALA,
  git.useGitDescribe := true,
  organization       := "net.leibman",
  startYear        := Some(2024),
  organizationName := "Roberto Leibman",
  headerLicense    := Some(HeaderLicense.MIT("2024", "Roberto Leibman", HeaderLicenseStyle.Detailed)),
  resolvers += Resolver.mavenLocal,
  scalacOptions ++= scala3Opts,
)

////////////////////////////////////////////////////////////////////////////////////
// Model
lazy val modelJVM = model.jvm
lazy val modelJS = model.js

lazy val model = crossProject(JSPlatform, JVMPlatform)
  .enablePlugins(
    AutomateHeaderPlugin,
    com.github.sbt.git.GitVersioning,
    BuildInfoPlugin,
  )
  .settings(
    name             := "full-zio-stack-model",
    buildInfoPackage := "fullZioStack",
    commonSettings,
    libraryDependencies ++= Seq(
      ("dev.zio" %% "zio-json" % zioJsonVersion).withSources()
    ),
  )
  .jvmSettings(
    libraryDependencies ++= Seq(
      ("dev.zio"     %% "zio"                   % zioVersion).withSources(),
      ("dev.zio"     %% "zio-nio"               % zioNioVersion).withSources(),
      ("dev.zio"     %% "zio-config"            % zioConfigVersion).withSources(),
      ("dev.zio"     %% "zio-config-derivation" % zioConfigVersion).withSources(),
      ("dev.zio"     %% "zio-config-magnolia"   % zioConfigVersion).withSources(),
      ("dev.zio"     %% "zio-config-typesafe"   % zioConfigVersion).withSources(),
      ("dev.zio"     %% "zio-prelude"           % zioPreludeVersion).withSources(),
      ("dev.zio"     %% "zio-http"              % zioHttpVersion).withSources(),
      ("io.getquill" %% "quill-jdbc-zio"        % quillVersion).withSources(),
      ("io.kevinlee" %% "just-semver-core"      % justSemverCoreVersion).withSources(),
    )
  )
  .jsSettings(
    // sbt 2 has no `%%%`: in a Scala.js project `%%` already resolves the _sjs1_3 artifact.
    libraryDependencies ++= Seq(
      ("dev.zio"                              %% "zio"                   % zioVersion).withSources(),
      ("dev.zio"                              %% "zio-prelude"           % zioPreludeVersion).withSources(),
      ("io.kevinlee"                          %% "just-semver-core"      % justSemverCoreVersion).withSources(),
      "com.github.plokhotnyuk.jsoniter-scala" %% "jsoniter-scala-core"   % jsoniterVersion,
      "com.github.plokhotnyuk.jsoniter-scala" %% "jsoniter-scala-macros" % jsoniterVersion,
    )
  )

//////////////////////////////////////////////////////////////////////////////////////////////////
// DB
lazy val dbJVM = db.jvm
lazy val dbJS = db.js
lazy val db = crossProject(JSPlatform, JVMPlatform)
  .enablePlugins(AutomateHeaderPlugin, com.github.sbt.git.GitVersioning)
  .settings(
    name := "full-zio-stack-db",
    commonSettings,
  )
  .dependsOn(model)
  .jvmSettings(
    libraryDependencies ++= Seq(
      // DB
      ("org.mariadb.jdbc" % "mariadb-java-client" % mariadbVersion).withSources(),
      ("io.getquill"     %% "quill-jdbc-zio"      % quillVersion).withSources(),
      // Log
      ("ch.qos.logback" % "logback-classic" % logbackVersion).withSources(),
      // ZIO
      ("dev.zio"                %% "zio"                   % zioVersion).withSources(),
      ("dev.zio"                %% "zio-nio"               % zioNioVersion).withSources(),
      ("dev.zio"                %% "zio-cache"             % zioCacheVersion).withSources(),
      ("dev.zio"                %% "zio-config"            % zioConfigVersion).withSources(),
      ("dev.zio"                %% "zio-config-derivation" % zioConfigVersion).withSources(),
      ("dev.zio"                %% "zio-config-magnolia"   % zioConfigVersion).withSources(),
      ("dev.zio"                %% "zio-config-typesafe"   % zioConfigVersion).withSources(),
      ("dev.zio"                %% "zio-logging-slf4j2"    % zioLoggingSlf4j2Version).withSources(),
      ("dev.zio"                %% "izumi-reflect"         % izumiReflectVersion).withSources(),
      ("dev.zio"                %% "zio-json"              % zioJsonVersion).withSources(),
      ("org.scala-lang.modules" %% "scala-xml"             % scalaXmlVersion).withSources(),
      // Other random utilities
      ("com.github.pathikrit" %% "better-files"                 % betterFilesVersion).withSources(),
      "commons-codec"          % "commons-codec"                % commonsCodecVersion,
      ("com.dimafeng"         %% "testcontainers-scala-mariadb" % testContainerVersion).withSources(),
      // Testing
      ("dev.zio" %% "zio-test"     % zioVersion % "test").withSources(),
      ("dev.zio" %% "zio-test-sbt" % zioVersion % "test").withSources(),
    )
  )
  .jsSettings(
    libraryDependencies ++= Seq(
      ("com.softwaremill.sttp.client4"     %% "core"                  % sttpClient4Version).withSources(),
      ("com.softwaremill.sttp.client4"     %% "zio-json"              % sttpClient4Version).withSources(),
      "com.olvind"                         %% "scalablytyped-runtime" % scalablytypedRuntimeVersion,
      ("com.github.japgolly.scalajs-react" %% "core"                  % scalajsReactVersion).withSources(),
      ("com.github.japgolly.scalajs-react" %% "extra"                 % scalajsReactVersion).withSources(),
      ("com.github.ghostdogpr"             %% "caliban-client"        % calibanVersion).withSources(),
      ("dev.zio"                           %% "zio-json"              % zioJsonVersion).withSources(),
    )
  )

//////////////////////////////////////////////////////////////////////////////////////////////////
// Server
lazy val api = project
  .enablePlugins(
    AutomateHeaderPlugin,
    com.github.sbt.git.GitVersioning,
    LinuxPlugin,
    JavaServerAppPackaging,
    SystemloaderPlugin,
    SystemdPlugin,
    CalibanPlugin,
  )
  .settings(commonSettings)
  .dependsOn(modelJVM, dbJVM)
  .settings(
    name := "full-zio-stack-server",
    libraryDependencies ++= Seq(
      // DB
      ("org.mariadb.jdbc" % "mariadb-java-client" % mariadbVersion).withSources(),
      ("io.getquill"     %% "quill-jdbc-zio"      % quillVersion).withSources(),
      // Log
      ("ch.qos.logback" % "logback-classic" % logbackVersion).withSources(),
      // ZIO
      ("dev.zio"                %% "zio"                   % zioVersion).withSources(),
      ("dev.zio"                %% "zio-nio"               % zioNioVersion).withSources(),
      ("dev.zio"                %% "zio-cache"             % zioCacheVersion).withSources(),
      ("dev.zio"                %% "zio-config"            % zioConfigVersion).withSources(),
      ("dev.zio"                %% "zio-config-derivation" % zioConfigVersion).withSources(),
      ("dev.zio"                %% "zio-config-magnolia"   % zioConfigVersion).withSources(),
      ("dev.zio"                %% "zio-config-typesafe"   % zioConfigVersion).withSources(),
      ("dev.zio"                %% "zio-logging-slf4j2"    % zioLoggingSlf4j2Version).withSources(),
      ("dev.zio"                %% "zio-schema"            % zioSchemaVersion).withSources(),
      ("dev.zio"                %% "izumi-reflect"         % izumiReflectVersion).withSources(),
      ("com.github.ghostdogpr"  %% "caliban"               % calibanVersion).withSources(),
      // caliban-zio-http stopped at 2.x; caliban-quick (QuickAdapter) is its replacement.
      ("com.github.ghostdogpr"  %% "caliban-quick"         % calibanVersion).withSources(),
      ("dev.zio"                %% "zio-http"              % zioHttpVersion).withSources(),
      ("dev.zio"                %% "zio-http-cli"          % zioHttpVersion).withSources(),
      ("com.github.jwt-scala"   %% "jwt-circe"             % jwtCirceVersion).withSources(),
      ("dev.zio"                %% "zio-json"              % zioJsonVersion).withSources(),
      ("org.scala-lang.modules" %% "scala-xml"             % scalaXmlVersion).withSources(),
      // Other random utilities
      ("com.github.pathikrit"  %% "better-files"                 % betterFilesVersion).withSources(),
      ("com.github.daddykotex" %% "courier"                      % courierVersion).withSources(),
      "commons-codec"           % "commons-codec"                % commonsCodecVersion,
      ("com.dimafeng"          %% "testcontainers-scala-mariadb" % testContainerVersion).withSources(),
      // Testing
      ("dev.zio" %% "zio-test"     % zioVersion % "test").withSources(),
      ("dev.zio" %% "zio-test-sbt" % zioVersion % "test").withSources(),
    ),
  )

//////////////////////////////////////////////////////////////////////////////////////////////////
// Utility
lazy val util = project
  .settings(commonSettings)

lazy val codeGen = project
  .settings(
    commonSettings,
    libraryDependencies ++= Seq(
      ("dev.zio" %% "zio-http"     % zioHttpVersion).withSources(),
      ("dev.zio" %% "zio-http-gen" % zioHttpVersion).withSources(),
    ),
  )

////////////////////////////////////////////////////////////////////////////////////
// Web
/** Links with Scala.js, bundles with `vite build`, then lays the result out beside the static assets.
  *
  * sbt drives vite, not the reverse: @scala-js/vite-plugin-scalajs resolves the linker output by spawning
  * `sbt print fastLinkJSOutput`, which from inside an sbt task means sbt re-entering itself for a path the caller
  * already holds. The paths go over in the environment instead -- see client/vite.config.js.
  */
def viteDistImpl(
  viteRoot:      File,
  scalaJSOutput: File,
  assets:        File,
  stagingDir:    File,
  outputFolder:  File,
  mode:          String,
  log:           Logger,
): File = {
  import scala.sys.process.*

  if (!(viteRoot / "node_modules").exists()) {
    log.info(s"node_modules missing, running `npm install` in $viteRoot")
    val installed = Process("npm" :: "install" :: Nil, viteRoot).!
    if (installed != 0) sys.error(s"npm install failed in $viteRoot (exit code $installed)")
  }

  val env = Seq(
    "SCALAJS_OUTPUT_DIR" -> scalaJSOutput.getAbsolutePath,
    "VITE_OUT_DIR"       -> stagingDir.getAbsolutePath,
  )
  log.info(s"vite build --mode $mode (scala.js output: $scalaJSOutput)")
  val built = Process("npx" :: "vite" :: "build" :: "--mode" :: mode :: Nil, viteRoot, env*).!
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

lazy val commonWeb: Project => Project =
  _.settings(
    libraryDependencies ++= Seq(
      // Hand-suffixed: this jar comes from ~/.ivy2/local, where coursier cross-versions the module *directory*
      // to full-zio-stack-stlib_sjs1_3 but derives the *jar* name as full-zio-stack-stlib_3.jar, which does not
      // exist.
      ("net.leibman"                        % "full-zio-stack-stlib_sjs1_3" % stlibVersion).withSources(),
      ("com.github.ghostdogpr"             %% "caliban-client"              % calibanVersion).withSources(),
      ("dev.zio"                           %% "zio"                         % zioVersion).withSources(),
      ("com.softwaremill.sttp.client4"     %% "core"                        % sttpClient4Version).withSources(),
      ("com.softwaremill.sttp.client4"     %% "zio-json"                    % sttpClient4Version).withSources(),
      ("io.github.cquiroz"                 %% "scala-java-time"             % scalaJavaTimeVersion).withSources(),
      ("io.github.cquiroz"                 %% "scala-java-time-tzdb"        % scalaJavaTimeVersion).withSources(),
      ("org.scala-js"                      %% "scalajs-dom"                 % scalajsDomVersion).withSources(),
      "com.olvind"                         %% "scalablytyped-runtime"       % scalablytypedRuntimeVersion,
      ("com.github.japgolly.scalajs-react" %% "core"                        % scalajsReactVersion).withSources(),
      ("com.github.japgolly.scalajs-react" %% "extra"                       % scalajsReactVersion).withSources(),
      ("com.lihaoyi"                       %% "scalatags"                   % scalatagsVersion).withSources(),
      ("com.github.japgolly.scalacss"      %% "core"                        % scalacssVersion).withSources(),
      ("com.github.japgolly.scalacss"      %% "ext-react"                   % scalacssVersion).withSources(),
    ),
    dependencyOverrides ++= Seq(
      "com.github.japgolly.scalajs-react" %% "core"  % scalajsReactVersion,
      "com.github.japgolly.scalajs-react" %% "extra" % scalajsReactVersion,
    ),
    organizationName                     := "Roberto Leibman",
    startYear                            := Some(2024),
    headerLicense                        := Some(HeaderLicense.MIT("2024", "Roberto Leibman", HeaderLicenseStyle.Detailed)),
    Compile / unmanagedSourceDirectories := Seq((Compile / scalaSource).value),
    Test / unmanagedSourceDirectories    := Seq((Test / scalaSource).value),
  )

lazy val client = project
  .dependsOn(modelJS, dbJS)
  .settings(commonSettings)
  .configure(commonWeb)
  .enablePlugins(
    AutomateHeaderPlugin,
    com.github.sbt.git.GitVersioning,
    ScalaJSPlugin,
  )
  .settings(
    name := "full-zio-stack-web",
    // scalajs-react's StBuildingComponent is `inline`, so its body -- including a call to the deprecated
    // scala.scalajs.runtime.linkingInfo -- is reported at every one of our call sites. Nothing here can fix it.
    // Narrowly matched so any other deprecation still warns.
    scalacOptions += "-Wconf:msg=linkingInfo in package scala.scalajs.runtime is deprecated:s",
    // ES modules, the only module kind vite consumes directly -- this replaces what the bundler plugin set up.
    // SmallModulesFor keeps application code in many small chunks so an incremental fastLinkJS rewrites little.
    scalaJSLinkerConfig ~= {
      _.withModuleKind(ModuleKind.ESModule)
        .withModuleSplitStyle(ModuleSplitStyle.SmallModulesFor(List("scalajsdemo")))
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
        log = streams.value.log,
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
        log = streams.value.log,
      )
    },
  )

//////////////////////////////////////////////////////////////////////////////////////////////////
// Root project
lazy val root = project
  .in(file("."))
  .aggregate(modelJVM, modelJS, api, util, dbJVM, dbJS, client, codeGen)
  .settings(
    name           := "full-zio-stack",
    publish / skip := true,
    version        := "0.1.0",
    headerLicense  := None,
  )
