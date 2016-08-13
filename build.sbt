name := "InTouch"

version := "1.0"

scalaVersion := "2.11.8"

resolvers += sbt.Resolver.bintrayRepo("denigma", "denigma-releases") //add resolver

libraryDependencies ++=  {
  val AkkaVersion       = "2.4.8"
  val log4jVersion      = "2.6.2"
  val Json4sVersion     = "3.4.0"
  Seq(
    "com.h2database"           %  "h2"                                % "1.4.192",
    "org.apache.derby"         %  "derby"                             % "10.12.1.1",
    "com.typesafe.slick"       %% "slick"                             % "3.1.1",
    "com.typesafe"             %  "config"                            % "1.3.0",
    "com.typesafe.akka"        %% "akka-http-experimental"            % AkkaVersion,
    "com.typesafe.akka"        %% "akka-http-spray-json-experimental" % AkkaVersion,
    "com.typesafe.akka"        %% "akka-slf4j"                        % AkkaVersion,
    "org.denigma"              %% "akka-http-extensions"              % "0.0.13",
    "com.lihaoyi"              %% "scalatags"                         % "0.6.0",
    "org.json4s"               %% "json4s-jackson"                    % Json4sVersion,
    "org.apache.logging.log4j" %  "log4j-core"                        % log4jVersion,
    "org.apache.logging.log4j" %  "log4j-api"                         % log4jVersion,
    "org.apache.logging.log4j" %  "log4j-slf4j-impl"                  % log4jVersion
  )
}