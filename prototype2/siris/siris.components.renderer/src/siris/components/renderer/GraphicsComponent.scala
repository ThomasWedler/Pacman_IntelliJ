package siris.components.renderer

import siris.core.component.Component
import siris.ontology.Symbols

/**
 * User: dwiebusch
 * Date: 12.04.11
 * Time: 19:10
 */

trait GraphicsComponent extends Component{
  def componentType = Symbols.graphics
}