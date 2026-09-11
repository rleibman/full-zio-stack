// sbt-converter (ScalablyTyped) sbt 2 build: rleibman's fork, published locally from
// ~/projects/third-party/Converter (commit a19eaafd plus uncommitted fixes: scala3 = 3.8.4, Scala 3.8+ stdlib selection) -- upstream only publishes an sbt 1 build.
addSbtPlugin("com.github.sbt"              % "sbt-header"    % "5.11.0")
addSbtPlugin("org.scala-js"                % "sbt-scalajs"   % "1.22.0")
addSbtPlugin("org.scalablytyped.converter" % "sbt-converter" % "1.0.0-beta45+3-a19eaafd+20260911-1101-SNAPSHOT")

// The fork's core_3 uses os-lib and ammonite-ops via their _2.13 builds, which drag in scala-collection-compat_2.13
// next to the _3 one the rest of the classpath uses, and sbt 2 refuses mixed cross-version suffixes outright.
// Same classes either way (Scala 3 reads 2.13 binaries), so keep only the _3 copy.
excludeDependencies += ExclusionRule("org.scala-lang.modules", "scala-collection-compat_2.13")
