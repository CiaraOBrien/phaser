lazy val root = project.in(file("."))
.aggregate(phaser.jvm)
.settings(
  Compile / sources := Nil,
  Test    / sources := Nil,
  publish           := {},
  publishLocal      := {},
  taste := (tasty / Compile / taste).value
)

lazy val phaser = crossProject(JVMPlatform, JSPlatform).crossType(CrossType.Pure).in(file("."))
.settings(  
  name                 := "phaser",
  version              := "0.2.1",
  scalaVersion         := "3.0.0-M3",
  organization         := "edu.yale.cafferty",
  organizationName     := "Cafferty Lab",
  organizationHomepage := Some(url("https://www.caffertylab.org/")),
  developers           := List(
    Developer(id = "CiaraOBrien", name = "Ciara O'Brien", 
              email = "ciaraobrienf@gmail.com", url = url("https://github.com/CiaraOBrien")),
  ),
  startYear            := Some(2021),
  licenses             := List("MIT" -> url("https://opensource.org/licenses/MIT")),
  scmInfo              := Some(ScmInfo(url("https://github.com/CiaraOBrien/phaser"), "scm:git@github.com:CiaraOBrien/phaser.git")),
  homepage             := Some(url("https://github.com/CiaraOBrien/phaser")),
  publishMavenStyle    := true,
  libraryDependencies ++= Seq(
     "org.typelevel" %%% "cats-core"    % "2.3.1",
  ),
  testFrameworks       += new TestFramework("minitest.runner.Framework"),
  parallelExecution    := false,
  scalacOptions    ++= Seq(
    "-source:3.1", "-indent", "-new-syntax",
    "-Yexplicit-nulls", "-Ycheck-init", "-Yerased-terms",
    "-language:strictEquality", 
  )
).jvmSettings(

).jsSettings(
  Compile / scalaJSUseMainModuleInitializer := true,
	jsEnv := new org.scalajs.jsenv.jsdomnodejs.JSDOMNodeJSEnv(),
)

val typerPhase    = "typer"
val macrosPhase   = "staging"
val erasurePhase  = "erasure"
val lastPhase     = "collectSuperCalls"
val bytecodePhase = "genBCode"
val printPhases   = Seq(macrosPhase, bytecodePhase)
val taste         = taskKey[Unit]("Clean and run \"tasty\"")

lazy val tasty = project.in(file("tasty"))
.dependsOn(phaser.jvm)
.settings(
  name := "tasty-playground",
  scalaVersion := "3.0.0-M3",
  Compile / scalaSource      := (ThisBuild / baseDirectory).value / "tasty",
  Test    / unmanagedSources := Nil,
  Compile / logBuffered      := true,
  Compile / scalacOptions    += ("-Xprint:" + printPhases.mkString(",")),
  Compile / taste := Def.sequential(
    phaser.jvm / Compile / compile,
    Compile / clean,
    (Compile / run).toTask("")
  ).value,
  publish := {}, publishLocal := {}, test := {}, doc := { file(".") }
)
