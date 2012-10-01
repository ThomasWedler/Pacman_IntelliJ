/*
 * Created by IntelliJ IDEA.
 * User: martin
 * Date: 2/28/11
 * Time: 7:47 PM
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

class SVarViewPanel(val node: TreeNodeWithValue, sVarIdentifier: Symbol, man: ClassManifest[_], sVarViews: Map[Symbol, DynamicReloader[SVarViewGeneratorBase]], availableViews: Set[DynamicReloader[SVarViewGeneratorBase]], editorActor: Actor) extends DetailsView
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
        val selectedItem: DynamicReloader[SVarViewGeneratorBase]  = list.selection.item
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
        val shortClassName = typeName.substring(typeName.lastIndexOf('.') + 1) + "SVarViewGenerator" + java.util.UUID.randomUUID.toString.replace("-", "")
        val fullClassName = "siris.components.editor.gui." + shortClassName
        val file = new File(SVarViewPanel.sourceDir, shortClassName + ".scala")
        val dr = new DynamicReloader[SVarViewGeneratorBase] (
          ClassFile(file, fullClassName),
          SVarViewPanel.compilerSettings,
          Some(SVarViewPanel.generateClassTemplateFor(shortClassName, typeName)),
          (svarViewGenBaseOption: Option[SVarViewGeneratorBase]) => {list.repaint; updateSVarView}
        ) {
          override def toString(): String = {
            getCurrentClass match {
              case Some(clazz) => clazz.toString
              case None => "Class loaded with errors"
            }
          }
        }
        availableViews.add(dr)
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
        val selectedItem: DynamicReloader[SVarViewGeneratorBase]  = list.selection.item

        if(selectedItem != SVarViewPanel.default){
          availableViews.remove(selectedItem)
          sVarViews.get(sVarIdentifier).collect{
            case dr => if(dr == selectedItem) sVarViews.remove(sVarIdentifier)
          }
          println("Removing file " + selectedItem.classFile.file.getName)
          selectedItem.classFile.file.deleteOnExit
          publish(AvailableViewsChanged())
        }
    }
  }

  val list = new AdvComboBox[DynamicReloader[SVarViewGeneratorBase]](Seq(SVarViewPanel.default).union(availableViews.toSeq)){
    listenTo(this)

    sVarViews.get(sVarIdentifier).collect{
      case dr =>
        selection.item = dr
    }

    reactions += {
      case SelectionChanged(source) if(source == this) =>
        val selectedItem: DynamicReloader[SVarViewGeneratorBase]  = selection.item
        //println("Updating " + sVarIdentifier.name)
        sVarViews.update(sVarIdentifier, selectedItem)
        updateSVarView
    }
  }

  private def updateSVarView(){
    val newSVarViewBase = list.selection.item.getCurrentClass.getOrElse(SVarViewPanel.default.getCurrentClass.get).generate
    pane.contents = newSVarViewBase.component
    updateFunc = () => {newSVarViewBase.internalUpdate(node.value)}
    updateFunc.apply

    list.selection.item.onLoad = (svarViewGenBaseOption: Option[SVarViewGeneratorBase]) => {list.repaint; updateSVarView}
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
        list.peer.setModel(ComboBox.newConstantModel(Seq(SVarViewPanel.default).union(availableViews.toSeq)))
        if(list.getItems.contains(sel)) list.selection.item = sel
    }
  }

  def update = updateFunc.apply
  /*Main component END*/

}

class DefaultSvarView extends SVarView[Any] {
  def update(sVarValue: Any) {
    component.text = sVarValue.toString
  }

  val component = new Label
//  val name = "Default SVar View"
}

object SVarViewPanel {

  val default = new DynamicReloader[SVarViewGeneratorBase](ClassFile(null, ""), CompilerSettings(null, List[File](), List[File]()), None, (o : Option[SVarViewGeneratorBase]) => {}) {

    private val viewGen = Some(new SVarViewGenerator[Any] {
      val name = "Default View"
      private val view = new DefaultSvarView
      def generate = view
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

class """ + className + """ extends SVarViewGenerator[""" + shortTypeName + """] {

  def generate: SVarView[""" + shortTypeName + """] = new SVarView[""" + shortTypeName + """] {

//AutoGenerated END
//Put your code below

    /**
     *  The scala.swing.Component that visualizes the value.
     */
    //Todo: Implement yourself!
    val component = new Label

    /**
      *  This function is called whenever the visualized value changes.
      *  It should update component accordingly.
      */
    //Todo: Implement yourself!
    def update(sVarValue: """ + shortTypeName + """) {
      component.text = sVarValue.toString
    }

  }

  /**
   *  The name of this visualizer.
   *  This must not be unique.
   */
  //Todo: Name it!
  val name: String = """" + shortTypeName + """ View"

}"""
}

}