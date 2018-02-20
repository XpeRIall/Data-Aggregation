lazy val commonSettings = Seq(
  organization := "com.opinov8",
  name := "anyware",
  version := "0.1.0",
  scalaVersion := "2.12.4",

)
libraryDependencies ++= Seq(
  "com.github.tototoshi" %% "slick-joda-mapper" % "2.3.0",
  "com.github.tototoshi" %% "scala-csv" % "1.3.5",
  "joda-time" % "joda-time" % "2.9.9",
  "org.joda" % "joda-convert" % "1.8.3",
  "com.typesafe.play" %% "play-json-joda" % "2.6.5",
  "com.typesafe.play" %% "play-ahc-ws-standalone" % "1.1.3",
  "com.typesafe.play" %% "play-ws-standalone-xml" % "1.1.3",
  "com.typesafe.play" %% "play-ws-standalone-json" % "1.1.3",

)
libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.5.9",
  "com.typesafe.akka" %% "akka-testkit" % "2.5.9" % Test
)
libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-http" % "10.0.11",
  "com.typesafe.akka" %% "akka-http-testkit" % "10.0.11" % Test
)
