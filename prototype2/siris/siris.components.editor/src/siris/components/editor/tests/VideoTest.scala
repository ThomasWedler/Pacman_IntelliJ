//package siris.components.editor.tests
//
//import swing.{MainFrame, Label}
//import java.awt.Dimension
//import javax.swing.ImageIcon
//
///**
// * Created by IntelliJ IDEA.
// * User: martin
// * Date: 4/28/11
// * Time: 2:17 PM
// * To change this template use File | Settings | File Templates.
// */
//
////Run using the following vm parameters: "-d32 -Djava.library.path=lib/quicktime"
//object VideoTest {
//
//  def main(args: Array[String]): Unit = {
//
//    val vc = new siris.components.editor.gui.VideoCapture(640, 480)
//
//    val l = new Label()
//
//    val mf = new MainFrame{
//      contents = l
//      size = new Dimension(640, 480)
//    }
//    mf.visible = true
//
//    var i = 0
//    while(true) {
//      //println("Frame " + i)
//      //i += 1
//      l.icon = new ImageIcon(vc.getNextImage)
//      Thread.sleep(10)
//    }
//
//  }
//}