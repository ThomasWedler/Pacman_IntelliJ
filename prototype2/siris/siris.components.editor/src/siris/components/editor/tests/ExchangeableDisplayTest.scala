package siris.components.editor.tests

import siris.components.editor.filesystem.FileListener
import scala.swing._
import event.{TreePathSelected, ValueChanged, ButtonClicked, SelectionChanged}
import scala.swing.Tree._
import java.awt.Color._
import java.awt.{Color, Graphics2D, Dimension}
import tools.nsc.util.ScalaClassLoader.URLClassLoader
import util.matching.Regex
import javax.swing.event.TreeSelectionListener
import scala.collection.mutable.{Map, SynchronizedMap, HashMap}
import javax.swing.{JComponent, InputVerifier, Icon, ImageIcon}
import java.io.{FileWriter, File}
import xml.{MetaData, Elem}
import siris.components.editor.filesystem._

/**
 * Created by IntelliJ IDEA.
 * User: martin
 * Date: 1/31/11
 * Time: 2:32 PM
 * To change this template use File | Settings | File Templates.
 */

object ExchangeableDisplayTest {

  val baseDir = new File(System.getProperty("user.home")+ "/exchangeableDisplayTest")
  if(!baseDir.exists) baseDir.mkdirs

  val defaultFCG = new FileComponentGenerator{ def generate(f: File) = new Label(f.getName)}

  //Extension -> Component
  val displayMap: Map[String, (File, FileComponentGenerator)] =
    new HashMap[String, (File, FileComponentGenerator)] with SynchronizedMap[String, (File, FileComponentGenerator)] {}

