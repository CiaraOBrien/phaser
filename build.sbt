val printPhases = settingKey[Seq[String]]("The phases after which to print the tree")
val playgroundFilter = settingKey[FileFilter]("The files to print")

Global / onChangedBuildSource := ReloadOnSourceChanges

lazy val commonSettings = Seq(
  name := "phaser",
  version := "0.1.0",

  scalaVersion := "3.0.0-M3",
  scalacOptions ++= Seq(
		"-source:3.1", "-indent", "-new-syntax",
		"-Yexplicit-nulls", "-Ycheck-init", "-Yerased-terms",
    "-language:strictEquality", 
	),
  libraryDependencies += "io.monix" %% "minitest" % "2.8.2-5ebd81f-SNAPSHOT",
  testFrameworks += new TestFramework("minitest.runner.Framework"),
)

lazy val root = project.in(file("."))
  .dependsOn(phaser, coerce, phaserBank)
  .settings(commonSettings,
    name := "phaser",
    commands += Command.command("code") { state =>
			"codeGen / Compile / clean" ::
    	"codeGen / Compile / run"   :: 
      state
  	},
  )

lazy val phaser = project.in(file("core"))
  .settings(commonSettings,
    name := "phaser-core",
  )

lazy val coerce = project.in(file("coerce"))
  .dependsOn(phaser)
  .settings(commonSettings,
    name := "phaser-coerce",
  )

lazy val phaserBank = project.in(file("phaserBank"))
  .dependsOn(phaser, coerce)
  .settings(commonSettings,
    name := "phaser-bank",
  )

val typerPhase    = "typer"
val macrosPhase   = "staging"
val erasurePhase  = "erasure"
val lastPhase     = "collectSuperCalls"
val bytecodePhase = "genBCode"
lazy val codeGen = project.in(file("codeGen"))
  .dependsOn(phaser, coerce, phaserBank)
  .settings(commonSettings,
    Compile / managedSourceDirectories := Seq(baseDirectory.value),
    publish := {}, publishLocal := {},
		printPhases := Seq(macrosPhase),
		logBuffered := true,
    scalacOptions ++= Seq("-Xprint:" + printPhases.value.mkString(","))
  )