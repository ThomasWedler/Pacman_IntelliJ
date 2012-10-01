package siris.components.editor.tests

import javax.swing.tree.TreeNode
import java.awt.Dimension
import actors.Actor
import util.Random
import javax.swing.JTree
import scala.swing.Tree._
import swing._
;

/*
 * Created by IntelliJ IDEA.
 * User: martin
 * Date: 2/14/11
 * Time: 11:05 AM
 */


object SimpleTreeTest {

  case class Node[A](value: A, c: Node[A]*) {
    var children: Seq[Node[A]] = c
  }

  val menuItems = Node("Hobbies", Node("aufduengen"), Node("absahnen"))

  def main(args: Array[String]): Unit = {

    adjustTheScheduler

     //... etc

    val tree = new Tree[Node[String]] {
      treeData = TreeModel(menuItems)(_.children).updatableWith(
        (p: Path[Node[String]], newValue: Node[String]) => {

          if(newValue.value == "") p.reverse.head.children = List[Node[String]]().toSeq
          else {
            if(p.reverse.head.children != null)
              p.reverse.head.children = newValue +: p.reverse.head.children
            else
              p.reverse.head.children = (newValue :: Nil).toSeq
          }
          expandAll
          newValue
      })
      renderer = Tree.Renderer(_.value)
    }

    val mf = new MainFrame {
      contents = new ScrollPane(tree)
      size = new Dimension(800, 600)
      minimumSize = new Dimension(800, 600)
    }
    mf.visible = true
    startActor(tree, menuItems :: menuItems.children(0) :: Nil, "N")


  }

  def startActor(t: Tree[Node[String]], p: Path[Node[String]], s: String): Unit = {
    new Actor {
      def act() = {
//        val rnd = new Random
//        val r = rnd.nextInt(1000) + 1000
        Thread.sleep(200)
        //println("Adding added " + s + "A and " + s + "B")

        if(s != "") {
          if(s.size <= 15) {
            val newNode1 = new Node(s + "A")
            //val newNode2 = new Node(s + "B")
//            println("adding node")
            t.treeData.update(p, newNode1)
            //t.treeData.update(p, newNode2)
            startActor(t, p ::: (newNode1 :: Nil), s + "o")
            //startActor(t, p ::: (newNode2 :: Nil), s + "o")
          }
          else {
            val newNode = new Node("")
//            print("starting to remove node ")
//            p.reverse.head.children.foreach(n => {println(n.value)})
            t.treeData.update(p, newNode)
            startActor(t, p.dropRight(1), "")
          }
        }
        else {
          if(p.size > 1) {
            val newNode = new Node("")
//            print("removing node ")
//            p.reverse.head.children.foreach(n => {println(n.value)})
            t.treeData.update(p, newNode)
            startActor(t, p.dropRight(1), "")
          }
          else {
            val newNode1 = new Node("NA")
            //val newNode2 = new Node(s + "B")
//            println("starting to add nodes")
            t.treeData.update(menuItems :: menuItems.children(0) :: Nil, newNode1)
            //t.treeData.update(p, newNode2)
            startActor(t, menuItems :: menuItems.children(0) :: newNode1 :: Nil, "No")
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

}