  def main( args : Array[String]) : Unit = {

    val tree = new Tree[File] {

      treeData = TreeModel(new File(System.getProperty("user.home"))) {
        f =>
          if (f.isDirectory) f.listFiles.toSeq.filter((f: File) => {if(f.getName.startsWith(".")) false else true}) else Seq()
      }

      peer.setRowHeight(23)
      val me = this

      peer.getSelectionModel.addTreeSelectionListener(new TreeSelectionListener {
        def valueChanged(e: javax.swing.event.TreeSelectionEvent) {
          val (newPath, oldPath) = e.getPaths.map(treePathToPath).toList.partition(e.isAddedPath(_))
          me.publish(new TreePathSelected(me, newPath, oldPath, Option(e.getNewLeadSelectionPath: Path[File]), Option(e.getOldLeadSelectionPath: Path[File])))
        }
      })

      renderer = new Renderer[File]{
        def componentFor(tree: Tree[_], value: File, cellInfo: companion.CellInfo): Component = {

          val posOfDot = value.getName.indexOf('.')
          val extension = if(posOfDot >= 0) value.getName.substring(posOfDot) else ""

          extension match {
            case  _ =>
              new Label(value.getName) {
                icon = new ImageIcon("models/editor/icons/Spots.png")
                background = Color.lightGray
                if(cellInfo.asInstanceOf[Renderer.CellInfo].isSelected) opaque = true
              }
          }
        }
      }
    }


    val p = new ScrollPane(new Label()){}

    val scrollPane = new ScrollPane(tree)

    //val bbb = displayMap.keySet.toSeq
    val cb = new MyCB[String](("No data available." :: Nil).toSeq)
    cb.enabled = false

    val addButton = new Button("Add new extension")


    val tf = new TextField(){
      val v = (s: String) => {
        //println("""\.[a-z]+""".r.findFirstIn(s).isDefined)
        """\.[a-z]+""".r.findFirstIn(s).isDefined
      }



      peer.setInputVerifier(new InputVerifier {
        private val old = peer.getInputVerifier

        def verify(c: JComponent) = v(text)

        override def shouldYieldFocus(c: JComponent) = {
          verify(c) match {
            case false => background = Color.red; false
            case true => background = Color.white; true
          }
        }
      })

    }

    def updateCB(box: ComboBox[String], extension: String) {
      if (box.enabled == false) {
        box.enabled = true
        box.peer.setModel(ComboBox.newConstantModel((extension :: Nil).toSeq))
      }
      else {
        box.peer.setModel(ComboBox.newConstantModel(cb.getItems.union((extension :: Nil).toSeq)))
      }
    }

    val options = new MainFrame{
      title = "Options"
      size = new Dimension(300, 200)
      minimumSize = new Dimension(300, 200)

      contents = new GridBagPanel() {
        val gbc = new Constraints()
        gbc.fill = GridBagPanel.Fill.Both
        gbc.gridy = 0
        gbc.gridx = 0
        gbc.weighty = 0.0
        gbc.weightx = 1.0
        add(cb, gbc)
        gbc.gridy = 1
        gbc.gridx = 0
        gbc.weighty = 0.0
        gbc.weightx = 1.0
        add(tf, gbc)
        gbc.gridy = 2
        gbc.gridx = 0
        gbc.weighty = 1.0
        gbc.weightx = 1.0
        add(addButton, gbc)
        border = Swing.EmptyBorder(10, 10, 10, 10)
      }

      listenTo(addButton)
      listenTo(cb)

      reactions += {
        case SelectionChanged(box) => {
          if(box == cb) {
            displayMap.get(cb.item).collect({
              case (f, fcg) => openCode(f)
            })

          }
        }
      }

      reactions += {
        case bc: ButtonClicked => {
          if(tf.verifier.apply("")) {
            val newFile = new File( baseDir.getCanonicalPath + "/" + tf.text.substring(1).toLowerCase + "FileComponentGenerator.scala")
            if(!newFile.exists) {
              val fw = new FileWriter(newFile)
              fw.write(generateClassTemplate(tf.text.substring(1).toLowerCase + "FileComponentGenerator"))
              fw.close

              val fm = new FileMonitor(1000L)
              fm.addFile(newFile)
              fm.addListener(new FileListener{
                def fileChanged(p1: File) = {
                  println("File " + p1.getName + " changed. Recompiling and loading ..." )
                  val key = tf.text.toLowerCase
                  displayMap.update(key, (newFile, loadFCG(newFile, "siris.components.editor.tests." + key.substring(1) + "FileComponentGenerator")))
                }
              })

            }

            displayMap += tf.text.toLowerCase -> (newFile, defaultFCG)

            updateCB(cb, tf.text.toLowerCase)
//            if(cb.enabled == false) {
//              cb.enabled = true
//              cb.peer.setModel(ComboBox.newConstantModel((tf.text.toLowerCase :: Nil).toSeq) )
//            }
//            else {
//              cb.peer.setModel(ComboBox.newConstantModel(cb.getItems.union((tf.text.toLowerCase :: Nil).toSeq) ))
//            }


            openCode(newFile)
          }
        }
      }

      override def closeOperation() {
        println("Saving configuration to: " + (new File( baseDir.getCanonicalPath + "/config.xml")).getCanonicalPath)
        if(cb.enabled == true) saveTo(new File( baseDir.getCanonicalPath + "/config.xml"))
      }
    }

    val mf = new MainFrame {
      title = "Browser"
      contents = new GridBagPanel() {
        val gbc = new Constraints()
        gbc.fill = GridBagPanel.Fill.Both
        gbc.gridy = 0
        gbc.gridx = 0
        gbc.weighty = 1.0
        gbc.weightx = 1.0
        add(scrollPane, gbc)
        gbc.gridy = 0
        gbc.gridx = 1
        gbc.weighty = 1.0
        gbc.weightx = 1.0
        add(p, gbc)
        border = Swing.EmptyBorder(10, 10, 10, 10)
      }

      val me = this

      menuBar = new MenuBar{
        contents += new Menu("File") {
          contents += new MenuItem(new Action("Options"){
            def apply() = {
              val loc = me.locationOnScreen
              loc.translate(50, 50)
              options.location = loc
              options.visible = !options.visible
            }
          })
          contents += new MenuItem(new Action("Exit") {
            def apply() = System.exit(0)
          })
        }
      }

      listenTo(tree)

      reactions += {
        case e: TreePathSelected[_] => {
          try{
            val selectedFile = e.newPaths.head.reverse.head.asInstanceOf[File]
            val posOfDot = selectedFile.getName.indexOf('.')
            val extension = if (posOfDot >= 0) selectedFile.getName.substring(posOfDot) else ""

            p.contents = (displayMap.get(extension) match {
              case Some(v) => v._2
              case None => defaultFCG
            }).generate(selectedFile)
            }
          catch {
            case _ => {}
          }
        }
      }

      override def closeOperation() {
        println("Saving configuration to: " + (new File( baseDir.getCanonicalPath + "/config.xml")).getCanonicalPath)
        if(cb.enabled == true) saveTo(new File( baseDir.getCanonicalPath + "/config.xml"))
        System.exit(0)
      }
    }




    val configFile = new File( baseDir.getCanonicalPath + "/config.xml")
    if (configFile.exists) {
      loadFrom(configFile)
      displayMap.foreach(
        (entry) => {
          updateCB(cb, entry._1)
        }
      )
    }

    mf.size = new Dimension(800, 600)
    mf.minimumSize = new Dimension(800, 600)
    mf.visible = true

    //findFiles(new File(System.getProperty("java.home") + "/lib"), """.*\.jar""".r).foreach(println)

//    Files.walkfiletree()
//    println(System.getProperty("java.home"))
//    val c = new File(System.getProperty("user.home"))

  }

