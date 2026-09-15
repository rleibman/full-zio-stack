////////////////////////////////////////////////////////////////////////////////////
// Common stuff
addSbtPlugin("com.github.sbt" % "sbt-git"             % "2.2.0")
addSbtPlugin("com.github.sbt" % "sbt-header"          % "5.11.0")
addSbtPlugin("com.github.sbt" % "sbt-native-packager" % "1.11.7")
addSbtPlugin("com.eed3si9n"   % "sbt-buildinfo"       % "0.13.1")
addSbtPlugin("org.scalameta"  % "sbt-scalafmt"        % "2.6.2")
// No sbt2 build of sbt-explicit-dependencies.
// addSbtPlugin("com.github.cb372"  % "sbt-explicit-dependencies" % "0.3.1")
addSbtPlugin("ch.epfl.scala" % "sbt-scalafix"    % "0.14.8")
addSbtPlugin("com.typesafe"  % "sbt-mima-plugin" % "1.2.0")

////////////////////////////////////////////////////////////////////////////////////
// Server
// No sbt2 build of sbt-revolver, so `reStart` is gone; use `server-ziohttp/run`.
// addSbtPlugin("io.spray" % "sbt-revolver" % "0.10.0")

////////////////////////////////////////////////////////////////////////////////////
// Web client
addSbtPlugin("org.scala-js"          % "sbt-scalajs"              % "1.22.0")
addSbtPlugin("com.github.ghostdogpr" % "caliban-codegen-sbt"      % "3.1.5")
addSbtPlugin("org.portable-scala"    % "sbt-scalajs-crossproject" % "1.4.0")
// sbt-scalajs-bundler (webpack) has no sbt2 build. Bundling is vite now -- see viteDistImpl in build.sbt and
// client/vite.config.js. Vite handles CSS and asset imports natively, so custom.webpack.config.js and its
// css-loader/style-loader/file-loader/url-loader stack are gone with it.
// The ScalablyTyped facades are a standalone build in stLib/ (sbt 2, on a locally published fork of sbt-converter).

////////////////////////////////////////////////////////////////////////////////////
// Testing
addSbtPlugin("io.stryker-mutator" % "sbt-stryker4s" % "1.1.1")
addSbtPlugin("org.scoverage"      % "sbt-scoverage" % "2.4.4")

libraryDependencies ++= Seq("org.eclipse.jgit" % "org.eclipse.jgit" % "7.8.0.202609011348-r")
