import com.typesafe.sbt.SbtNativePackager._
import NativePackagerKeys._

name := "ebay-snipe-server"

organization := "net.ruippeixotog"

version := "0.1-SNAPSHOT"

scalaVersion := "2.11.1"

resolvers += "Spray repository" at "http://repo.spray.io"

libraryDependencies ++= Seq(
  "com.googlecode.json-simple"  % "json-simple"     % "1.1",
  "org.jsoup"                   % "jsoup"           % "1.7.1",
  "ch.qos.logback"              % "logback-classic" % "1.1.2",
  "com.github.nscala-time"     %% "nscala-time"     % "1.2.0",
  "com.typesafe"                % "config"          % "1.2.1",
  "com.typesafe.akka"          %% "akka-actor"      % "2.3.2",
  "com.typesafe.akka"          %% "akka-slf4j"      % "2.3.2",
  "io.spray"                   %% "spray-can"       % "1.3.1",
  "io.spray"                   %% "spray-json"      % "1.2.6",
  "io.spray"                   %% "spray-routing"   % "1.3.1")

packageArchetype.java_server

sources in (Compile, doc) := Nil

// the resources to provide in the conf folder instead of inside the JAR file
val confResources = Seq("application.conf", "logback.xml")

// copy the confResources to the conf folder...
mappings in Universal <++= (resourceDirectory in Compile) map { resDir =>
  confResources.map { resName => resDir / resName -> ("conf/" + resName) }
}

// ...and do not include them inside the JAR
mappings in (Compile, packageBin) ~= { _.filterNot {
  case (s, name) => confResources.contains(name)
}}

// include the conf folder in the classpath when the start script is executed
scriptClasspath += "../conf"
