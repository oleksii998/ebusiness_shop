name := """untitled8"""
organization := "com.example"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)
resolvers += Resolver.jcenterRepo
resolvers += "Akka Snapshot Repository" at "https://repo.akka.io/snapshots/"

scalaVersion := "2.12.8"

libraryDependencies += ehcache
libraryDependencies += ws
libraryDependencies += specs2 % Test
libraryDependencies += guice
libraryDependencies += "org.scalatestplus.play" %% "scalatestplus-play" % "5.0.0" % Test
libraryDependencies += "com.typesafe.play" %% "play-slick" % "4.0.0"
libraryDependencies += "com.typesafe.play" %% "play-slick-evolutions" % "4.0.0"
libraryDependencies += "org.xerial" % "sqlite-jdbc" % "3.30.1"
libraryDependencies += "com.iheart" %% "ficus" % "1.4.7"
libraryDependencies += "com.mohiva" %% "play-silhouette" % "7.0.0"
libraryDependencies += "com.mohiva" %% "play-silhouette-password-bcrypt" % "7.0.0"
libraryDependencies += "com.mohiva" %% "play-silhouette-persistence" % "7.0.0"
libraryDependencies += "com.mohiva" %% "play-silhouette-crypto-jca" % "7.0.0"
libraryDependencies += "com.mohiva" %% "play-silhouette-totp" % "7.0.0"
libraryDependencies += "net.codingwell" %% "scala-guice" % "4.2.6"

// Adds additional packages into Twirl
//TwirlKeys.templateImports += "com.example.controllers._"

// Adds additional packages into conf/routes
// play.sbt.routes.RoutesKeys.routesImport += "com.example.binders._"
