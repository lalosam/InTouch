import scala.util.Try

name := "InTouch"

version := "1.0"

scalaVersion := "2.11.8"

organization := "lalosam"
organizationName := "Lalosam"
startYear := Some(2015)
maintainer := "lalosam369@gmail.com"

resolvers += sbt.Resolver.bintrayRepo("denigma", "denigma-releases") //add resolver

val buildNumber = Try(sys.env("BUILD_NUMBER")).getOrElse("0000")

mainClass in Compile := Some("com.rojosam.InTouch")

enablePlugins(SbtNativePackager, JavaAppPackaging, DockerPlugin)

libraryDependencies ++=  {
  val AkkaVersion       = "2.4.8"
  val log4jVersion      = "2.6.2"
  val Json4sVersion     = "3.4.0"
  Seq(
    "org.scala-lang.modules"   %% "scala-xml"                         % "1.0.5",
    "com.typesafe"             %  "config"                            % "1.3.0",
    "com.typesafe.akka"        %% "akka-http-experimental"            % AkkaVersion,
    "com.typesafe.akka"        %% "akka-http-spray-json-experimental" % AkkaVersion,
    "com.typesafe.akka"        %% "akka-slf4j"                        % AkkaVersion,
    "org.denigma"              %% "akka-http-extensions"              % "0.0.13",
    "com.typesafe.akka"        %% "akka-remote"                       % AkkaVersion,
    "com.lihaoyi"              %% "scalatags"                         % "0.6.0",
    "org.json4s"               %% "json4s-native"                     % Json4sVersion,
    "org.apache.logging.log4j" %  "log4j-core"                        % log4jVersion,
    "org.apache.logging.log4j" %  "log4j-api"                         % log4jVersion,
    "org.apache.logging.log4j" %  "log4j-slf4j-impl"                  % log4jVersion,
    "org.apache.tomcat"        %  "tomcat-jdbc"                       % "8.5.4",
    "mysql"                    %  "mysql-connector-java"              % "8.0.12"
  )
}


assemblyJarName in assembly := s"${name.value}-${version.value}-${"%04d".format(buildNumber.toInt)}.jar"

assemblyMergeStrategy in assembly := {
  case PathList("META-INF", "MANIFEST.MF") => MergeStrategy.discard
  case "reference.conf" => MergeStrategy.concat
  case _ => MergeStrategy.first
}

dockerRepository := Some("lalosam")
dockerUpdateLatest := true
dockerBaseImage := "openjdk:8"