  val jdklibs = findFiles(new File(System.getProperty("java.home") + "/../Classes"), """.*\.jar""".r).
      foldRight[String]("")(_.getCanonicalPath + ":" + _)

  val scalalibs = "../../scala/current/lib/scala-library.jar:../../scala/current/lib/scala-swing.jar"
  val interface = "siris.components.editor/src/siris/components/editor/tests/FileComponentGenerator.scala"

  def loadFCG(f: File, className: String): FileComponentGenerator =  {
    val args = ("-d" :: baseDir.getCanonicalPath :: "-classpath" :: (jdklibs + scalalibs) :: interface :: f.getCanonicalPath :: Nil).toArray
    scala.tools.nsc.Main.process(args)

    if (scala.tools.nsc.Main.reporter.hasErrors) {
      println("Compiled with errors. Using default fallback.")
      return defaultFCG
    }

    //val className: String = "siris.components.editor.MyLabel"
    val bytecodeDirectory: File = new File(baseDir.getCanonicalPath)

    val classLoader = new URLClassLoader( Array( bytecodeDirectory.toURI.toURL ), this.getClass.getClassLoader )
    val clazz = classLoader.loadClass( className )

//    // Instantiate the servlet object
//    val ccc : List[Class[_]] = (classOf[File] :: Nil)
//    val ddd = ccc.toArray
//
//    val label: Label = clazz.asInstanceOf[Class[Label]].getConstructor(ddd:_*).newInstance((f :: Nil).toArray:_*)

    val fcg: FileComponentGenerator = clazz.asInstanceOf[Class[FileComponentGenerator]].newInstance
    fcg
  }

  def findFiles(baseDir: File, r: Regex): List[File] = {
    val thisDir = baseDir.listFiles.toList
    thisDir.filter(
      (f: File) => {
        (!f.isDirectory) && (r.findFirstIn(f.getName).isDefined)
      }) ::: thisDir.filter(_.isDirectory).flatMap(findFiles(_,r))
  }


  def generateClassTemplate(className: String): String = {
"""
package siris.components.editor.tests

import java.io.File
import swing.Component
import scala.swing._

class """ + className + """ extends FileComponentGenerator {
  def generate(f: File): Component = {
    /*Put your code here*/
  }
}
"""
  }

  def openCode(f: File) {
    val cmd: List[String] = "open" :: f.getCanonicalPath :: Nil
    var child = Runtime.getRuntime().exec(cmd.toArray);
    child.waitFor
    child = Runtime.getRuntime().exec(cmd.toArray);
    child.waitFor
  }

  def saveTo(f: File) {
    val data = {for(entry <- displayMap) yield
      <entry>
        <extension>{entry._1}</extension>
        <fcg>{entry._2._1.getCanonicalPath}</fcg>
      </entry>
    }.toSeq

    val xml = new Elem(null, "config", null, scala.xml.TopScope, data:_*)

    scala.xml.XML.save(f.toString,xml)
  }

  def loadFrom(f: File) {
    val xml = scala.xml.XML.loadFile(f)
    val entries = xml \ "entry"
    for(entry <- entries){
      val f = new File((entry \ "fcg").text)
      val key = (entry \ "extension").text
      if(f.exists) {
        displayMap += key -> (f, loadFCG(f, "siris.components.editor.tests." + key.substring(1) + "FileComponentGenerator"))
        val fm = new FileMonitor(1000L)
        fm.addFile(f)
        fm.addListener(new FileListener {
          def fileChanged(p1: File) = {
            println("File " + p1.getName + " changed. Recompiling and loading ..." )
            displayMap.update(key, (f, loadFCG(f, "siris.components.editor.tests." + key.substring(1) + "FileComponentGenerator")))
          }
        })
      }
    }
  }

}

class MyCB[A](items: Seq[A]) extends ComboBox(items) {
  def getItems = (for(i <- 0 until peer.getModel.getSize) yield peer.getModel.getElementAt(i).asInstanceOf[A]).toSeq


  peer.addActionListener(Swing.ActionListener{
    e => publish(event.SelectionChanged(this))
  })


}