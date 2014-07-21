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
  "io.spray"                   %% "spray-can"       % "1.3.1-20140423",
  "io.spray"                   %% "spray-json"      % "1.2.6",
  "io.spray"                   %% "spray-routing"   % "1.3.1-20140423")

packageArchetype.java_server

sources in (Compile, doc) := Nil

mappings in Universal ++= Seq(
  file("src/main/resources/application.conf") -> "conf/application.conf",
  file("src/main/resources/logback.xml") -> "conf/logback.xml")

mappings in (Compile, packageBin) ~= { _.filterNot { case (_, name) =>
  Seq("application.conf", "logback.xml").contains(name)
}}

scriptClasspath += "../conf"
