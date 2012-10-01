/*
 * Created by IntelliJ IDEA.
 * User: martin
 * Date: 3/13/11
 * Time: 1:42 PM
 */
package siris.components.editor.gui

import swing._
import scala.collection.mutable.{Set, Map}
import java.io.File
import siris.components.editor.filesystem._
import swing.event.{ButtonClicked, SelectionChanged}
import siris.components.editor.AvailableViewsChanged
import actors.Actor
import siris.components.editor.UpdateSVarDetailsView
import util.matching.Regex

class SVarSetterPanel(val node: EnSVar, sVarIdentifier: Symbol, man: ClassManifest[_], sVarSetters: Map[Symbol, DynamicReloader[SVarSetterGeneratorBase]], availableSetters: Set[DynamicReloader[SVarSetterGeneratorBase]], editorActor: Actor) extends DetailsView
{
  private var updateFunc: () => Unit = () => {}

  /*Minor Components*/
  val editButton = new Button("e"){
    maximumSize = new Dimension(20, 20)
    minimumSize = new Dimension(20, 20)
    preferredSize = new Dimension(20, 20)

    listenTo(this)
    reactions += {
      case ButtonClicked(source) if(source == this) =>
        val selectedItem: DynamicReloader[SVarSetterGeneratorBase]  = list.selection.item
        if(selectedItem != SVarViewPanel.default)
          selectedItem.showInIDE
    }

  }
  val addButton = new Button("+"){
    maximumSize = new Dimension(20, 20)
    minimumSize = new Dimension(20, 20)
    preferredSize = new Dimension(20, 20)

    listenTo(this)

    reactions += {
      case ButtonClicked(source) if(source == this) =>
        val typeName = man.erasure.getName
        val shortClassName = typeName.substring(typeName.lastIndexOf('.') + 1) + "SVarSetterGenerator" + java.util.UUID.randomUUID.toString.replace("-", "")
        val fullClassName = "siris.components.editor.gui." + shortClassName
        val file = new File(SVarViewPanel.sourceDir, shortClassName + ".scala")
        val dr = new DynamicReloader[SVarSetterGeneratorBase] (
          ClassFile(file, fullClassName),
          SVarSetterPanel.compilerSettings,
          Some(SVarSetterPanel.generateClassTemplateFor(shortClassName, typeName)),
          (svarViewGenBaseOption: Option[SVarSetterGeneratorBase]) => {list.repaint; updateSVarView}
        ) {
          override def toString(): String = {
            getCurrentClass match {
              case Some(clazz) => clazz.toString
              case None => "Class loaded with errors"
            }
          }
        }
        availableSetters.add(dr)
        publish(AvailableViewsChanged())
    }
  }
  val removeButton = new Button("-"){
    maximumSize = new Dimension(20, 20)
    minimumSize = new Dimension(20, 20)
    preferredSize = new Dimension(20, 20)

    listenTo(this)
    reactions += {
      case ButtonClicked(source) if(source == this) =>
        val selectedItem: DynamicReloader[SVarSetterGeneratorBase]  = list.selection.item

        availableSetters.remove(selectedItem)
        sVarSetters.get(sVarIdentifier).collect{
          case dr => if(dr == selectedItem) sVarSetters.remove(sVarIdentifier)
        }
        println("Removing file " + selectedItem.classFile.file.getName)
        selectedItem.classFile.file.deleteOnExit
        publish(AvailableViewsChanged())

    }
  }

  private val initialListItems = if(availableSetters.isEmpty) Seq(SVarSetterPanel.defaulListItem) else availableSetters.toSeq

  val list = new AdvComboBox[DynamicReloader[SVarSetterGeneratorBase]](initialListItems){
    listenTo(this)

    sVarSetters.get(sVarIdentifier).collect{
      case dr =>
        selection.item = dr
    }

    reactions += {
      case SelectionChanged(source) if(source == this) =>
        val selectedItem: DynamicReloader[SVarSetterGeneratorBase]  = selection.item
        //println("Updating " + sVarIdentifier.name)
        sVarSetters.update(sVarIdentifier, selectedItem)
        updateSVarView
    }

    if((getItems.size == 1) && (getItems(0) == SVarSetterPanel.defaulListItem)) enabled = false
  }

  private def updateSVarView(){
    if(list.enabled) {
      if(list.selection.item.getCurrentClass.isDefined) {
        val newSVarSetterBase: SVarSetterBase = list.selection.item.getCurrentClass.get.generate
        newSVarSetterBase.changeRegisteredSvarTo(node.svar)
        pane.contents = newSVarSetterBase.component
        list.selection.item.onLoad = (svarViewGenBaseOption: Option[SVarSetterGeneratorBase]) => {list.repaint; updateSVarView}
        updateFunc = () => {newSVarSetterBase.internalUpdate(node.children.head.asInstanceOf[EnSVarValue].value)}
        updateFunc.apply
      }
      else {
        pane.contents = new Label("Error on loading SVar setter")
      }
    }
    else
      pane.contents = new Label("No SVar setter selected.")
  }

