import scalariform.formatter.preferences._

name := "ebay-snipe-server"

organization := "net.ruippeixotog"

version := "0.2-SNAPSHOT"

scalaVersion := "2.11.7"

resolvers ++= Seq(
  "Spray repository" at "http://repo.spray.io",
  Resolver.sonatypeRepo("snapshots"))

libraryDependencies ++= Seq(
  "com.github.nscala-time"     %% "nscala-time"     % "2.10.0",
  "com.typesafe"                % "config"          % "1.3.0",
  "com.typesafe.akka"          %% "akka-actor"      % "2.4.2",
  "com.typesafe.akka"          %% "akka-slf4j"      % "2.4.2",
  "io.spray"                   %% "spray-can"       % "1.3.3",
  "io.spray"                   %% "spray-json"      % "1.3.2",
  "io.spray"                   %% "spray-routing"   % "1.3.3",
  "net.ruippeixotog"           %% "scala-scraper"   % "0.1.2",
  "ch.qos.logback"              % "logback-classic" % "1.1.5"            % "runtime")

scalariformPreferences := scalariformPreferences.value
  .setPreference(DanglingCloseParenthesis, Prevent)
  .setPreference(DoubleIndentClassDeclaration, true)

scalacOptions ++= Seq(
  "-deprecation",
  "-unchecked",
  "-feature",
  "-language:implicitConversions")

// -- general packaging settings --

enablePlugins(JavaServerAppPackaging)

mainClass in Compile := Some("net.ruippeixotog.ebaysniper.SnipeServer")

sources in (Compile, doc) := Nil

// the resources to provide in the conf folder instead of inside the JAR file
val confResources = Seq("application.conf", "logback.xml")

// copy the confResources to the conf folder...
mappings in Universal <++= (resourceDirectory in Compile) map { resDir =>
  confResources.flatMap { resName =>
    val resFile = resDir / resName
    if(resFile.exists) Some(resFile -> ("conf/" + resName)) else None
  }
}

// ...and do not include them inside the JAR
mappings in (Compile, packageBin) ~= { _.filterNot {
  case (_, resName) => confResources.contains(resName)
}}

// include the conf folder in the classpath when the start script is executed
scriptClasspath += "../conf"

// -- Docker packaging settings --

maintainer in Docker := "Rui Gonçalves <ruippeixotog@gmail.com>"

daemonUser in Docker := "root" // the server must be able to write to mounted volumes

dockerExposedPorts in Docker := Seq(3647)

dockerExposedVolumes in Docker := Seq("/opt/docker/appdata", "/opt/docker/logs")

dockerRepository := Some("ruippeixotog")
