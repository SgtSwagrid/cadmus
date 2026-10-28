import sbt._
import sbt.Keys._
import sbtunidoc.BaseUnidocPlugin.autoImport.*
import sbtunidoc.ScalaUnidocPlugin

val scala3 = "3.9.0"

ThisBuild / scalaVersion := scala3

ThisBuild / scalacOptions ++= Seq(
  "-explain",
  "-explain-types",
  "-explain-cyclic",
  "-deprecation",
  "-feature",
  "-unchecked",
  "-Wunused:all",
)

// Every project is named after the library so as not to clash with the
// projects of a build that includes this one by reference. The matrix's sources
// sit in their packages' directories rather than under a package prefix, as
// IntelliJ never gives one to the sources the JVM and JS share.
lazy val cadmus = projectMatrix
  .in(file("."))
  .settings(
    name := "cadmus",

    // A matrix resolves sources against the working directory, which is not
    // this build's base when another build includes it by reference.
    sourceDirectory := (ThisBuild / baseDirectory).value / "src",
    libraryDependencies += "org.scalameta" %% "munit" % "1.3.6" % Test,
  )
  .jvmPlatform(scalaVersions = Seq(scala3))
  .jsPlatform(scalaVersions = Seq(scala3))

// Code using Cadmus compiled with `-Xdrop-docs`, where Scala keeps no comments
// and asking for one throws, to test that each is read as none. The option
// holds for a whole compilation, so these tests sit beside the others, in
// `src/test-without-docs`, but are compiled apart from them.
lazy val `cadmus-without-docs` = projectMatrix
  .in(file("."))
  .dependsOn(cadmus)
  .settings(
    publish / skip := true,
    scalacOptions += "-Xdrop-docs",
    Compile / unmanagedSourceDirectories   := Nil,
    Compile / unmanagedResourceDirectories := Nil,
    Test / unmanagedSourceDirectories      := Seq(
      (ThisBuild / baseDirectory).value / "src" / "test-without-docs" / "scala",
    ),
    Test / unmanagedResourceDirectories := Nil,
    libraryDependencies += "org.scalameta" %% "munit" % "1.3.6" % Test,
  )
  .jvmPlatform(scalaVersions = Seq(scala3))
  .jsPlatform(scalaVersions = Seq(scala3))

lazy val `cadmus-root` = project
  .in(file("."))
  .enablePlugins(ScalaUnidocPlugin)
  .aggregate((cadmus.projectRefs ++ `cadmus-without-docs`.projectRefs) *)
  .settings(
    publish / skip := true,

    // The root shares the matrix's directory, so must not build its sources.
    Compile / unmanagedSourceDirectories   := Nil,
    Test / unmanagedSourceDirectories      := Nil,
    Compile / unmanagedResourceDirectories := Nil,
    Test / unmanagedResourceDirectories    := Nil,

    // The JS side would only document the same sources again.
    ScalaUnidoc / unidoc / unidocProjectFilter :=
      inProjects(cadmus.jvm(scala3)),
    ScalaUnidoc / unidoc / scalacOptions ++= Seq("-project", "Cadmus"),
  )
