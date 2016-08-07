name := "InTouch"

version := "1.0"

scalaVersion := "2.11.8"

resolvers += sbt.Resolver.bintrayRepo("denigma", "denigma-releases") //add resolver

libraryDependencies ++=  {
  val AkkaVersion       = "2.4.8"
  // val AkkaHttpVersion   = "2.0.1"
  // val Json4sVersion     = "3.2.11"
  Seq(
  "org.apache.derby"     %  "derby"                             % "10.12.1.1",
  "com.typesafe.slick"   %% "slick"                             % "3.1.1",
  "com.typesafe"         %  "config"                            % "1.3.0",
  "com.typesafe.akka"    %% "akka-http-experimental"            % AkkaVersion,
  "com.typesafe.akka"    %% "akka-http-spray-json-experimental" % AkkaVersion,
  "com.typesafe.akka"    %% "akka-slf4j"                        % AkkaVersion,
  "org.denigma"          %% "akka-http-extensions"              % "0.0.13",
  "com.lihaoyi"          %% "scalatags"                         % "0.6.0"
)
}