  val pane = new ScrollPane
  /*Minor Components END*/

  //Set the svarview
  updateSVarView
//  private val initialSVarViewBase = list.selection.item.getCurrentClass.getOrElse(SVarViewPanel.default.getCurrentClass.get).generate
//  pane.contents = initialSVarViewBase.component
//  private var updateFunc: () => Unit = () => {initialSVarViewBase.internalUpdate(node.value)}
//  updateFunc.apply

  /*Main Component*/
  val component = new GridBagPanel() {

    listenTo(addButton)
    listenTo(removeButton)

    val gbc = new Constraints()
    gbc.fill = GridBagPanel.Fill.Both
    gbc.gridy = 0
    gbc.gridx = 0
    gbc.weighty = 1.0
    gbc.weightx = 1.0
    add(pane, gbc)
    gbc.gridy = 1
    gbc.gridx = 0
    gbc.weighty = 0.0
    gbc.weightx = 0.0
    add(new BoxPanel(Orientation.Horizontal) {
      contents += addButton
      contents += removeButton
      contents += editButton
      contents += list
    }, gbc)
    border = Swing.EmptyBorder(10, 10, 10, 10)

    reactions += {
      case event: AvailableViewsChanged =>
        val sel = list.selection.item
        val listItems = if(availableSetters.isEmpty) Seq(SVarSetterPanel.defaulListItem) else availableSetters.toSeq
        list.peer.setModel(ComboBox.newConstantModel(listItems))
        if(list.getItems.contains(sel)) list.selection.item = sel
        if((list.getItems.size == 1) && (list.getItems(0) == SVarSetterPanel.defaulListItem)) list.enabled = false else list.enabled = true
    }
  }

  //TODO: refactor class structure. Update is not used for a setter
  def update = updateFunc.apply
  /*Main component END*/

}

object SVarSetterPanel {


  val defaulListItem = new DynamicReloader[SVarSetterGeneratorBase](ClassFile(null, ""), CompilerSettings(null, List[File](), List[File]()), None, (o : Option[SVarSetterGeneratorBase]) => {}) {

    private val viewGen = Some(new SVarSetterGeneratorBase {
      val name = "No setter avaiable."

      def generate: SVarSetterBase = {
        throw new Exception {
          override def toString = "SVarSetterPanel.defaulListItem this should not be called"
        }
        null.asInstanceOf[SVarSetterBase]
      }
    })

    currentClass = viewGen

    override def showInIDE = {}
    override protected def init() = {}

    override def toString(): String = {
      currentClass match {
        case Some(clazz) => clazz.toString
        case None => "Class loaded with errors"
      }
    }
  }

  val sourceDir = new File("./editor-settings/src")
  val sirisLibs = findFiles(new File("./lib"), """.*\.jar""".r)
  val sirisClassDir = new File("./target/scala_2.9.0-1/classes")

  val compilerSettings = CompilerSettings(
    sourceDir,
    sirisLibs ::: new File("../../scala/current/lib/scala-library.jar") :: new File("../../scala/current/lib/scala-swing.jar") :: sirisClassDir :: Nil,
    List[File]()//new File("./siris.components.editor/src/siris/components/editor/gui/SVarView.scala") :: Nil
  )

  def findFiles(baseDir: File, r: Regex): List[File] = {
    val thisDir = baseDir.listFiles.toList
    thisDir.filter(
      (f: File) => {
        (!f.isDirectory) && (r.findFirstIn(f.getName).isDefined)
      }) ::: thisDir.filter(_.isDirectory).flatMap(findFiles(_, r))
  }

def generateClassTemplateFor(className: String, typeName: String): String = {

  val shortTypeName = typeName.substring(typeName.lastIndexOf('.') + 1)

"""//AutoGenerated Begin
//DO NOT EDIT!
package siris.components.editor.gui

import scala.swing._
import """ + typeName + """

class """ + className + """ extends SVarSetterGenerator[""" + shortTypeName + """] {

  def generate: SVarSetter[""" + shortTypeName + """] = new SVarSetter[""" + shortTypeName + """] {

//AutoGenerated END
//Put your code below

    /**
     *  The scala.swing.Component that visualizes the SVar setter.
     *  Call
     *  setSvar(newValue: """ + shortTypeName + """): Unit
     *  to set new svar value.
     */
    //Todo: Implement yourself!
    val component = new Button("Click me to set the svar") {
      listenTo(this)
      reactions += {
        case event: event.ButtonClicked =>
          Dialog.showMessage(null, "Use 'e' button to edit the code", "Setter not implemented yet")

          /*
          setSvar(...)
          */
      }
    }

    /**
     * Override update if you want to use the current value of the SVar.
     * This function is initially called once and then every time the value of the SVar changes.
     */
    // override def update(newValue: """ + shortTypeName + """): Unit = {}

  }

  /**
   *  The name of this visualizer.
   *  This must not be unique.
   */
  //Todo: Name it!
  val name: String = """" + shortTypeName + """ Setter"

}"""
}

}