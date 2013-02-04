import com.typesafe.sbt.SbtStartScript

seq(SbtStartScript.startScriptForClassesSettings: _*)

name := "hello"

version := "1.0"

scalaVersion := "2.9.2"

resolvers += "twitter-repo" at "http://maven.twttr.com"

libraryDependencies ++= Seq(
	"com.twitter" % "finagle-core" % "1.9.0", // finagle core http://twitter.github.com/finagle
	"com.twitter" % "finagle-http" % "1.9.0", // finagle http library http://twitter.github.com/finagle
	"org.json4s" %% "json4s-jackson" % "3.1.0", // json4s for JSON http://json4s.org
	"org.squeryl" %% "squeryl" % "0.9.5-2", // Squeryl for db ORM http://squeryl.org
	"c3p0" % "c3p0" % "0.9.1.2" // c3p0 for db connection pooling http://www.mchange.com/projects/c3p0
	)