package siris.components.editor

import siris.core.svaractor.synclayer.PluggableSyncLayer
import siris.core.helper.SVarUpdateFunctionMap
import siris.core.entity.Entity
import siris.core.svaractor.{SVar, SVarActorHW}
import scala.collection._
import actors.Actor
import swing.Publisher
import siris.core.component.{ConfigureComponentMessage, Component}
import gui._
import siris.core.entity.component.Removability
import siris.core.entity.description._
import siris.ontology.types.Name
import siris.ontology.Symbols

//Global Types
import siris.ontology.{Symbols, types => gt}

/**
 * User: martin
 * Date: Oct 29, 2010
 */

object Editor  {}

class Editor(override val componentName : Symbol)
        extends SVarActorHW with Component with SVarUpdateFunctionMap with PluggableSyncLayer with Publisher {

  protected def configure(params: SValList) = {
    params.getFirstValueFor(gt.Name).collect {
      case name => conf.name = name
    }
    params.getFirstValueFor(gt.NamedContainer).collect {
      case nc: NamedSValList =>
        if(nc.semantics == Symbols.application) {
          nc.getFirstValueFor(gt.Name).collect{
            case appName =>
              conf.appName = appName
              publish(AppNameChanged(appName))
          }
        }
    }
  }

  def componentType = Symbols.editor

  //Internal Classes
  private case class EditorConfiguration() {
    var name = "Unnamed Editor (" + this.hashCode().toString + ")"
    var appName = "Amazing SIRIS App"
  }

  //Internal Classes END

  //Members
  def removeFromLocalRep(e: Entity) = {
    e.getAllSVars.foreach {
      x =>
      //Stop observing
        x._3.ignore()
    }
    publish(RemoveEntity(e))
  }

  private val conf = EditorConfiguration()

  //SVarUpdateInterval
  //private var lastPublishes = mutable.Map[(Entity, Symbol), scala.swing.event.Event]()
  private var entityToNode = mutable.Map[Entity, TreeNode]()
//  private var treeRoot = new EnRoot("SIRIS App")

  private var gui: Option[EditorPanel] = Some(new EditorPanel(this))

  /**
   * Gui update interval in seconds
   */
  //SVarUpdateInterval
  //private var updateInterval = 1.0
  //private var currentWakeUpActor: Actor = null
  //Members END

  //Register Convertibles END
  //Register Convertibles

  //"Constructor Code
  //Register to be notified if an entity is created.
  IEntityDescription.registerCreationObserver(this)

  override def startUp() = {
    //SVarUpdateInterval
    //currentWakeUpActor = this
    //this ! UpdateSVars(this)

  }

  //Register the handlers

  //Add handler to react on an entity-creation
  //EntityConfiguration
  addHandler[EntityConfiguration]{ msg: EntityConfiguration =>
    //Add the new configuration to the tree
    publish(EntityConfigurationArrived(msg.e, msg.csets))

    msg.e.getAllSVars.foreach{ triple =>
      triple._3.observe( a => publish(NewSVarValueArrived(msg.e, triple._1, a)) )
      triple._3.get( a => publish(NewSVarValueArrived(msg.e, triple._1, a)) )
      if (triple._1 == Name.sVarIdentifier){
        triple._3.observe( a => publish(NewEntityNameArrived(msg.e, a.toString)) )
        triple._3.get( a => publish(NewEntityNameArrived(msg.e, a.toString)) )
      }
    }
  }

  //Forward events to the editor panel
  addHandler[scala.swing.event.Event]{ msg: scala.swing.event.Event =>
    publish(msg)
  }

  //SVarUpdateInterval
//  addHandler[UpdateSVars]{
//    msg: UpdateSVars =>
//      if(msg.sender == currentWakeUpActor) {
////        println("Publishing " + lastPublishes.size.toString + " changes.")
//        lastPublishes.foreach( (item: Tuple2[Tuple2[Entity, Symbol], scala.swing.event.Event]) => publish(item._2) )
//        lastPublishes.clear
//        currentWakeUpActor = new WakeUpActor(((updateInterval * 1000.0).toLong, 0), this)
//        currentWakeUpActor.start
//      }
//  }
//
//  addHandler[ChangeUpdateInterval]{
//    msg: ChangeUpdateInterval =>
//      updateInterval = msg.newUpdateInterval
//      currentWakeUpActor = new WakeUpActor(((updateInterval * 1000.0).toLong, 0), this)
//      currentWakeUpActor.start
//  }
  //"Constructor Code END
  

  //Methods
  /**
   *  method to be implemented by a component. Will be called each time a svar provided or required by this
   * component was injected to an entity
   * @param sVarName the name of the sVar
   * @param sVar the actual sVar
   * @param cparam the set of create parameters provided to this component (defined in the aspect)
   */
  protected def handleNewSVar[T](sVarName : Symbol, sVar : SVar[T], e : Entity, cparam : NamedSValList) : Unit = {}

  /**
   *  method to be implemented by a component. Will be called each time the creation of an entity containing a
   * svar the component provided or requires has been completed
   * @param e the entity
   * @param cParam the set of create parameters used during the configuration process
   */
  protected def entityConfigComplete(e : Entity, cParam : NamedSValList) : Unit = {}

  override def shutdown = {
    super.shutdown
    if(gui != null)
    gui.collect({case v => v.visible = false})
    //If this is commented in, exit(0) will be invoked
    //gui.collect({case v => v.closeOperation})
  }

  //Methods END
}