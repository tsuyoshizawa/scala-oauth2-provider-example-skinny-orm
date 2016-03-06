lazy val root = (project in file("."))
.enablePlugins(PlayScala)
.settings(
  scalaVersion := "2.11.7",
  scalacOptions := Seq("-language:_", "-deprecation", "-unchecked", "-feature", "-Xlint"),
  transitiveClassifiers in Global := Seq(Artifact.SourceClassifier),
  sources in (Compile, doc) := Nil,
  publishArtifact in (Compile, packageDoc) := false,
  parallelExecution in Test := false
).settings(
  resolvers += Resolver.file(
    "local-ivy-repos", file(Path.userHome + "/.ivy2/local")
  )(Resolver.ivyStylePatterns),
  libraryDependencies ++= Seq(
    jdbc,
    evolutions,
    "org.skinny-framework" %% "skinny-orm" % "2.0.7",
    "org.scalikejdbc" %% "scalikejdbc-play-dbapi-adapter" % "2.5.0",
    "com.nulab-inc" %% "play2-oauth2-provider" % "0.17.0"
  )
)

routesGenerator := InjectedRoutesGenerator
