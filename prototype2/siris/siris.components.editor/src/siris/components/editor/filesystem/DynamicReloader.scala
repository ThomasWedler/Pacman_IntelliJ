/*
 * Created by IntelliJ IDEA.
 * User: martin
 * Date: 3/5/11
 * Time: 10:35 AM
 */
package siris.components.editor.filesystem

import tools.nsc.util.ScalaClassLoader.URLClassLoader
import util.matching.Regex
import siris.components.editor.tests.MyBaseClass
import java.beans.XMLEncoder
import simplex3d.math.floatm.ConstMat4f
import siris.core.entity.description.SVal
import siris.ontology.SVarDescription
import java.io.{IOException, FileOutputStream, FileWriter, File}
import tools.nsc.Properties
import siris.core.helper.Loggable

case class ClassFile(file: File, className: String)
case class CompilerSettings(outputPath: File, additionalClassPaths: List[File], additionalSourceFiles: List[File])

class DynamicReloader[T] (val classFile: ClassFile, compilerSettings: CompilerSettings, classTemplate: Option[String], var onLoad: (Option[T]) => Unit, pollingIntervalInMillis: Long)
extends Loggable
{

  def this (classFile: ClassFile, compilerSettings: CompilerSettings, classTemplate: Option[String], onLoad: (Option[T]) => Unit) =
    this(classFile, compilerSettings, classTemplate, onLoad, 1000L)

  protected var currentClass: Option[T] = None
  private lazy val fileMonitor = new FileMonitor(pollingIntervalInMillis)

  init

  def getCurrentClass: Option[T] = currentClass

  protected def init() {
    classTemplate.collect{
      case template =>
        val fw = new FileWriter(classFile.file)
        fw.write(template)
        fw.close
    }

    if(!classFile.file.exists) throw new java.io.FileNotFoundException( this.toString + " could not find the file '" + classFile.file.getCanonicalPath + "'.")

    fileMonitor.addFile(classFile.file)
    fileMonitor.addListener(new FileListener{
      def fileChanged(p1: File) = {
        emit("File " + p1.getName + " changed. Recompiling and loading ..." )
        load.collect{
          case clazz =>
            currentClass = Some(clazz)
            onLoad(currentClass)
        }

      }
    })


    emit("Initially compiling and loading '" + classFile.file.getName + "'..." )
    currentClass = load
    onLoad(currentClass)
  }

  private def load(): Option[T] =  {
     //-make:transitive -dependencyfile ${build.dir}/.scala_dependencies
    val depFile = new File(compilerSettings.outputPath, ".scala_dependencies")

    val args =  /*"-make:changed" ::  "-make:transitive" :: "-dependencyfile" :: depFile.getAbsolutePath ::*/ "-d" :: compilerSettings.outputPath.getCanonicalPath ::
                /*"-deprecation" ::*/ "-classpath" :: compilerSettings.additionalClassPaths.foldLeft(DynamicReloader.jdklibs)(_ + DynamicReloader.pathSeperator + _.getCanonicalPath) ::
                compilerSettings.additionalSourceFiles.map(_.getCanonicalPath) ::: classFile.file.getCanonicalPath :: Nil

    //scala.tools.nsc.Main.process(args.toArray)
    var success = false
    var trys = 0
    val timeToWait = 1000L

    while((success == false) && (trys < 3)){
      try{
        if(!scala.tools.nsc.CompileClient.process(args.toArray)){
          emit("Compiled with errors.")
          return None
        }
        success = true
      }
      catch {
        case e =>
          trys = trys + 1
          emit(e.toString)
          emit("Waiting " + (timeToWait.toFloat / 1000.0f) + "s before retrying.")
          Thread.sleep(timeToWait)
      }
    }

//    if (scala.tools.nsc.Main.reporter.hasErrors) {
//      if(DynamicReloader.verbose) println("Compiled with errors.")
//      return None
//    }

    //val className: String = "siris.components.editor.MyLabel"
    val bytecodeDirectory: File = new File(compilerSettings.outputPath.getCanonicalPath)

    val classLoader = new URLClassLoader( Array( bytecodeDirectory.toURI.toURL ), this.getClass.getClassLoader )

    try {
      val clazz = classLoader.loadClass( classFile.className )
      val loadedObject: T = clazz.asInstanceOf[Class[T]].newInstance
      emit("successful.")
      Some(loadedObject)
    } catch {
      case e =>
        emit("Error: \n" + e)
        None
    }
  }

  def showInIDE {

    val app = if((System.getProperty("os.name") == "Linux")) "idea" else if ( (System.getProperty("os.name") == "Windows 7")) "proton" else "open"
    val cmd: List[String] = app :: classFile.file.getCanonicalPath :: Nil
    //println(cmd)
    var child = Runtime.getRuntime().exec(cmd.toArray);
    child.waitFor
    child = Runtime.getRuntime().exec(cmd.toArray);
    child.waitFor
  }

  def emit(s: String): Unit = if(DynamicReloader.emitToLog) info(s) else println(s)

}

object DynamicReloader {

  System.setProperty("scala.home", new File("../../scala/current").getAbsolutePath)

  val emitToLog = false

  val pathSeperator = if(System.getProperty("os.name") == "Windows 7") ";" else ":"

  val jdklibs = if(System.getProperty("os.name") == "Windows 7")
                  new File(System.getProperty("java.home") + "/../src.zip").getCanonicalPath
                else
                  findFiles(new File(System.getProperty("java.home") + "/../Classes"), """.*\.jar""".r).mkString(":")
      //Not so safe //foldLeft[String]("")(_ + ":" + _.getCanonicalPath).substring(1)

  def findFiles(baseDir: File, r: Regex): List[File] = {
    //println(baseDir)
    val thisDir = baseDir.listFiles.toList
    thisDir.filter(
      (f: File) => {
        (!f.isDirectory) && (r.findFirstIn(f.getName).isDefined)
      }) ::: thisDir.filter(_.isDirectory).flatMap(findFiles(_,r))
  }

  def main(args: Array[String]) {

//    println(Ontology.transform.typeinfo.name)
//    println((OntologyJVR.initialPosition <= ConstMat4f(0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0)).asInstanceOf[SVal[_]].typedSemantics.typeinfo.name)
//    println(SVarDescription.apply(Symbol("Ontology:vector3")).typeinfo.erasure.getCanonicalName)

    System.exit(0)

    val scalalibs = new File("../../scala/current/lib/scala-library.jar") :: new File("../../scala/current/lib/scala-swing.jar") :: Nil
    val interface = new File("siris.components.editor/src/siris/components/editor/tests/MyBaseClass.scala") :: Nil

    val dr = new DynamicReloader[MyBaseClass](
      ClassFile(new File("/Users/martin/Temp/dcr/MyClass.scala"), "simple.MyClass"),
      CompilerSettings(new File("/Users/martin/Temp/dcr"), scalalibs, interface),
      Some(ClassTemplateTest.get),
      (c: Option[MyBaseClass]) => {c.collect{case v => v.print}},
      1000L)

    //Wait and see
  }

}

object ClassTemplateTest {

  def get: String =

"""
package simple

import siris.components.editor.tests.MyBaseClass

class MyClass extends MyBaseClass {

  def print(): Unit = {
    println("Hello World")
  }
}
"""
}