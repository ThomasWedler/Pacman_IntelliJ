package siris.components.editor.tests

import scala.swing.Component
import java.io.File

/**
 * Created by IntelliJ IDEA.
 * User: martin
 * Date: 2/1/11
 * Time: 5:03 PM
 * To change this template use File | Settings | File Templates.
 */

abstract class FileComponentGenerator extends Component {
  def generate(f: File): Component
}