val scala213 = "2.13.16"

ThisBuild / scalaVersion := scala213
ThisBuild / crossScalaVersions := Seq("2.12.20", scala213, "3.3.5")

ThisBuild / licenses := Seq("MIT" -> url("http://opensource.org/licenses/MIT"))

val kindProjectorV = "0.13.3"
val betterMonadicForV = "0.3.1"

// Projects
lazy val `epimetheus-redis4cats` = project.in(file("."))
  .disablePlugins(MimaPlugin)
  .enablePlugins(NoPublishPlugin)
  .settings(universalSettings)
  .aggregate(core)

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

// Microsite via sbt-davenverse
lazy val site = project.in(file("site"))
  .disablePlugins(MimaPlugin)
  .enablePlugins(NoPublishPlugin)
  .enablePlugins(DavenverseMicrositePlugin)
  .dependsOn(core)
  .settings{
    import microsites._
    Seq(
      micrositeDescription := "Redis4cats Metrics",
    )
  }

// For regular modules, but not the root project
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

// For regular modules and the root project
lazy val universalSettings = Seq(
  // overrides the stale one in sbt-davenverse
  libraryDependencies ++= PartialFunction.condOpt(CrossVersion.partialVersion(scalaVersion.value)) { case Some((2, _)) =>
    Seq(
      compilerPlugin("org.typelevel" % "kind-projector" % kindProjectorV cross CrossVersion.full),
    )
  }.toList.flatten
)
