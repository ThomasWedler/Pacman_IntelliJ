package siris.components.editor.tests

import swing.{ScrollPane, TreeModel, Tree, MainFrame}
import java.awt.Dimension
import actors.Actor
import scala.swing.Tree._
import javax.swing.event.TreeModelEvent
import javax.swing.tree.{DefaultTreeModel, TreePath}
import util.Random

/*
 * Created by IntelliJ IDEA.
 * User: martin
 * Date: 2/14/11
 * Time: 11:05 AM
 */


object SimpleTreeTestWithAdjustedTree {


//  val stream = getClass.getClassLoader.getResourceAsStream("quotes.quotes.txt")
//  println(stream)

  val quotes = "1f u c4n r34d th1s u r34lly n33d t0 g37 l41d" ::
               "If at first you don't succeed; call it version 1.0 " ::
               "There are 10 types of people in the world: those who understand binary, and those who don't." ::
               Nil

  class MyTreeModel[A](override val roots: Seq[A], children: A => Seq[A]) extends TreeModel[A](roots, children) {

    val hr = peer.getRoot

    private def getChildrenOf(parent: Any) = parent match {
      case `hr` => roots
      case a => children(a.asInstanceOf[A])
    }

    private def createEvent(path: TreePath, newValue: Any) = new TreeModelEvent(this, path,
      Array(getChildrenOf(path.getPath.last) indexOf newValue),
      Array(newValue.asInstanceOf[AnyRef]))

    def fire(path: Path[A], result: Any) = {
      //peer.fireTreeStructureChanged(pathToTreePath(path), result)

      //peer.treeModelListeners.foreach(_.treeStructureChanged(new TreeModelEvent(this, pathToTreePath(path))))
      peer.treeModelListeners.foreach(_.treeStructureChanged(createEvent(pathToTreePath(path), result)))
//      val x = new DefaultTreeModel
//      x.nodeChanged()
    }

    def fire2(path: Path[A], result: Any) = {
      //peer.fireTreeStructureChanged(pathToTreePath(path), result)

      //peer.treeModelListeners.foreach(_.treeStructureChanged(new TreeModelEvent(this, pathToTreePath(path))))
      peer.treeModelListeners.foreach(_.treeNodesChanged(createEvent(pathToTreePath(path), result)))
      //      val x = new DefaultTreeModel
      //      x.nodeChanged()
    }
  }

  class MyTree[A](private var treeDataModel: TreeModel[A] = TreeModel.empty[A]) extends Tree[A](treeDataModel) {
    private var myTreeData: MyTreeModel[A] = null

    def treeData_=(tm: MyTreeModel[A]) = {
      if (treeDataModel != null)
        treeDataModel.peer.removeTreeModelListener(modelListener)

      treeDataModel = tm
      peer.setModel(tm.peer)
      treeDataModel.peer.addTreeModelListener(modelListener)
      myTreeData = tm
    }

    def fire(path: Path[A], result: Any) = myTreeData.fire(path, result)
    def fire2(path: Path[A], result: Any) = myTreeData.fire2(path, result)
  }

  case class Node[A](var value: A, c: Node[A]*) {
    var children: Seq[Node[A]] = c
  }

  val menuItems = Node("Hobbies", Node("aufduengen"), Node("absahnen"))
  var tree: MyTree[Node[String]] = null

  def main(args: Array[String]): Unit = {

    adjustTheScheduler

     //... etc

    tree = new MyTree[Node[String]] {
      treeData = new MyTreeModel[Node[String]]((menuItems :: Nil).toSeq, _.children)
//        .updatableWith(
//        (p: Path[Node[String]], newValue: Node[String]) => {
//
//          if(newValue.value == "") p.reverse.head.children = List[Node[String]]().toSeq
//          else {
//            if(p.reverse.head.children != null)
//              p.reverse.head.children = newValue +: p.reverse.head.children
//            else
//              p.reverse.head.children = (newValue :: Nil).toSeq
//          }
//          expandAll
//          newValue
//      })

      renderer = Tree.Renderer(_.value)
    }

    val mf = new MainFrame {
      contents = new ScrollPane(tree)
      size = new Dimension(800, 600)
      minimumSize = new Dimension(800, 600)
    }
    mf.visible = true
    startActor(tree, menuItems :: menuItems.children(0) :: Nil, "L")


  }

  def startActor(t: Tree[Node[String]], p: Path[Node[String]], s: String): Unit = {
    new Actor {
      def act() = {
//        val rnd = new Random
//        val r = rnd.nextInt(1000) + 1000
        Thread.sleep(16)
        //println("Adding added " + s + "A and " + s + "B")

        if(s != "") {
          if(s.size <= 15) {
            val newNode1 = new Node(s + "L")
            //val newNode2 = new Node(s + "B")
//            println("adding node")
            myUpdate(p, newNode1)
            tree.fire(p, newNode1)
            //t.treeData.update(p, newNode2)
            startActor(t, p ::: (newNode1 :: Nil), s + "o")
            //startActor(t, p ::: (newNode2 :: Nil), s + "o")
          }
          else {



            val text = quotes(Random.nextInt(quotes.size))
            for(i <- 0 to text.size){
              menuItems.children(0).children(0).value = text.substring(0,i)
              tree.fire2(menuItems :: menuItems.children(0) :: Nil, menuItems.children(0).children(0))
              //tree.expandAll
              Thread.sleep(16)
            }


            val newNode = new Node("")
//            print("starting to remove node ")
//            p.reverse.head.children.foreach(n => {println(n.value)})
            myUpdate(p, newNode)
            //menuItems.children(0).children = List[Node[String]]()
            //tree.expandAll
            tree.fire(p, newNode)

            //tree.fire(menuItems :: menuItems.children(0) :: Nil, null)
            //startActor(tree, menuItems :: menuItems.children(0) :: Nil, "N")
            startActor(t, p.dropRight(1), "")
          }
        }
        else {
          if(p.size > 1) {
            val newNode = new Node("")
//            print("removing node ")
//            p.reverse.head.children.foreach(n => {println(n.value)})
            myUpdate(p, newNode)
            tree.fire(p, newNode)
            startActor(t, p.dropRight(1), "")
          }
          else {
            val newNode1 = new Node("LL")
            //val newNode2 = new Node(s + "B")
//            println("starting to add nodes")
            myUpdate(menuItems :: menuItems.children(0) :: Nil, newNode1)
            tree.fire(menuItems :: menuItems.children(0) :: Nil, newNode1)
            //t.treeData.update(p, newNode2)
            startActor(t, menuItems :: menuItems.children(0) :: newNode1 :: Nil, "Lo")
          }
        }
//        dump(menuItems)
//        println
      }
    }.start
  }

  def adjustTheScheduler = {
    val sched = new scala.actors.scheduler.ResizableThreadPoolScheduler(false)
    scala.actors.Scheduler.impl.shutdown
    scala.actors.Scheduler.impl = sched
    sched.start()
  }

  def dump(n: Node[String], prefix: String = " "): Unit = {
    println(prefix + n.value)
    if(n.children != null)
      n.children.foreach(dump(_, prefix + " "))
  }

  def myUpdate(p: Path[Node[String]], newValue: Node[String]): Unit = {

    if (newValue.value == "") p.reverse.head.children = List[Node[String]]().toSeq
    else {
      if (p.reverse.head.children != null)
        p.reverse.head.children = newValue +: p.reverse.head.children
      else
        p.reverse.head.children = (newValue :: Nil).toSeq
    }
    tree.expandAll
  }

}