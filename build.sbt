lazy val root = (project in file("."))
.enablePlugins(PlayScala)
.settings(
  scalaVersion := "2.11.6",
  scalacOptions := Seq("-language:_", "-deprecation", "-unchecked", "-feature", "-Xlint"),
  transitiveClassifiers in Global := Seq(Artifact.SourceClassifier),
  sources in (Compile, doc) := Nil,
  publishArtifact in (Compile, packageDoc) := false,
  parallelExecution in Test := false
).settings(
  libraryDependencies ++= Seq(
    jdbc,
    "org.skinny-framework" %% "skinny-orm" % "1.3.18",
    "org.scalikejdbc" %% "scalikejdbc-play-dbplugin-adapter" % "2.3.6",
    "com.nulab-inc" %% "play2-oauth2-provider" % "0.13.3"
  )
)
