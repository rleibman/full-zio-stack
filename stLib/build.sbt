//////////////////////////////////////////////////////////////////////////////////////////////////
// ScalablyTyped facades for the npm libraries the web client uses.
//
// This is a STANDALONE build, not a subproject of the main build. Generate and publish it locally before building the
// main project:
//
//   cd stLib && npm install && sbt --error publishLocal
//
// scalajsReactVersion here and "react" in package.json (both here and in client/package.json) must move together:
// scalajs-react 4.x is written for React 19.
lazy val SCALA = "3.9.0"

val scalajsReactVersion = "4.0.0"

version := "3.0.0"

enablePlugins(ScalablyTypedConverterExternalNpmPlugin)

Global / onChangedBuildSource := ReloadOnSourceChanges
scalaVersion                  := SCALA
Global / scalaVersion         := SCALA

organization     := "net.leibman"
startYear        := Some(2024)
organizationName := "Roberto Leibman"
headerLicense    := Some(HeaderLicense.MIT("2024", "Roberto Leibman", HeaderLicenseStyle.Detailed))
name             := "full-zio-stack-stlib"
// Under .st so the facades can never clash with the app's own packages.
stOutputPackage  := "net.leibman.fullziostack.st"
stFlavour        := Flavour.ScalajsReact

// ExternalNpm: npm dependencies live in ./package.json and are installed with `npm install` here.
externalNpm := baseDirectory.value

libraryDependencies ++= Seq(
  "com.github.japgolly.scalajs-react" %% "core"  % scalajsReactVersion,
  "com.github.japgolly.scalajs-react" %% "extra" % scalajsReactVersion
)

// sbt 2 has no `%%%`: in a Scala.js project `%%` already resolves the _sjs1_3 artifact.
dependencyOverrides += "com.github.japgolly.scalajs-react" %% "core" % scalajsReactVersion

// The converter compiles the generated facades against a hard-coded scalajs-react 2.1.3 (Versions.scalajsReact), and
// 4.x wins at resolution. Same as it always was under sbt 1, which only warned; sbt 2's eviction check is fatal.
// `%%` would only cover the JVM artifacts, hence the explicit _sjs1_3 names.
libraryDependencySchemes ++= Seq(
  "com.github.japgolly.scalajs-react" % "core_sjs1_3"         % VersionScheme.Always,
  "com.github.japgolly.scalajs-react" % "core-generic_sjs1_3" % VersionScheme.Always,
  "com.github.japgolly.scalajs-react" % "extra_sjs1_3"        % VersionScheme.Always,
  "com.github.japgolly.scalajs-react" % "facade_sjs1_3"       % VersionScheme.Always,
  "com.github.japgolly.scalajs-react" % "util_sjs1_3"         % VersionScheme.Always,
  "com.github.japgolly.scalajs-react" % "callback_sjs1_3"     % VersionScheme.Always,
)

/* disabled because it somehow triggers many warnings */
scalaJSLinkerConfig ~= (_.withSourceMap(false))

// Fully generate facades only for these libraries; everything else is minimized to the reachable API surface.
stMinimize := Selection.AllExcept("@mui/material")

licenses := Seq(License.MIT)

doc / sources := Nil
