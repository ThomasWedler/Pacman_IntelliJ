import sbt._
import java.io.File
import Process._

class SirisLogger extends sbt.BasicLogger {
  def logAll(s: Seq[sbt.LogEvent]) { s.foreach(e => {
      e.getClass.getDeclaredMethods.find(_.getName == "msg") match {
        case Some(method) => println(method.invoke(e.asInstanceOf[AnyRef]))
        case _ =>
      }
    })}

  def success(s: => String) {}
  def log(l: sbt.Level.Value,s: => String) {}
  def control(ce: sbt.ControlEvent.Value,s: => String) {}
  def trace(t: => Throwable) {}
}

class SirisConfig(info: ProjectInfo) 
  extends DefaultProject(info)
  with AutoCompilerPlugins
{

  val logger = new SirisLogger()

  lazy val hi = task { println("Hello Siris World"); None }

  //Pacman documantation
  lazy val pdoc = task {


    val javaModules: PathFinder = (path(".") * "siris.javainterface")
    val javaSources = javaModules * "src" ** "*.java"
    val javaOutDir = new File( "pacman/doc/java" )
    if(javaOutDir.exists) ("rm -r " + javaOutDir.getAbsolutePath) ! logger
    javaOutDir.mkdir

    val javaDocCMD =
      "javadoc -d " +
      javaOutDir.getAbsolutePath + " " +
      javaSources.getFiles.map(_.getAbsolutePath).
        filter(!_.contains("""siris.javainterface/src/siris/pacman/graph/impl""")).
        mkString(" ")


    javaDocCMD ! logger





    val scalaModules: PathFinder = (path(".") * "siris.javainterface")
    val scalaSources = scalaModules * "src" ** "*.scala"


    temporaryDocPath = Some(path("pacman")/ "doc")
    temporaryDocDirName = Some("scala")

    if((temporaryDocPath.get / "scala").asFile.exists)
      ("rm -r " + (temporaryDocPath.get / "scala").asFile.getAbsolutePath) ! logger
    temporaryDocPath.get.asFile.mkdir

    docAction.run



    ("mv " + (temporaryDocPath.get / "main" / "api").asFile.getAbsolutePath +
     " " + (temporaryDocPath.get / "scala").asFile.getAbsolutePath) ! logger

    ("rm -r " + (temporaryDocPath.get / "main").asFile.getAbsolutePath) ! logger

    temporaryDocPath = None
    temporaryDocDirName = None

    None
  }


  override def docPath = temporaryDocPath match{
    case Some(p) => p
    case _ => super.docPath
  }

  override def docDirectoryName = temporaryDocDirName match{
      case Some(n) => n
      case _ => super.docDirectoryName
    }

  var temporaryDocOptions = Seq()
  var temporaryDocPath: Option[Path] = None
  var temporaryDocDirName: Option[String] = None

  lazy val dist = task {
    println( "Creating distribution" )
    val distDir = new File( "dist" )
    val logger = new ConsoleLogger
    if( distDir.exists ) {
      println( "Distribution directory exists, cleaning!" )
      FileUtilities.clean( sbt.Path.fromFile( distDir ) , logger )
    }
    println( "Creating distribution directory" )
    distDir.mkdir
    println( "Copying libraries" )
    FileUtilities.copyDirectory( sbt.Path.fromFile( new File("lib") ), sbt.Path.fromFile( distDir ) / "lib", logger)
    println( "Copying scala" )
    FileUtilities.copyDirectory( sbt.Path.fromFile( new File("../../scala/current") ), sbt.Path.fromFile( distDir ) / "lib" / "scala", logger)
    println( "Copying general models" )
    FileUtilities.copyDirectory( sbt.Path.fromFile( new File("models") ), sbt.Path.fromFile( distDir ) / "models", logger)
    println( "Copying pipeline shader" )
    FileUtilities.copyDirectory( sbt.Path.fromFile( new File("pipeline_shader") ), sbt.Path.fromFile( distDir ) / "pipeline_shader", logger)
    println( "Copying shader" )
    FileUtilities.copyDirectory( sbt.Path.fromFile( new File("shader") ), sbt.Path.fromFile( distDir ) / "shader", logger)
    println( "Copying content" )
    FileUtilities.copyDirectory( sbt.Path.fromFile( new File("simthief") ), sbt.Path.fromFile( distDir ) / "simthief", logger)
    println( "Copying start scripts" )
    FileUtilities.copyFile( sbt.Path.fromFile( new File("start-scripts") ) / "start_on_linux32.sh", sbt.Path.fromFile( distDir ) / "start_on_linux32.sh", logger)
    FileUtilities.copyFile( sbt.Path.fromFile( new File("start-scripts") ) / "start_on_mac.sh", sbt.Path.fromFile( distDir ) / "start_on_mac.sh", logger)
    FileUtilities.copyFile( sbt.Path.fromFile( new File("start-scripts") ) / "start_on_windows.bat", sbt.Path.fromFile( distDir ) / "start_on_windows.bat", logger)
    println( "Copying SIRIS jar file" )
    FileUtilities.copyFile( outputPath / defaultJarName, sbt.Path.fromFile( distDir ) / defaultJarName, logger)
    None
  } dependsOn( packageAction )

  override def mainClass = Some("siris.applications.simthief.SimThief_IEEE_VR")

  val cont = compilerPlugin("org.scala-lang.plugins" % "continuations" % "2.9.0-1")
  override def compileOptions = 
    super.compileOptions ++ 
      compileOptions("-P:continuations:enable") ++
      compileOptions("-unchecked")

  override def documentOptions =
    compileOptions.map(co => SimpleDocOption(co.asString)).toSeq ++
    temporaryDocOptions ++
    super.documentOptions
    //super.documentOptions ++
//      SimpleDocOption("-P:continuations:enable") ++
//      SimpleDocOption("-unchecked")

  val scalaToolsSnapshots = ScalaToolsSnapshots
  val scalatest = "org.scalatest" % "scalatest" % "1.2"
  val scalswing = "org.scala-lang" % "scala-swing" % "2.9.0-1"

  val modules: PathFinder = (path(".") * "siris.*") +++ (path(".") * "applications.*") +++ (path(".") * "ext.*")
  //Fixes the "Could not detrmine source of class" warns when compiling .java files
  override def mainSourceRoots = super.mainSourceRoots +++ ((modules * "src") ## )
  override def mainSources = modules * "src" ** "*.scala" +++ modules * "src" ** "*.java"
  override def mainResources: PathFinder = modules * "resource"
  override def testSources: PathFinder = modules * "test-src" ** "*.scala"
  override def testResources: PathFinder = modules * "test-resource"
  
  override def fork = forkRun(
      "-Xms1G" ::
      "-Xmx2G" ::
      "-Xss4M" ::
      "-Djava.library.path=lib/jogl2.0/macosx:lib/joal/:lib/lwjgl-2.6/native/macosx:lib/optix" ::
      Nil)

  override def unmanagedClasspath =
    super.unmanagedClasspath +++
    ("lib" / "jogl2.0" / "gluegen-rt.jar") +++
    ("lib" / "jogl2.0" / "jogl.all.jar") +++
    ("lib" / "jogl2.0" / "nativewindow.all.jar") +++
    ("lib" / "jogl2.0" / "newt.all.jar") +++
    ("lib" / "lwjgl-2.6" / "lwjgl.jar") +++
    ("lib" / "jbullet" / "jbullet.jar") +++
    ("lib" / "jbullet" / "vecmath.jar") +++
    ("lib" / "gestures" / "gracia_siris_embedded.jar") +++
    ("lib" / "unification" / "unificationModuleWithoutLibs.jar") +++
    ("lib" / "wiiusej" / "wiiusej.jar") +++
    ("lib" / "optix" / "OptixWrap.jar")

}