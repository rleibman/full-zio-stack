////////////////////////////////////////////////////////////////////////////////////
// Global / Common Stuff

import com.typesafe.sbt.SbtGit.GitKeys.gitDescribedVersion
import org.apache.commons.io.FileUtils

import java.nio.file.Files
import java.nio.file.StandardCopyOption.REPLACE_EXISTING

lazy val buildTime: SettingKey[String] = SettingKey[String]("buildTime", "time of build").withRank(KeyRanks.Invisible)

ThisBuild / resolvers ++= Resolver.sonatypeOssRepos("snapshots")

lazy val SCALA = "3.6.4"
Global / onChangedBuildSource := ReloadOnSourceChanges
scalaVersion                  := SCALA
Global / scalaVersion         := SCALA

import scala.concurrent.duration.*
Global / watchAntiEntropy := 1.second

//////////////////////////////////////////////////////////////////////////////////////////////////
// Shared settings
lazy val start = TaskKey[Unit]("start")
lazy val dist = TaskKey[File]("dist")
lazy val debugDist = TaskKey[File]("debugDist")

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
  "-Xfatal-warnings", // Fail the compilation if there are any warnings.
  "-Xmax-inlines",
  "128",
  //  "-explain-types", // Explain type errors in more detail.
  //  "-explain",
  "-Yexplicit-nulls", // Make reference types non-nullable. Nullable types can be expressed with unions: e.g. String|Null.
  "-Yretain-trees" // Retain trees for debugging.,
)

enablePlugins(
  GitVersioning
)

val calibanVersion = "2.10.0"
val quillVersion = "4.8.6"
val scalajsReactVersion = "2.1.2"
val testContainerVersion = "0.43.0"
val zioConfigVersion = "4.0.4"
val zioHttpVersion = "3.2.0"
val zioJsonVersion = "0.7.42"
val zioVersion = "2.1.17"

//npm versions
val reactVersion = "^18.3.0"

lazy val commonSettings = Seq(
  scalaVersion     := SCALA,
  organization     := "net.leibman",
  startYear        := Some(2024),
  organizationName := "Roberto Leibman",
  headerLicense    := Some(HeaderLicense.MIT("2024", "Roberto Leibman", HeaderLicenseStyle.Detailed)),
  resolvers += Resolver.mavenLocal,
  scalacOptions ++= scala3Opts
)

////////////////////////////////////////////////////////////////////////////////////
// Model
lazy val modelJVM = model.jvm
lazy val modelJS = model.js

lazy val model = crossProject(JSPlatform, JVMPlatform)
  .enablePlugins(
    AutomateHeaderPlugin,
    GitVersioning,
    BuildInfoPlugin
  )
  .settings(
    name             := "full-zio-stack-model",
    buildInfoPackage := "fullZioStack",
    commonSettings,
    libraryDependencies ++= Seq(
      "dev.zio" %% "zio-json" % zioJsonVersion withSources ()
    )
  )
  .jvmSettings(
    version := gitDescribedVersion.value.getOrElse("0.0.1-SNAPSHOT"),
    libraryDependencies ++= Seq(
      "dev.zio"     %% "zio"                   % zioVersion withSources (),
      "dev.zio"     %% "zio-nio"               % "2.0.2" withSources (),
      "dev.zio"     %% "zio-config"            % zioConfigVersion withSources (),
      "dev.zio"     %% "zio-config-derivation" % zioConfigVersion withSources (),
      "dev.zio"     %% "zio-config-magnolia"   % zioConfigVersion withSources (),
      "dev.zio"     %% "zio-config-typesafe"   % zioConfigVersion withSources (),
      "dev.zio"     %% "zio-json"              % zioJsonVersion withSources (),
      "dev.zio"     %% "zio-prelude"           % "1.0.0-RC40" withSources (),
      "dev.zio"     %% "zio-http"              % zioHttpVersion withSources (),
      "io.getquill" %% "quill-jdbc-zio"        % quillVersion withSources (),
      "io.kevinlee" %% "just-semver-core"      % "1.1.1" withSources ()
    )
  )
  .jsSettings(
    version := gitDescribedVersion.value.getOrElse("0.0.1-SNAPSHOT"),
    libraryDependencies ++= Seq(
      "dev.zio" %%% "zio"                                                 % zioVersion withSources (),
      "dev.zio" %%% "zio-json"                                            % zioJsonVersion withSources (),
      "dev.zio" %%% "zio-prelude"                                         % "1.0.0-RC40" withSources (),
      "io.kevinlee" %%% "just-semver-core"                                % "1.1.1" withSources (),
      "com.github.plokhotnyuk.jsoniter-scala" %%% "jsoniter-scala-core"   % "2.35.2",
      "com.github.plokhotnyuk.jsoniter-scala" %%% "jsoniter-scala-macros" % "2.35.2"
    )
  )

