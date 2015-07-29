package com.mesosphere.mesos.framework

import net.virtualvoid.sbt.graph.Plugin._
import sbtassembly.Plugin._
import sbt.Keys._
import sbt._

object AdditionFrameworkBuild extends Build {

  lazy val projectScalaVersion = "2.11.6"
  lazy val projectVersion = "0.0.1-SNAPSHOT"

  object V {
    val logback = "1.1.2"
    val mockito = "1.9.5"
    val slf4j = "1.7.7"
    val scalaTest = "2.2.1"
    val scalaCheck = "1.10.0"

    val mesos = "0.22.1"

  }

  object Deps {
    val mesos = Seq(
      "org.apache.mesos"  % "mesos"  % V.mesos
    )
  }

  val extraSettings = Defaults.coreDefaultSettings ++ graphSettings

  val sharedSettings = extraSettings ++ Seq(
    organization := "com.mesosphere.mesos.framework",
    scalaVersion := projectScalaVersion,
    version := projectVersion,

    resolvers ++= Seq(
      "mesosphere-release" at "http://nexus.msphere.co/nexus/content/repositories/mesosphere-release",
      "mesosphere-snapshot" at "http://nexus.msphere.co/nexus/content/repositories/mesosphere-snapshot"
    ),

    libraryDependencies ++= Seq(
      "org.slf4j"           %  "slf4j-api"        % V.slf4j,
      // unify ALL logging over slf4j
      "org.slf4j"           % "jul-to-slf4j"      % V.slf4j,
      "org.slf4j"           % "jcl-over-slf4j"    % V.slf4j,
      "org.slf4j"           % "log4j-over-slf4j"  % V.slf4j,
      "ch.qos.logback"      %  "logback-classic"  % V.logback,
      "org.scalatest"       %% "scalatest"        % V.scalaTest     % "test",
      "org.mockito"         %  "mockito-all"      % V.mockito       % "test",
      "org.scalacheck"      %% "scalacheck"       % V.scalaCheck    % "test"
    ),

    javacOptions in Compile ++= Seq(
      "-source", "1.7",
      "-target", "1.7",
      "-Xlint:unchecked",
      "-Xlint:deprecation"
    ),

    scalacOptions ++= Seq(
      "-unchecked",
      "-deprecation",
      "-Xlint",
      "-Yresolve-term-conflict:package",
      "-target:jvm-1.7",
      "-encoding", "UTF-8"
    ),

    scalacOptions in Compile ++= Seq(
      "-deprecation", // Emit warning and location for usages of deprecated APIs.
      "-feature",  // Emit warning and location for usages of features that should be imported explicitly.
      "-unchecked", // Enable additional warnings where generated code depends on assumptions.
      "-Xlint", // Enable recommended additional warnings.
      "-Ywarn-adapted-args", // Warn if an argument list is modified to match the receiver.
      "-Ywarn-dead-code"
    ),

    scalacOptions in Test ~= { (options: Seq[String]) =>
      options.filterNot(_ == "-Ywarn-dead-code" /* to fix warnings due to Mockito */)
    },

    // Publishing options:
    publishMavenStyle := true,

    pomIncludeRepository := { x => false },

    publishArtifact in Test := false,

    parallelExecution in ThisBuild := false,

    parallelExecution in Test := false,

    // Enable forking (see sbt docs) because our full build (including tests) uses many threads.
    fork := true
  )

  lazy val root = Project(
    id = "addition-framework",
    base = file("."),
    settings = sharedSettings ++ assemblySettings
  ).settings(
    libraryDependencies ++=
      Deps.mesos
  )

  //////////////////////////////////////////////////////////////////////////////
  // BUILD TASKS
  //////////////////////////////////////////////////////////////////////////////

  sys.env.get("TEAMCITY_VERSION") match {
    case None => // no-op
    case Some(teamcityVersion) =>
      // add some info into the teamcity build context so that they can be used
      // by later steps
      reportParameter("SCALA_VERSION", projectScalaVersion)
      reportParameter("PROJECT_VERSION", projectVersion)
  }

  def reportParameter(key: String, value: String): Unit = {
    println(s"##teamcity[setParameter name='env.SBT_$key' value='$value']")
    println(s"##teamcity[setParameter name='system.sbt.$key' value='$value']")
  }
}
