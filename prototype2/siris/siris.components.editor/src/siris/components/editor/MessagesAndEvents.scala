package siris.components.editor

import actors.Actor
import siris.core.entity.description.NamedSValList
import siris.core.entity.Entity

/**
 * Created by IntelliJ IDEA.
 * User: martin
 * Date: 2/9/11
 * Time: 3:21 PM
 * To change this template use File | Settings | File Templates.
 */

/*SIRIS MESSAGES EVENTS*/
case class ChangeUpdateInterval(newUpdateInterval: Int)

private[editor] case class UpdateSVars(sender: Actor)
/*SIRIS MESSAGES EVENTS END*/


/*SCALA SWING EVENTS*/
private[editor] case class EntityConfigurationArrived(e : Entity, csets : Map[Symbol, NamedSValList]) extends scala.swing.event.Event
private[editor] case class NewSVarValueArrived(e : Entity, sVarName : Symbol, value: Any) extends scala.swing.event.Event
private[editor] case class NewEntityNameArrived(e : Entity, name: String) extends scala.swing.event.Event
private[editor] case class AppNameChanged(name: String) extends scala.swing.event.Event
private[editor] case class AvailableViewsChanged() extends scala.swing.event.Event
private[editor] case class UpdateSVarDetailsView() extends scala.swing.event.Event
private[editor] case class RemoveEntity(e : Entity) extends scala.swing.event.Event
/*SCALA SWING EVENTS END*/