//////////////////////////////////////////////////////////////////////////////////////////////////
// Server
lazy val dbJVM = db.jvm
lazy val dbJS = db.js
lazy val db = crossProject(JSPlatform, JVMPlatform)
  .enablePlugins(AutomateHeaderPlugin, GitVersioning)
  .settings(
    name         := "full-zio-stack-db",
    scalaVersion := SCALA,
    commonSettings
  )
  .dependsOn(model)
  .jvmSettings(
    version := gitDescribedVersion.value.getOrElse("0.0.1-SNAPSHOT"),
    libraryDependencies ++= Seq(
      // DB
      "org.mariadb.jdbc" % "mariadb-java-client" % "3.5.3" withSources (),
      "io.getquill"     %% "quill-jdbc-zio"      % quillVersion withSources (),
      // Log
      "ch.qos.logback" % "logback-classic" % "1.5.18" withSources (),
      // ZIO
      "dev.zio"                %% "zio"                   % zioVersion withSources (),
      "dev.zio"                %% "zio-nio"               % "2.0.2" withSources (),
      "dev.zio"                %% "zio-cache"             % "0.2.4" withSources (),
      "dev.zio"                %% "zio-config"            % zioConfigVersion withSources (),
      "dev.zio"                %% "zio-config-derivation" % zioConfigVersion withSources (),
      "dev.zio"                %% "zio-config-magnolia"   % zioConfigVersion withSources (),
      "dev.zio"                %% "zio-config-typesafe"   % zioConfigVersion withSources (),
      "dev.zio"                %% "zio-logging-slf4j2"    % "2.5.0" withSources (),
      "dev.zio"                %% "izumi-reflect"         % "3.0.2" withSources (),
      "dev.zio"                %% "zio-json"              % zioJsonVersion withSources (),
      "org.scala-lang.modules" %% "scala-xml"             % "2.3.0" withSources (),
      // Other random utilities
      "com.github.pathikrit" %% "better-files"                 % "3.9.2" withSources (),
      "commons-codec"         % "commons-codec"                % "1.18.0",
      "com.dimafeng"         %% "testcontainers-scala-mariadb" % testContainerVersion withSources (),
      // Testing
      "dev.zio" %% "zio-test"     % zioVersion % "test" withSources (),
      "dev.zio" %% "zio-test-sbt" % zioVersion % "test" withSources ()
    )
  )
  .jsSettings(
    libraryDependencies ++= Seq(
      "com.softwaremill.sttp.client4" %%% "core"      % "4.0.3" withSources (),
      "com.softwaremill.sttp.client4" %%% "zio-json"  % "4.0.3" withSources (),
      "com.olvind" %%% "scalablytyped-runtime"        % "2.4.2",
      "com.github.japgolly.scalajs-react" %%% "core"  % scalajsReactVersion withSources (),
      "com.github.japgolly.scalajs-react" %%% "extra" % scalajsReactVersion withSources (),
      "com.github.ghostdogpr" %%% "caliban-client"    % calibanVersion withSources (),
      "dev.zio" %%% "zio-json"                        % zioJsonVersion withSources ()
    )
  )

