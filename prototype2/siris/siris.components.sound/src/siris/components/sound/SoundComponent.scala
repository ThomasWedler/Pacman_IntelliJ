package siris.components.sound

import siris.core.component.Component

/**
 * User: dwiebusch
 * Date: 12.04.11
 * Time: 19:08
 */

trait SoundComponent extends Component{
  def componentType = siris.ontology.Symbols.sound
}