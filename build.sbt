import java.nio.file.Files
import java.nio.file.StandardCopyOption.REPLACE_EXISTING

//////////////////////////////////////////////////////////////////////////////////////////////////
// Global stuff
Global / onChangedBuildSource := ReloadOnSourceChanges
Global / scalaVersion         := "3.0.1"

//////////////////////////////////////////////////////////////////////////////////////////////////
// Shared settings
lazy val buildInfoSettings =
  Seq(
    buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion)
  )

lazy val gitSettings =
  Seq(
    git.useGitDescribe := true
  )

lazy val licenseSettings = Seq(
  startYear := Some(2021),
  headerLicense := Some(HeaderLicense.MIT("2021", "Roberto Leibman", HeaderLicenseStyle.SpdxSyntax))
)

lazy val zioVersion = "1.0.9"

lazy val commonSettings: Project => Project =
  _.enablePlugins(AutomateHeaderPlugin, GitVersioning, BuildInfoPlugin)
    .settings(buildInfoSettings, gitSettings, licenseSettings)
    .settings(
      scalaVersion := "3.0.1",
      scalacOptions ++= Seq(
        "-no-indent", //scala3
        "-old-syntax", //scala3
        "-encoding",
        "UTF-8",
        "-Xtarget:11",
        "-Xfatal-warnings",
        "-explain-types",
        "-deprecation",
        "-feature",
        "-unchecked",
        "-language:implicitConversions"
//        "-Yexplicit-nulls", //Make reference types non-nullable. Nullable types can be expressed with unions: e.g. String|Null.
      ),
      libraryDependencies ++= Seq(
        //ZIO
        "dev.zio" %% "zio" % zioVersion withSources (),
        //        "dev.zio" %% "zio-logging" % "0.5.11" withSources(),
        //Test
        "dev.zio" %% "zio-test"     % zioVersion % "test" withSources (),
        "dev.zio" %% "zio-test-sbt" % zioVersion % "test" withSources ()
        //        "dev.zio" %% "zio-test-magnolia" % zioVersion % "test" withSources(), // optional
      )
    )

//////////////////////////////////////////////////////////////////////////////////////////////////
// Shared
lazy val sharedJVM = shared.jvm
lazy val sharedJS = shared.js
lazy val shared = crossProject(JSPlatform, JVMPlatform)
  .configure(commonSettings)
  .jvmSettings(
    libraryDependencies ++= Seq(
      //      "dev.zio" %% "zio-logging-slf4j" % "0.5.11" withSources(),
    )
  )
  .jsSettings(
    libraryDependencies ++= Seq(
      //      "dev.zio" %%% "zio-logging-jsconsole" % "0.5.11" withSources(),
    )
  )

//////////////////////////////////////////////////////////////////////////////////////////////////
// Server

val doobieVersion = "1.0.0-M5"
val quillVersion = "3.8.0"
val zioConfigVersion = "1.0.6"

lazy val server = project
  .configure(commonSettings)
  .dependsOn(sharedJVM)
  .settings(
    libraryDependencies ++= Seq(
      "io.d11"       %% "zhttp"                 % "1.0.0.0-RC17" withSources (),
      "dev.zio"      %% "zio-json"              % "0.2.0-M1" withSources (),
      "dev.zio"      %% "zio-config"            % zioConfigVersion withSources(),
//      "dev.zio"      %% "zio-config-magnolia"   % zioConfigVersion withSources(),
      "dev.zio"      %% "zio-config-typesafe"   % zioConfigVersion withSources(),
//      "io.getquill"  %% "quill-jdbc"            % quillVersion withSources(),
//      "io.getquill"  %% "quill-jdbc-zio"        % quillVersion withSources(),
      "org.tpolecat" %% "doobie-core"           % doobieVersion withSources (),
      "org.tpolecat" %% "doobie-hikari"         % doobieVersion withSources (), // HikariCP transactor.
//      "org.tpolecat"  % "doobie-quill_2.13"     % doobieVersion withSources (), // Support for Quill 3.6.1
      "mysql"         % "mysql-connector-java"  % "8.0.26" withSources (),
      "dev.zio"      %% "zio-interop-cats"      % "3.1.1.0" withSources (),
//      "org.tpolecat" % "doobie-scalatest_2.13" % doobieVersion % "test" withSources () // ScalaTest support for typechecking statements.
    )
  )

//////////////////////////////////////////////////////////////////////////////////////////////////
// Utility
lazy val util = project
  .configure(commonSettings)
  .settings(libraryDependencies ++= Seq())

//////////////////////////////////////////////////////////////////////////////////////////////////
// Client (scala.js)
/** Custom task to start demo with webpack-dev-server, use as `<project>/start`.
  * Just `start` also works, and starts all frontend demos
  *
  * After that, the incantation is this to watch and compile on change:
  * `~<project>/fastOptJS::webpack`
  */
lazy val start = TaskKey[Unit]("start")

/** Say just `dist` or `<project>/dist` to make a production bundle in
  * `dist` for publishing
  */
lazy val dist = TaskKey[File]("dist")

val scalajsReactVersion = "2.0.0-RC2"

