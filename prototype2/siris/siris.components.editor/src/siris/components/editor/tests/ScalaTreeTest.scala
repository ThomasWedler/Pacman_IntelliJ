package siris.components.editor.tests

import swing._
import scala.swing.Tree._
import java.io.File
import event.ButtonClicked

/**
 * Created by IntelliJ IDEA.
 * User: martin
 * Date: 2/9/11
 * Time: 10:44 AM
 * To change this template use File | Settings | File Templates.
 */

object ScalaTreeTest extends SimpleSwingApplication {
  case class Customer(id: Int, title: String, firstName: String, lastName: String)
  case class Product(id: String, name: String, price: Double)
  case class Order(id: Int, customer: Customer, product: Product, quantity: Int)
  val orders: List[Order] = Order(1, Customer(11,"DW", "Dennis", "Wiebusch"), Product("111", "Bier 5L", 30.99), 6) :: Nil

//  val test = orders: _*
//  println(test)

  val tre = new Tree[Any] {
    treeData = TreeModel[Any](orders: _*)({
      case Order(_, cust, prod, qty) => Seq(cust, prod, "Qty" -> qty)
      case Product(id, name, price) => Seq("ID" -> id, "Name" -> name, "Price" -> ("$" + price))
      case Customer(id, _, first, last) => Seq("ID" -> id, "First name" -> first, "Last name" -> last)
      case _ => Seq.empty
    })

    renderer = Renderer({
      case Order(id, _, _, qty) => "Order #" + id + " x " + qty
      case Product(id, _, _) => "Product " + id
      case Customer(_, title, first, last) => title + " " + first + " " + last
      case (field, value) => field + ": " + value
      case x => x.toString
    })
  }

  val tre2 = new Tree[File] {
    treeData = TreeModel(new File(".")) {f =>
      if (f.isDirectory) f.listFiles.toSeq else Seq()
    }
  }

  case class Node[A](value: A, children: Node[A]*)
  val menuItems = Node("A", Node("AA", Node("AAA")), Node("AB", Node("ABA")))

  val tre3 = new Tree[Node[String]] {
    treeData = TreeModel(menuItems)(_.children)
    renderer = Tree.Renderer(_.value)
  }

  val button = new Button{
    text = "Puff"
  }

  def top = new MainFrame {
    title = "First Swing App"
//    contents = new TextArea {
//      text = "klöfvdj\nklöfvdj\nklöfvdj\nklöfvdj"
//    }
    contents = new BoxPanel(Orientation.Horizontal) {
      contents += tre3
      contents += button
      border = Swing.EmptyBorder(30, 30, 10, 30)
    }

    listenTo(button)
    listenTo(tre3)
    reactions += {
      case ButtonClicked(b) =>
        tre3.expandPath(menuItems :: menuItems.children(1) :: Nil)
        println("Button clicked")
      case e => println(e)
    }
  }

}