lazy val api = project
  .enablePlugins(
    AutomateHeaderPlugin,
    GitVersioning,
    LinuxPlugin,
    JavaServerAppPackaging,
    SystemloaderPlugin,
    SystemdPlugin,
    CalibanPlugin
  )
  .settings(commonSettings)
  .dependsOn(modelJVM, dbJVM)
  .settings(
    name := "full-zio-stack-server",
    libraryDependencies ++= Seq(
      // DB
      "org.mariadb.jdbc" % "mariadb-java-client" % "3.5.3" withSources (),
      "io.getquill"     %% "quill-jdbc-zio"      % quillVersion withSources (),
      // Log
      "ch.qos.logback" % "logback-classic" % "1.5.18" withSources (),
      // ZIO
      "dev.zio"                %% "zio"                   % zioVersion withSources (),
      "dev.zio"                %% "zio-nio"               % "2.0.2" withSources (),
      "dev.zio"                %% "zio-cache"             % "0.2.4" withSources (),
      "dev.zio"                %% "zio-config"            % zioConfigVersion withSources (),
      "dev.zio"                %% "zio-config-derivation" % zioConfigVersion withSources (),
      "dev.zio"                %% "zio-config-magnolia"   % zioConfigVersion withSources (),
      "dev.zio"                %% "zio-config-typesafe"   % zioConfigVersion withSources (),
      "dev.zio"                %% "zio-logging-slf4j2"    % "2.5.0" withSources (),
      "dev.zio"                %% "zio-schema"            % "1.7.0" withSources (),
      "dev.zio"                %% "izumi-reflect"         % "3.0.2" withSources (),
      "com.github.ghostdogpr"  %% "caliban"               % calibanVersion withSources (),
      "com.github.ghostdogpr"  %% "caliban-zio-http"      % calibanVersion withSources (),
      "com.github.ghostdogpr"  %% "caliban-quick"         % calibanVersion withSources (),
      "dev.zio"                %% "zio-http"              % zioHttpVersion withSources (),
      "dev.zio"                %% "zio-http-cli"          % zioHttpVersion withSources (),
      "com.github.jwt-scala"   %% "jwt-circe"             % "10.0.4" withSources (),
      "dev.zio"                %% "zio-json"              % zioJsonVersion withSources (),
      "org.scala-lang.modules" %% "scala-xml"             % "2.3.0" withSources (),
      // Other random utilities
      "com.github.pathikrit"  %% "better-files"                 % "3.9.2" withSources (),
      "com.github.daddykotex" %% "courier"                      % "4.0.0-RC1" withSources (),
      "commons-codec"          % "commons-codec"                % "1.18.0",
      "com.dimafeng"          %% "testcontainers-scala-mariadb" % testContainerVersion withSources (),
      // Testing
      "dev.zio" %% "zio-test"     % zioVersion % "test" withSources (),
      "dev.zio" %% "zio-test-sbt" % zioVersion % "test" withSources ()
    )
  )

//////////////////////////////////////////////////////////////////////////////////////////////////
// Utility
lazy val util = project
  .settings(commonSettings)

lazy val codeGen = project
  .settings(
    commonSettings,
    libraryDependencies ++= Seq(
      "dev.zio" %% "zio-http"     % zioHttpVersion withSources (),
      "dev.zio" %% "zio-http-gen" % zioHttpVersion withSources ()
    )
  )

////////////////////////////////////////////////////////////////////////////////////
// Web
lazy val bundlerSettings: Project => Project =
  _.enablePlugins(ScalaJSBundlerPlugin)
    .settings(
      webpack / version := "5.96.1",
      Compile / fastOptJS / artifactPath := ((Compile / fastOptJS / crossTarget).value /
        ((fastOptJS / moduleName).value + "-opt.js")),
      Compile / fullOptJS / artifactPath := ((Compile / fullOptJS / crossTarget).value /
        ((fullOptJS / moduleName).value + "-opt.js")),
      useYarn                                   := true,
      run / fork                                := true,
      Global / scalaJSStage                     := FastOptStage,
      Compile / scalaJSUseMainModuleInitializer := true,
      Test / scalaJSUseMainModuleInitializer    := false,
      webpackEmitSourceMaps                     := false,
      scalaJSLinkerConfig ~= {
        _.withSourceMap(false) // .withRelativizeSourceMapBase(None)
      },
      Compile / npmDependencies ++= Seq(
      )
    )

lazy val withCssLoading: Project => Project =
  _.settings(
    /* custom webpack file to include css */
    webpackConfigFile := Some((ThisBuild / baseDirectory).value / "custom.webpack.config.js"),
    Compile / npmDevDependencies ++= Seq(
      "webpack-merge" -> "6.0.1",
      "css-loader"    -> "7.1.2",
      "style-loader"  -> "4.0.0",
      "file-loader"   -> "6.2.0",
      "url-loader"    -> "4.1.1"
    )
  )

lazy val commonWeb: Project => Project =
  _.settings(
    libraryDependencies ++= Seq(
      "com.github.ghostdogpr" %%% "caliban-client"    % calibanVersion withSources (),
      "dev.zio" %%% "zio"                             % zioVersion withSources (),
      "com.softwaremill.sttp.client4" %%% "core"      % "4.0.3" withSources (),
      "com.softwaremill.sttp.client4" %%% "zio-json"  % "4.0.3" withSources (),
      "io.github.cquiroz" %%% "scala-java-time"       % "2.6.0" withSources (),
      "io.github.cquiroz" %%% "scala-java-time-tzdb"  % "2.6.0" withSources (),
      "org.scala-js" %%% "scalajs-dom"                % "2.8.0" withSources (),
      "com.olvind" %%% "scalablytyped-runtime"        % "2.4.2",
      "com.github.japgolly.scalajs-react" %%% "core"  % scalajsReactVersion withSources (),
      "com.github.japgolly.scalajs-react" %%% "extra" % scalajsReactVersion withSources (),
      "com.lihaoyi" %%% "scalatags"                   % "0.13.1" withSources (),
      "com.github.japgolly.scalacss" %%% "core"       % "1.0.0" withSources (),
      "com.github.japgolly.scalacss" %%% "ext-react"  % "1.0.0" withSources ()
    ),
    organizationName                     := "Roberto Leibman",
    startYear                            := Some(2024),
    headerLicense                        := Some(HeaderLicense.MIT("2024", "Roberto Leibman", HeaderLicenseStyle.Detailed)),
    Compile / unmanagedSourceDirectories := Seq((Compile / scalaSource).value),
    Test / unmanagedSourceDirectories    := Seq((Test / scalaSource).value)
    //    webpackDevServerPort                 := 8009
  )

