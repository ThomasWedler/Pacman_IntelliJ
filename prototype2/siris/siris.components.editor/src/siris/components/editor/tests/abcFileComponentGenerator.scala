package siris.components.editor.tests

import java.io.File
import swing.{Label, Component}

class abcFileComponentGenerator extends FileComponentGenerator {
  def generate(f: File): Component = {
    /*Put your code here*/
    new Label
  }
}