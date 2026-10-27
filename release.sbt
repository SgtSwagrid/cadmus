ThisBuild / description := "Reads Scaladoc comments at compile time."

ThisBuild / homepage := Some(uri("https://alecdorrington.com/cadmus"))

ThisBuild / organization         := "com.alecdorrington"
ThisBuild / organizationName     := "Alec Dorrington"
ThisBuild / organizationHomepage := Some(uri("https://alecdorrington.com"))

ThisBuild / versionScheme := Some("strict")

ThisBuild / licenses :=
  List("MIT" -> uri("https://opensource.org/licenses/MIT"))

ThisBuild / developers := List(Developer(
  id = "SgtSwagrid",
  name = "Alec Dorrington",
  email = "alecdorrington@gmail.com",
  url = uri("https://github.com/SgtSwagrid"),
))
