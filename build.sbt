ThisBuild / tlBaseVersion := "0.3" // current series x.y

ThisBuild / organization := "io.chrisdavenport"
ThisBuild / organizationName := "Christopher Davenport"
ThisBuild / startYear := Some(2022)
ThisBuild / licenses := Seq(License.MIT)
ThisBuild / developers := List(
  tlGitHubDev("christopherdavenport", "Christopher Davenport")
)

// sbt-davenverse published a snapshot from main on every push. Dropped: the
// Central Portal will not enable snapshots for the io.chrisdavenport namespace.
ThisBuild / tlCiReleaseBranches := Seq()

val scala213 = "2.13.18"
ThisBuild / scalaVersion := scala213
ThisBuild / crossScalaVersions := Seq(scala213, "3.3.8")

val kindProjectorV = "0.13.4"
val betterMonadicForV = "0.3.1"

// Projects
lazy val `epimetheus-redis4cats` = tlCrossRootProject.aggregate(core)

lazy val core = project.in(file("core"))
  .settings(commonSettings)
  .settings(
    name := "epimetheus-redis4cats",
    // we need to work around deprecated methods we have to implement from upstream, but we can't use @nowarn
    // as long as 2.12 is still supported – so we rely on 2.13 warnings only
    scalacOptions := (CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, y)) if y >= 13 =>  scalacOptions.value :+ "-Wconf:cat=deprecation:is"
      case _ => scalacOptions.value.filter(_ != "-Xfatal-warnings")
    })
  )

lazy val site = project.in(file("site"))
  .enablePlugins(TypelevelSitePlugin)
  .dependsOn(core)
  .settings(universalSettings)
  .settings(
    laikaTheme := tlSiteHelium.value.site
      .topNavigationBar(
        homeLink = laika.helium.config.IconLink.internal(laika.ast.Path.Root / "index.md", laika.helium.config.HeliumIcon.home)
      )
      .build
  )

// For regular modules
lazy val commonSettings = universalSettings ++ Seq(
  libraryDependencies ++= Seq(
    "io.chrisdavenport"           %% "epimetheus"                 % "0.5.0",
    "dev.profunktor"              %% "redis4cats-effects"         % "1.7.2"
  ),
  libraryDependencies ++= PartialFunction.condOpt(CrossVersion.partialVersion(scalaVersion.value)) { case Some((2, _)) =>
    Seq(
      compilerPlugin("com.olegpy" %% "better-monadic-for" % betterMonadicForV)
    )
  }.toList.flatten
)

// Compiler settings DavenversePlugin injected globally. sbt-typelevel-ci-release
// does not supply these (only sbt-typelevel-settings would).
lazy val universalSettings = Seq(
  libraryDependencies ++= PartialFunction.condOpt(CrossVersion.partialVersion(scalaVersion.value)) { case Some((2, _)) =>
    Seq(
      compilerPlugin("org.typelevel" % "kind-projector" % kindProjectorV cross CrossVersion.full),
    )
  }.toList.flatten,
  scalacOptions ++= (CrossVersion.partialVersion(scalaVersion.value) match {
    case Some((3, _)) => Seq("-Ykind-projector")
    case Some((2, 12)) => Seq("-Ypartial-unification")
    case _ => Nil
  })
)