lazy val stLib = project
  .in(file("full-zio-stack-stLib"))
  .enablePlugins(ScalablyTypedConverterGenSourcePlugin)
  .settings(
    name                     := "full-zio-stack-stLib",
    scalaVersion             := SCALA,
    useYarn                  := true,
    stOutputPackage          := "net.leibman.fullziostack",
    stFlavour                := Flavour.ScalajsReact,
    stReactEnableTreeShaking := Selection.All,
    Compile / npmDependencies ++= Seq(
      "@types/react"      -> reactVersion,
      "@types/react-dom"  -> reactVersion,
      "react"             -> reactVersion,
      "react-dom"         -> reactVersion,
      "@types/prop-types" -> "^15.7.0",
      "csstype"           -> "^3.1.0",
      "semantic-ui-react" -> "^2.1.5"
    ),
    Test / npmDependencies ++= Seq(
      "react"     -> reactVersion,
      "react-dom" -> reactVersion
    ),
    scalaJSUseMainModuleInitializer := true,
    // focus only on these libraries
    /* disabled because it somehow triggers many warnings */
    scalaJSLinkerConfig ~= (_.withSourceMap(false)),
    stMinimize       := Selection.AllExcept("semantic-ui-react"),
    organizationName := "Roberto Leibman",
    startYear        := Some(2024),
    headerLicense    := Some(HeaderLicense.MIT("2024", "Roberto Leibman", HeaderLicenseStyle.Detailed))
  )

lazy val client = project
  .dependsOn(modelJS, dbJS, stLib)
  .settings(commonSettings)
  .configure(bundlerSettings)
  .configure(withCssLoading)
  .configure(commonWeb)
  .enablePlugins(
    AutomateHeaderPlugin,
    GitVersioning,
    ScalaJSPlugin
  )
  .settings(
    name := "full-zio-stack-web",
    libraryDependencies ++= Seq(
      "dev.zio" %%% "zio-json" % zioJsonVersion withSources ()
    ),
    debugDist := {

      val assets = (ThisBuild / baseDirectory).value / "client" / "src" / "main" / "web"

      val artifacts = (Compile / fastOptJS / webpack).value
      val artifactFolder = (Compile / fastOptJS / crossTarget).value
      val debugFolder = (ThisBuild / baseDirectory).value / "debugDist"

      debugFolder.mkdirs()
      FileUtils.copyDirectory(assets, debugFolder, true)
      artifacts.foreach { artifact =>
        val target = artifact.data.relativeTo(artifactFolder) match {
          case None          => debugFolder / artifact.data.name
          case Some(relFile) => debugFolder / relFile.toString
        }
        Files.copy(artifact.data.toPath, target.toPath, REPLACE_EXISTING)
      }

      debugFolder
    },
    dist := {
      val assets = (ThisBuild / baseDirectory).value / "client" / "src" / "main" / "web"

      val artifacts = (Compile / fullOptJS / webpack).value
      val artifactFolder = (Compile / fullOptJS / crossTarget).value
      val distFolder = (ThisBuild / baseDirectory).value / "dist"

      distFolder.mkdirs()
      FileUtils.copyDirectory(assets, distFolder, true)
      artifacts.foreach { artifact =>
        val target = artifact.data.relativeTo(artifactFolder) match {
          case None          => distFolder / artifact.data.name
          case Some(relFile) => distFolder / relFile.toString
        }
        Files.copy(artifact.data.toPath, target.toPath, REPLACE_EXISTING)
      }

      distFolder
    }
  )

//////////////////////////////////////////////////////////////////////////////////////////////////
// Root project
lazy val root = project
  .in(file("."))
  .aggregate(modelJVM, modelJS, api, util, dbJVM, dbJS, stLib, client, codeGen)
  .settings(
    name           := "full-zio-stack",
    publish / skip := true,
    version        := "0.1.0",
    headerLicense  := None
  )
