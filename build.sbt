import java.nio.file.Files
import java.nio.file.StandardCopyOption.REPLACE_EXISTING

//////////////////////////////////////////////////////////////////////////////////////////////////
// Global stuff
Global / onChangedBuildSource := ReloadOnSourceChanges
Global / scalaVersion         := "3.1.2"

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
  startYear     := Some(2021),
  headerLicense := Some(HeaderLicense.MIT("2021", "Roberto Leibman", HeaderLicenseStyle.SpdxSyntax))
)

lazy val scala3Opts = Seq(
  "-no-indent", // scala3
  "-old-syntax", // scala3
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
  "-Yexplicit-nulls" // Make reference types non-nullable. Nullable types can be expressed with unions: e.g. String|Null.
)

val zioVersion = "2.0.0-RC6"
val zioHttpVersion = "2.0.0-RC7"
val zioConfigVersion = "3.0.0-RC9"
val zioLoggingVersion = "2.0.0-RC10"
val zioJsonVersion = "0.3.0-RC8"
val calibanVersion = "2.0.0-RC2"
val zioSqlVersion = "0.0.1"

lazy val commonSettings: Project => Project =
  _.enablePlugins(AutomateHeaderPlugin, GitVersioning, BuildInfoPlugin)
    .configs(IntegrationTest)
    .settings(Defaults.itSettings, buildInfoSettings, gitSettings, licenseSettings)
    .settings(
      scalaVersion := "3.1.2",
      scalacOptions ++= scala3Opts
    )

//////////////////////////////////////////////////////////////////////////////////////////////////
// Model
lazy val modelJVM = model.jvm
lazy val modelJS = model.js
lazy val model = crossProject(JSPlatform, JVMPlatform)
  .configure(commonSettings)
  .settings(
    libraryDependencies ++= Seq(
      "dev.zio" %% "zio-json" % zioJsonVersion withSources ()
    )
  )
  .jvmSettings(
  )
  .jsSettings(
  )

//////////////////////////////////////////////////////////////////////////////////////////////////
// Server

val quillVersion = "3.17.0.Beta3.0-RC4"

lazy val dbJVM = db.jvm
lazy val dbJS = db.js
lazy val db = crossProject(JSPlatform, JVMPlatform)
  .configure(commonSettings)
  .dependsOn(model)
  .settings(
    libraryDependencies ++= Seq(
      "dev.zio" %% "zio-json"            % zioJsonVersion withSources (),
      "dev.zio" %% "zio-config-magnolia" % zioConfigVersion withSources (),
      "dev.zio" %% "zio-config-typesafe" % zioConfigVersion withSources (),
      "dev.zio" %% "zio-test"            % zioVersion % "it, test" withSources (),
      "dev.zio" %% "zio-test-sbt"        % zioVersion % "it, test" withSources (),
      "dev.zio" %% "zio-test-magnolia"   % zioVersion % "it, test" withSources ()
    )
  )
  .jvmSettings(
    libraryDependencies ++= Seq(
      "io.getquill" %% "quill-jdbc" % quillVersion withSources () excludeAll (
        ExclusionRule("org.scala-lang.modules", "scala-collection-compat_2.13"),
        ExclusionRule("com.lihaoyi", "sourcecode_2.13"),
        ExclusionRule("com.lihaoyi", "fansi_2.13"),
        ExclusionRule("com.lihaoyi", "pprint_2.13"),
      ),
      "io.getquill" %% "quill-jdbc-zio" % quillVersion withSources () excludeAll (
        ExclusionRule("org.scala-lang.modules", "scala-collection-compat_2.13"),
        ExclusionRule("com.lihaoyi", "sourcecode_2.13"),
        ExclusionRule("com.lihaoyi", "fansi_2.13"),
        ExclusionRule("com.lihaoyi", "pprint_2.13"),
      ),
      "io.getquill" %% "quill-jasync-postgres" % quillVersion withSources () excludeAll (
        ExclusionRule("org.scala-lang.modules", "scala-collection-compat_2.13"),
        ExclusionRule("com.lihaoyi", "sourcecode_2.13"),
        ExclusionRule("com.lihaoyi", "fansi_2.13"),
        ExclusionRule("com.lihaoyi", "pprint_2.13"),
      ),
      "mysql" % "mysql-connector-java" % "8.0.29" withSources ()
    )
  )
  .jsSettings(
    libraryDependencies ++= Seq(
      "io.d11"                                    %% "zhttp" % zioHttpVersion withSources (),
      "com.github.ghostdogpr" %%% "caliban-client" % calibanVersion withSources ()
    )
  )

