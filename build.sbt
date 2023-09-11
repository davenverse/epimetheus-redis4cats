val scala213 = "2.13.12"

ThisBuild / scalaVersion := scala213
ThisBuild / crossScalaVersions := Seq("2.12.17", scala213, "3.2.2")

ThisBuild / licenses := Seq("MIT" -> url("http://opensource.org/licenses/MIT"))

val kindProjectorV = "0.13.2"
val betterMonadicForV = "0.3.1"

// Projects
lazy val `epimetheus-redis4cats` = project.in(file("."))
  .disablePlugins(MimaPlugin)
  .enablePlugins(NoPublishPlugin)
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

// General Settings
lazy val commonSettings = Seq(
  libraryDependencies ++= Seq(
    "io.chrisdavenport"           %% "epimetheus"                 % "0.5.0",
    "dev.profunktor"              %% "redis4cats-effects"         % "1.4.1"
  ),
  libraryDependencies ++= PartialFunction.condOpt(CrossVersion.partialVersion(scalaVersion.value)) { case Some((2, _)) =>
    Seq(
      compilerPlugin("org.typelevel" % "kind-projector" % kindProjectorV cross CrossVersion.full),
      compilerPlugin("com.olegpy" %% "better-monadic-for" % betterMonadicForV)
    )
  }.toList.flatten
)
