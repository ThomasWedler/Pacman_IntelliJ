package siris.components.renderer.jvr

import actors.Actor
import de.bht.jvr.util.Color
import siris.core.entity.Entity
import siris.components.renderer.messages.RendererMessage
import siris.core.entity.description.{SValList, EntityAspect}
import siris.core.entity.component.Removability

class JVRMessage

/**
 * This messages is used by the JVRConnector to publish a entity with the create param to the render actors.
 *
 * @param sender The sender of the message.
 * @param e The entity.
 * @param aspect the entity aspect
 */
case class PublishSceneElement( sender: Actor, e : Entity with Removability, aspect : EntityAspect ) extends RendererMessage

/**
 * This messages is used by the JVRConnector to remove an entity
 *
 * @param sender The sender of the message.
 * @param e The entity.
 */
case class RemoveSceneElement( sender: Actor,e : Entity ) extends RendererMessage

/**
 * Tells JVR to set the ambient color for all windows.
 *
 * @param sender The sender of the message.
 * @param color The new ambient color.
 */
case class SetAmbientColor( sender: Actor, color: Color ) extends RendererMessage

/**
 * Sending this message will regroup an entity within the internal scene graph.
 *
 * @param sender The sender of the message.
 * @param e The entity that should be regrouped
 * @param target An optional target. Passing none will group the entity under the root node. Default value is None.
 * @param convertTransform It true the world position of the entity will stay the same. The transformation matrix will be converted. Default value is true.
 *
 */
case class RegroupEntity( sender: Actor, e: Entity, target: Option[Entity] = None, convertTransform: Boolean = true ) extends RendererMessage

/**
 * this message will be sent when the regrouping operation has been applied
 *
 * @param e The entity that should be regrouped
 * @param target An optional target. See RegroupEntity
 */
case class RegroupApplied(e : Entity, target : Option[Entity])

/**
 * This message can be sent to the JVRConnector to request all keyboards of the managed windows.
 * It will be answered by a Keyboards message.
 *
 * @param sender The sender of the message. The answer will be sent to this sender.
 */
case class GetKeyboards( sender: Actor ) extends RendererMessage

/**
 * This message is an answer to the GetKeyboards message. It contains the requested keyboards as entites.
 *
 * @param sender The sender of the message. Typically the JVRConnector.
 * @param keyboards A list of all Keyboard entities of the managed windows.
 */
case class Keyboards( sender: Actor, keyboards: List[Entity] ) extends RendererMessage


/**
 * This message can be sent to the JVRConnector to request all mouses of the managed windows.
 * It will be answered by a Mouses message.
 *
 * @param sender The sender of the message. The answer will be sent to this sender.
 */
case class GetMouses( sender: Actor ) extends RendererMessage

/**
 * This message is an answer to the GetMouses message. It contains the requested mouses as entites.
 *
 * @param sender The sender of the message. Typically the JVRConnector.
 * @param mouses A list of all Mouses entities of the managed windows.
 */
case class Mouses( sender: Actor, mouses: List[Entity] ) extends RendererMessage

/**
 * This message requests all created user entities. The number of user entities depends on the display setup.
 * Each user entity has two state variables. The view platform and the head transformation.
 *
 * @param sender The sender of the message. The answer will be send to this sender.
 */
case class GetUser( sender: Actor ) extends RendererMessage

/**
 * This message is an answer to the User message. It contains the request user.
 * The number of user entities depends on the display setup.
 * Each user entity has two state variables. The view platform and the head transformation.
 *
 * @param sender The sender of the message. Typically the JVRConnector
 * @param user The list of users.
 */
case class User( sender: Actor, user: List[Entity] ) extends RendererMessage

/**
 * This messages signals the JVRConnector to notify the sender if one render window has been closed.
 *
 * @param sender The sender.
 */
case class NotifyOnClose( sender: Actor ) extends RendererMessage

case class SVarHandled( sender: Actor ) extends RendererMessage

case class DetachObject( sender: Actor, e : Entity ) extends RendererMessage

case class AttachObject( sender: Actor, e : Entity ) extends RendererMessage

case class JVRRenderActorConfigCompleted( sender: Actor ) extends RendererMessage

case class CreateMesh( sender : Actor, e : Entity, asp : EntityAspect, given : SValList, ready : SValList, configActor : Actor)

case class MeshCreated(configActor : Actor, initialValues : SValList )