lazy val api = project
  .configure(commonSettings)
  .dependsOn(modelJVM, dbJVM)
  .settings(
    libraryDependencies ++= Seq(
      "io.d11"  %% "zhttp"               % zioHttpVersion withSources (),
      "dev.zio" %% "zio-test"            % zioVersion % "it, test" withSources (),
      "dev.zio" %% "zio-test-sbt"        % zioVersion % "it, test" withSources (),
      "dev.zio" %% "zio-test-magnolia"   % zioVersion % "it, test" withSources (),
      "dev.zio" %% "zio-config"          % zioConfigVersion withSources (),
      "dev.zio" %% "zio-config-magnolia" % zioConfigVersion withSources (),
      "dev.zio" %% "zio-config-typesafe" % zioConfigVersion withSources (),
      "dev.zio" %% "zio-logging"         % zioLoggingVersion withSources (),
      "dev.zio" %% "zio-logging-slf4j"   % zioLoggingVersion withSources (),
      "dev.zio" %% "zio-json"            % zioJsonVersion withSources (),
//      "dev.zio" %% "zio-sql-mysql" % zioSqlVersion withSources(), // not really, neither compiled for 3, nor for the latest version of zio
      "com.github.ghostdogpr" %% "caliban"          % calibanVersion withSources (),
      "com.github.ghostdogpr" %% "caliban-zio-http" % calibanVersion withSources (),
//      "io.getquill" %% "quill-jdbc-zio" % "3.17.0.Beta3.0-RC1" withSources(),
      "ch.qos.logback" % "logback-classic" % "1.3.0-alpha14" withSources (),
      "io.d11"        %% "zhttp-test"      % zioHttpVersion % "it, test"
    )
  )

//////////////////////////////////////////////////////////////////////////////////////////////////
// Utility
lazy val util = project
  .configure(commonSettings)
  .settings()

//////////////////////////////////////////////////////////////////////////////////////////////////
// Client (scala.js)
/** Custom task to start demo with webpack-dev-server, use as `<project>/start`. Just `start` also works, and starts all frontend demos
  *
  * After that, the incantation is this to watch and compile on change: `~<project>/fastOptJS::webpack`
  */
lazy val start = TaskKey[Unit]("start")

/** Say just `dist` or `<project>/dist` to make a production bundle in `dist` for publishing
  */
lazy val dist = TaskKey[File]("dist")

val scalajsReactVersion = "2.1.1"
val reactVersion = "17.0.0"

// specify versions for all of reacts dependencies to compile less since we have many demos here
lazy val reactNpmDeps: Project => Project =
  _.settings(
    Compile / npmDependencies ++= Seq(
      "react-dom"         -> reactVersion,
      "@types/react-dom"  -> reactVersion,
      "react"             -> reactVersion,
      "@types/react"      -> reactVersion,
      "csstype"           -> "2.6.11",
      "@types/prop-types" -> "15.7.3"
    )
  )

lazy val bundlerSettings: Project => Project =
  _.enablePlugins(ScalaJSBundlerPlugin)
    .settings(
      Compile / fastOptJS / webpackDevServerExtraArgs += "--mode=development",
      Compile / fullOptJS / webpackDevServerExtraArgs += "--mode=production"
    )

lazy val withCssLoading: Project => Project =
  _.settings(
    /* custom webpack file to include css */
    webpackConfigFile := Some((ThisBuild / baseDirectory).value / "custom.webpack.config.js"),
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
    scalaVersion             := "3.1.2",
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
      "com.github.japgolly.scalajs-react" %%% "core"  % scalajsReactVersion withSources (),
      "com.github.japgolly.scalajs-react" %%% "extra" % scalajsReactVersion withSources ()
    )
  )

lazy val client = project
  .enablePlugins(ScalaJSPlugin)
  .dependsOn(modelJS, dbJS, stLib)
  .configure(commonSettings, reactNpmDeps, bundlerSettings, withCssLoading)
  .settings(
    name                            := "full-zio-stack-client",
    useYarn                         := true,
    webpackDevServerPort            := 8004,
    scalaJSUseMainModuleInitializer := true,
    /* disabled because it somehow triggers many warnings */
    scalaJSLinkerConfig := scalaJSLinkerConfig.value.withSourceMap(false),
    libraryDependencies ++= Seq(
      "dev.zio" %%% "zio"                       % zioVersion withSources (),
      "io.d11"                                 %% "zhttp" % zioHttpVersion withSources (), // TODO remove this, should use the dbJS module instead
      "io.github.cquiroz" %%% "scala-java-time" % "2.3.0" withSources (),
      "io.github.cquiroz" %%% "scala-java-time-tzdb"  % "2.3.0" withSources (),
      "com.olvind" %%% "scalablytyped-runtime"        % "2.4.2" withSources (),
      "com.github.japgolly.scalajs-react" %%% "core"  % scalajsReactVersion withSources (),
      "com.github.japgolly.scalajs-react" %%% "extra" % scalajsReactVersion withSources (),
      "org.scala-js" %%% "scalajs-dom"                % "2.1.0",
      ("org.scala-js" %%% "scalajs-java-securerandom" % "1.0.0").cross(CrossVersion.for3Use2_13)
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
          .map(_.replaceAllLiterally("-fastopt-", "-fastopt-"))
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
  .aggregate(modelJS, modelJVM, api, util, dbJS, dbJVM, stLib, client)
  .settings(
    name           := "full-zio-stack",
    publish / skip := true,
    version        := "0.1.0"
  )
