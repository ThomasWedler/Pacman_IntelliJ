package siris.components.physics

import siris.components.eventhandling.EventDescription
import siris.ontology.Symbols


/*
* Created by IntelliJ IDEA.
* User: martin
* Date: 6/8/11
* Time: 10:31 AM
*/
object PhysicsEvents {

  /**
   * An event of this type should
   * contain no additional data
   */
  val collision = new EventDescription( Symbols.collision )

  /**
   * An event of this type should
   * contain a siris.ontology.types.Impulse cvar
   */
  val impulse = new EventDescription( Symbols.impulse )

  /**
   * An event of this type should
   * contain a siris.ontology.types.Impulse cvar
   */
  val torqueImpulse = new EventDescription( Symbols.torqueImpulse )
}