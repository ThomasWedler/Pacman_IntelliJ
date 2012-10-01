package siris.core.component

import actors.Actor
import siris.core.entity.description.{SVal, SValList, Semantics}

/*
 * Created by IntelliJ IDEA.
 * User: martin
 * Date: 6/10/11
 * Time: 10:05 AM
 */
abstract class ComponentConfiguration {

  /**
   *  The target components type (used for lookup functionality)
   */
  def targetComponentType: SVal[Semantics]

  /**
   *  Creates a SValList that hold all information needed to configure a component.
   */
  def toConfigurationParams: SValList

  /**
   *  Sends a ConfigureComponentMessage to all Components of the targetComponentType
   */
  def deliver() {
    Component(targetComponentType).foreach(deliverTo)
  }

  /**
   *  Sends a ConfigureComponentMessage to receiver if it is of the correct component type.
   */
  def deliverTo(receiver: Component) {
    if(Component(targetComponentType).contains(receiver))
      receiver ! ConfigureComponentMessage(Actor.self, toConfigurationParams)
  }
}