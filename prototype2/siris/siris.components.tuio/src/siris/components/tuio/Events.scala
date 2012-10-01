package siris.components.tuio

import siris.components.eventhandling.EventDescription
import siris.ontology.Symbols

/*
 * Created by IntelliJ IDEA.
 * User: martin
 * Date: 9/26/11
 * Time: 11:58 AM
 */
object Events {

  /**
   * An event of this type should
   * contain a siris.ontology.types.Interactions cvar
   */
  val interactions = new EventDescription( Symbols.interactions )

  /**
   * An event of this type should contain
   * a siris.ontology.types.Identifier cvar,
   * a siris.ontology.types.Position2D cvar and
   * a siris.ontology.types.Angle cvar
   */
  val tuioObjectAdded = new EventDescription( Symbols.tuioObjectAdded )

  /**
   * An event of this type should contain
   * a siris.ontology.types.Identifier cvar,
   * a siris.ontology.types.Position2D cvar and
   * a siris.ontology.types.Angle cvar
   */
  val tuioObjectUpdated = new EventDescription( Symbols.tuioObjectUpdated )

  /**
   * An event of this type should contain
   * a siris.ontology.types.Identifier cvar,
   * a siris.ontology.types.Position2D cvar and
   * a siris.ontology.types.Angle cvar
   */
  val tuioObjectRemoved = new EventDescription( Symbols.tuioObjectRemoved )
}