import java.nio.file.Files
import java.nio.file.StandardCopyOption.REPLACE_EXISTING

//////////////////////////////////////////////////////////////////////////////////////////////////
// Global stuff
Global / onChangedBuildSource := ReloadOnSourceChanges
Global / scalaVersion := "3.1.0"

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

lazy val zioVersion = "1.0.13"

lazy val scala3Opts = Seq(
  "-no-indent", //scala3
  "-old-syntax", //scala3
  "-encoding",
  "utf-8", // Specify character encoding used by source files.
  "-feature", // Emit warning and location for usages of features that should be imported explicitly.
  "-language:existentials", // Existential types (besides wildcard types) can be written and inferred
  "-language:implicitConversions",
  "-language:higherKinds", // Allow higher-kinded types
  "-unchecked", // Enable additional warnings where generated code depends on assumptions.
  "-Xfatal-warnings", // Fail the compilation if there are any warnings.
  "-deprecation", // Emit warning and location for usages of deprecated APIs.
  "-explain-types", // Explain type errors in more detail.
  "-Yexplicit-nulls", //Make reference types non-nullable. Nullable types can be expressed with unions: e.g. String|Null.
)

lazy val commonSettings: Project => Project =
  _.enablePlugins(AutomateHeaderPlugin, GitVersioning, BuildInfoPlugin)
    .settings(buildInfoSettings, gitSettings, licenseSettings)
    .settings(
      scalaVersion := "3.1.0",
      scalacOptions ++= scala3Opts,
      libraryDependencies ++= Seq(
        //ZIO
        "dev.zio" %% "zio" % zioVersion withSources(),
        "dev.zio" %% "zio-macros" % zioVersion withSources(),
        "dev.zio" %% "zio-logging" % "0.5.14" withSources(),
        //Test
        "dev.zio" %% "zio-test" % zioVersion % "test" withSources(),
        "dev.zio" %% "zio-test-sbt" % zioVersion % "test" withSources()
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

val zioConfigVersion = "1.0.10"

val quillVersion = "3.12.0.Beta1.7"

lazy val db = project
  .configure(commonSettings)
  .dependsOn(sharedJVM)
  .settings(
    libraryDependencies ++= Seq(
      "dev.zio" %% "zio-config-typesafe" % zioConfigVersion withSources(),
      "io.getquill" %% "quill-jdbc" % quillVersion withSources() excludeAll (
        ExclusionRule("org.scala-lang.modules", "scala-collection-compat_2.13"),
          ExclusionRule("com.lihaoyi", "sourcecode_2.13"),
          ExclusionRule("com.lihaoyi", "fansi_2.13"),
          ExclusionRule("com.lihaoyi", "pprint_2.13"),
        ),
      "io.getquill" %% "quill-jdbc-zio" % quillVersion withSources() excludeAll (
        ExclusionRule("org.scala-lang.modules", "scala-collection-compat_2.13"),
        ExclusionRule("com.lihaoyi", "sourcecode_2.13"),
        ExclusionRule("com.lihaoyi", "fansi_2.13"),
        ExclusionRule("com.lihaoyi", "pprint_2.13"),
      ),
      "io.getquill" %% "quill-jasync-postgres" % quillVersion withSources() excludeAll (
        ExclusionRule("org.scala-lang.modules", "scala-collection-compat_2.13"),
        ExclusionRule("com.lihaoyi", "sourcecode_2.13"),
        ExclusionRule("com.lihaoyi", "fansi_2.13"),
        ExclusionRule("com.lihaoyi", "pprint_2.13"),
      ),
      "mysql" % "mysql-connector-java" % "8.0.27" withSources()
    )
  )

lazy val server = project
  .configure(commonSettings)
  .dependsOn(sharedJVM, db)
  .settings(
    libraryDependencies ++= Seq(
      "io.d11" %% "zhttp" % "1.0.0.0-RC21" withSources(),
      "dev.zio" %% "zio-json" % "0.2.0-M3" withSources(), //NOte, starting 0.3.0 it brings in zio 2.0.0
      "dev.zio" %% "zio-config" % zioConfigVersion withSources(),
      "dev.zio" %% "zio-config-magnolia" % zioConfigVersion withSources(),
      "dev.zio" %% "zio-config-typesafe" % zioConfigVersion withSources(),
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

val scalajsReactVersion = "2.0.0"

// specify versions for all of reacts dependencies to compile less since we have many demos here
lazy val reactNpmDeps: Project => Project =
  _.settings(
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
    stFlavour                := Flavour.ScalajsReact,
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
//
//lazy val client = project
//  .enablePlugins(ScalaJSPlugin, ScalaJSBundlerPlugin)
//  .dependsOn(sharedJS)
//  .configure(commonSettings, reactNpmDeps, bundlerSettings, withCssLoading)
//  .settings(
//    name                            := "full-zio-stack-client",
//    useYarn                         := true,
//    webpackDevServerPort            := 8004,
//    scalaJSUseMainModuleInitializer := true,
//    /* disabled because it somehow triggers many warnings */
//    scalaJSLinkerConfig := scalaJSLinkerConfig.value.withSourceMap(false),
//    libraryDependencies ++= Seq(
//      "com.olvind" %%% "scalablytyped-runtime"                           % "2.4.2" withSources (),
//      "com.github.japgolly.scalajs-react" %%% "core-ext-cats"            % scalajsReactVersion withSources (),
//      "com.github.japgolly.scalajs-react" %%% "core"                     % scalajsReactVersion withSources (),
//      "com.github.japgolly.scalajs-react" %%% "extra"                    % scalajsReactVersion withSources (),
//      "com.github.japgolly.scalajs-react" %%% "callback-ext-cats_effect" % scalajsReactVersion withSources ()
//    ),
//    start := {
//      (Compile / fastOptJS / startWebpackDevServer).value
//    },
//    dist := {
//      val artifacts = (Compile / fastOptJS / webpack).value
//      val artifactFolder = (Compile / fastOptJS / crossTarget).value
//      val distFolder = (ThisBuild / baseDirectory).value / "dist"
//
//      distFolder.mkdirs()
//      artifacts.foreach { artifact =>
//        val target = artifact.data.relativeTo(artifactFolder) match {
//          case None          => distFolder / artifact.data.name
//          case Some(relFile) => distFolder / relFile.toString
//        }
//
//        Files.copy(artifact.data.toPath, target.toPath, REPLACE_EXISTING)
//      }
//
//      val indexFrom = baseDirectory.value / "src/main/js/index.html"
//      val indexTo = distFolder / "index.html"
//
//      val indexPatchedContent = {
//        import scala.jdk.CollectionConverters._
//        Files
//          .readAllLines(indexFrom.toPath, IO.utf8)
//          .asScala
//          .map(_.replaceAllLiterally("-fastopt-", "-opt-"))
//          .mkString("\n")
//      }
//
//      Files.write(indexTo.toPath, indexPatchedContent.getBytes(IO.utf8))
//      distFolder
//    }
//  )

//////////////////////////////////////////////////////////////////////////////////////////////////
// Root project
lazy val root = project
  .in(file("."))
  .aggregate(sharedJS, sharedJVM, server, util, db, stLib)
  .settings(
    name := "full-zio-stack",
    publish := {},
    publishLocal := {},
    version := "0.1.0"
  )