// specify versions for all of reacts dependencies to compile less since we have many demos here
lazy val reactNpmDeps: Project => Project =
  _.settings(
    stTypescriptVersion := "3.9.3",
    Compile / npmDependencies ++= Seq(
      "react"             -> "16.13.1",
      "react-dom"         -> "16.13.1",
      "@types/react"      -> "16.9.42",
      "@types/react-dom"  -> "16.9.8",
      "csstype"           -> "2.6.11",
      "@types/prop-types" -> "15.7.3"
    )
  )

lazy val bundlerSettings: Project => Project =
  _.settings(
    Compile / fastOptJS / webpackExtraArgs += "--mode=development",
    Compile / fullOptJS / webpackExtraArgs += "--mode=production",
    Compile / fastOptJS / webpackDevServerExtraArgs += "--mode=development",
    Compile / fullOptJS / webpackDevServerExtraArgs += "--mode=production"
  )

lazy val withCssLoading: Project => Project =
  _.settings(
    /* custom webpack file to include css */
    webpackConfigFile := Some((ThisBuild / baseDirectory).value / "client" / "custom.webpack.config.js"),
    Compile / npmDevDependencies ++= Seq(
      "webpack-merge" -> "4.2.2",
      "css-loader"    -> "3.4.2",
      "style-loader"  -> "1.1.3",
      "file-loader"   -> "5.1.0",
      "url-loader"    -> "4.1.0"
    )
  )

lazy val stLib = project
  .in(file("full-zio-stack-stLib"))
  .enablePlugins(ScalablyTypedConverterGenSourcePlugin)
  .configure(reactNpmDeps)
  .settings(
    name                     := "full-zio-stack-stLib",
    scalaVersion             := "2.13.6",
    useYarn                  := true,
    stOutputPackage          := "net.leibman.fullziostack",
    stFlavour                := Flavour.Japgolly,
    stReactEnableTreeShaking := Selection.All,
    Compile / npmDependencies ++= Seq(
      "semantic-ui-react" -> "2.0.3"
    ),
    scalaJSUseMainModuleInitializer := true,
    /* disabled because it somehow triggers many warnings */
    scalaJSLinkerConfig ~= (_.withSourceMap(false)),
    libraryDependencies ++= Seq(
      "com.github.japgolly.scalajs-react" %%% "core-ext-cats"            % scalajsReactVersion withSources (),
      "com.github.japgolly.scalajs-react" %%% "core"                     % scalajsReactVersion withSources (),
      "com.github.japgolly.scalajs-react" %%% "extra"                    % scalajsReactVersion withSources (),
      "com.github.japgolly.scalajs-react" %%% "callback-ext-cats_effect" % scalajsReactVersion withSources ()
    )
  )

lazy val client = project
  .enablePlugins(ScalaJSPlugin, ScalaJSBundlerPlugin)
  .dependsOn(sharedJS)
  .configure(commonSettings, reactNpmDeps, bundlerSettings, withCssLoading)
  .settings(
    name                            := "full-zio-stack-client",
    useYarn                         := true,
    webpackDevServerPort            := 8004,
    scalaJSUseMainModuleInitializer := true,
    /* disabled because it somehow triggers many warnings */
    scalaJSLinkerConfig := scalaJSLinkerConfig.value.withSourceMap(false),
    libraryDependencies ++= Seq(
      "com.github.japgolly.scalajs-react" %%% "core-ext-cats"            % scalajsReactVersion withSources (),
      "com.github.japgolly.scalajs-react" %%% "core"                     % scalajsReactVersion withSources (),
      "com.github.japgolly.scalajs-react" %%% "extra"                    % scalajsReactVersion withSources (),
      "com.github.japgolly.scalajs-react" %%% "callback-ext-cats_effect" % scalajsReactVersion withSources ()
    ),
    start := {
      (Compile / fastOptJS / startWebpackDevServer).value
    },
    dist := {
      val artifacts = (Compile / fastOptJS / webpack).value
      val artifactFolder = (Compile / fastOptJS / crossTarget).value
      val distFolder = (ThisBuild / baseDirectory).value / "dist"

      distFolder.mkdirs()
      artifacts.foreach { artifact =>
        val target = artifact.data.relativeTo(artifactFolder) match {
          case None          => distFolder / artifact.data.name
          case Some(relFile) => distFolder / relFile.toString
        }

        Files.copy(artifact.data.toPath, target.toPath, REPLACE_EXISTING)
      }

      val indexFrom = baseDirectory.value / "src/main/js/index.html"
      val indexTo = distFolder / "index.html"

      val indexPatchedContent = {
        import scala.jdk.CollectionConverters._
        Files
          .readAllLines(indexFrom.toPath, IO.utf8)
          .asScala
          .map(_.replaceAllLiterally("-fastopt-", "-opt-"))
          .mkString("\n")
      }

      Files.write(indexTo.toPath, indexPatchedContent.getBytes(IO.utf8))
      distFolder
    }
  )

//////////////////////////////////////////////////////////////////////////////////////////////////
// Root project
lazy val root = project
  .in(file("."))
  .aggregate(sharedJS, sharedJVM, server, client, util)
  .settings(
    name         := "full-zio-stack",
    publish      := {},
    publishLocal := {},
    version      := "0.1.0"
  )
