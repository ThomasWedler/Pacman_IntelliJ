package siris.components.editor.gui

import scala.swing._
import event.{Event, TreePathSelected}
import scala.swing.Tree.Renderer
import scala.collection._
import siris.core.entity.Entity
import java.io.File
import java.awt.{Color, Dimension}
import siris.components.editor._
import javax.swing.{SwingUtilities, ImageIcon}

/**
* Created by IntelliJ IDEA.
* User: martin
* Date: 2/9/11
* Time: 10:51 AM
* To change this template use File | Settings | File Templates.
*/
class EditorPanel(editorActor: Editor) extends MainFrame with SynchronizedReactor {

  private var treeRoot = new EnRoot("SIRIS App")
  private var svarToNode = mutable.Map[(Entity, Symbol), EnSVarValue]()
  private var selectedNode: Option[TreeNode] = None

  val visualisations = new TreeNodeVisualisations(new File("./editor-settings/config.xml"), editorActor)

  //Set up swing
  val tree =
    new AdvTree[TreeNode] {

      peer.setRowHeight(31)

      private val entityIcon = new ImageIcon("editor-settings/icons/entity.jpg")
      private val aspectIcon = new ImageIcon("editor-settings/icons/aspect.jpg")
      private val svarIcon = new ImageIcon("editor-settings/icons/svar.jpg")
      private val rootIcon = new ImageIcon("editor-settings/icons/root.jpg")
      private val svarRootIcon = new ImageIcon("editor-settings/icons/svarroot.jpg")
      private val createParamIcon = new ImageIcon("editor-settings/icons/createparam.jpg")

      private val childFunc = (tn: TreeNode) => {
        tn match {
          case node: EnSVar => Seq()
          case node: EnCreateParam => Seq()
          case node => node.children
        }
      }

      treeData = new AdvTreeModel[TreeNode](Seq(treeRoot), childFunc)

      renderer = new Renderer[TreeNode] {
        def componentFor(tree: Tree[_], value: TreeNode, cellInfo: companion.CellInfo): Component = {


          val comp =
            value match {

              case node: EnRoot => new Label(node.appName) {icon = rootIcon}
              case node: EnEntity => new Label(node.name) {icon = entityIcon}
              case node: EnSVarCollection => new Label("SVars") {icon = svarRootIcon}
              case node: EnSVar => new Label(node.name.name) {icon = svarIcon}
              case node: EnSVarValue => new Label(node.value.toString)
              case node: EnCreateParamSet => new Label(node.component.name) {icon = aspectIcon}
              case node: EnCreateParam => new Label(node.cpb.typedSemantics.sVarIdentifier.name) {icon = createParamIcon}
              case node: EnCreateParamValue => new Label(node.toString)
              case _ => new Label("Unknown")
            }

          if (cellInfo.isSelected) {
            comp.opaque = true
            comp.background = Color.lightGray
          }

          comp.font = comp.font.deriveFont(18.0f)

          comp
        }
      }

    }

  def sVarChanged(pathToParentNode: List[TreeNode], newNode: TreeNode) = {
    tree.fireNodesChanged(pathToParentNode, newNode)

    if(tree.currentPath.corresponds(pathToParentNode ::: (newNode :: Nil))((a, b) => a == b)) println("update")
  }

  val details = new DetailsScrollPane(
    new DetailsView {
      val component = new Label("Click on a tree node to see its details here.")
      def update = {}
      val node = treeRoot
    }
  )


  //SVarUpdateInterval
//  val slider = new Slider() {
//
//    min = 1
//    max = 13
//    value = 1
//    majorTickSpacing = 4
//    minorTickSpacing = 1
//    paintTicks = true
//    paintLabels = true
//    snapToTicks = true
//  }
//
//  val label = new BoxPanel(Orientation.Horizontal){
//    contents += new Label("SVar update interval in sec", Swing.EmptyIcon , Alignment.Center)
//  }


  title = "Siris Editor"
  contents = new GridPanel(1, 2) {
    contents += new ScrollPane(tree)
    contents += details
    border = Swing.EmptyBorder(10, 10, 10, 10)
  }

  listenTo(tree)
  listenTo(editorActor)

  addSynchronizedReaction {
    case msg: AppNameChanged => {

      treeRoot.appName = msg.name
      tree.fireNodesChanged(List[TreeNode](), treeRoot)
    }
  }

  addSynchronizedReaction {
    case msg: NewSVarValueArrived => {
      svarToNode.get((msg.e, msg.sVarName)).collect({ case node =>
        node.value = msg.value
        tree.fireNodesChanged(node.getPathToParent, node)
        if(node == details.detailsView.node) details.detailsView.update
      })
    }
  }

  addSynchronizedReaction {
    case msg: NewEntityNameArrived => {
      treeRoot.children.find((tn) => {
        tn match {
          case node: EnEntity => node.e == msg.e
          case _ => false
        }
      }).collect{
        case node: EnEntity =>
          node.name = msg.name
          tree.fireNodesChanged(node.getPathToParent, node)
      }
    }
  }

  addSynchronizedReaction {
    case msg: EntityConfigurationArrived => {
      //Add the new configuration to the tree

      val eNode = new EnEntity(msg.e, Option(treeRoot))
      msg.csets.foreach( (symbolCsetTuple) => {
        val csetNode = new EnCreateParamSet(symbolCsetTuple._1, symbolCsetTuple._2, Option(eNode))

        symbolCsetTuple._2.foreach( (cpb) => {
          val cparamNode = new EnCreateParam(cpb, Option(csetNode))
          new EnCreateParamValue(cpb.value, Option(cparamNode))
        })
      })

      val svarCollNode = new EnSVarCollection(Option(eNode))

      msg.e.getAllSVars.foreach{
        x => svarToNode += (msg.e, x._1) -> new EnSVarValue( Some( EnSVar( x._3, x._1, Some( svarCollNode ) ) ) )
      }

      tree.fireNodesInserted(eNode.getPathToParent, eNode)
    }
  }

  addSynchronizedReaction {
    case msg: RemoveEntity => {
      treeRoot.children.find((tn) => {
        tn match {
          case node: EnEntity => node.e == msg.e
          case _ => false
        }
      }).collect {
        case node: EnEntity =>
          val e = tree.prepareNodeRemoval(node.getPathToParent, node)
          treeRoot.children = (treeRoot.children.filterNot(_ == node)):_*
          tree.fireNodesRemoved(e)
      }
    }
  }

  reactions += {
    case e: TreePathSelected[_] => {
      try {
        selectedNode = Some(e.newPaths.head.reverse.head.asInstanceOf[TreeNode])
        details.detailsView = visualisations.detailsViewFor(selectedNode.get)
        details.detailsView.update
      }
      catch {
        case e => {} //{println(e)}
      }
    }
  }

  addSynchronizedReaction {
    case e: UpdateSVarDetailsView => {
      selectedNode.collect{
        case node =>
          details.detailsView = visualisations.detailsViewFor(node)
          details.detailsView.update
      }
    }
  }

  //SVarUpdateInterval
//    listenTo(slider)
//    reactions += {
//      case ValueChanged(comp) if comp == slider =>
//        editorActor ! ChangeUpdateInterval(slider.value)
//    }

  size = new Dimension(1067, 600)
  minimumSize = new Dimension(640, 480)
  visible = true

  override def closeOperation() = {
    visualisations.saveConfiguration
    //super.closeOperation
  }
}