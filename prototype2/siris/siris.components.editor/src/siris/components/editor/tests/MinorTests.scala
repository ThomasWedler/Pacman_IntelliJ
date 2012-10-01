package siris.components.editor.tests

import siris.components.editor.gui.EnCreateParamValue

/*
 * Created by IntelliJ IDEA.
 * User: martin
 * Date: 2/17/11
 * Time: 11:13 AM
 */
object MinorTests {


  def main(args: Array[String]): Unit = {

    val a = new EnCreateParamValue("a", None)
    val b = new EnCreateParamValue("b", Option(a))
    val c = new EnCreateParamValue("c", Option(a))
    val d = new EnCreateParamValue("d", Option(a))

    val e = new EnCreateParamValue("e", Option(c))

    a.children.foreach((tn) => {println(tn.asInstanceOf[EnCreateParamValue].toString)})

    println(e.getPath)
    println(e.getPathToParent)

  }
}