package siris.components.renderer.messages

import actors.Actor
import siris.core.svaractor.SIRISMessage
import siris.components.renderer.setup.DisplaySetupDesc
import siris.core.component.{Component, ComponentConfiguration}
import siris.core.entity.description.{Semantics, SValList}
import siris.ontology.Symbols

/**
 * This is the base class of all messages for the renderer.
 *
 * @author Stephan Rehfeld
 */
abstract class RendererMessage extends SIRISMessage

/**
 * This class sends a configuration to a rendering connector.
 *
 * @author Stephan Rehfeld
 *
 * @param sender The sender of the configure message.
 * @param displaySetup The display setup description that should be interpreted by the renderer to set up the windows.
 * @param autoRender A flag if the renderer should render by it self after frame has been finished or if it should wait until it receives a RenderNextFrame message.
 *
 */
case class ConfigureRenderer( sender: Actor, displaySetup: DisplaySetupDesc, autoRender: Boolean, effectsConfiguration : EffectsConfiguration ) extends RendererMessage

/**
 * This class represents a effects configuration. You can control if a effect is enabled and the quality of the effect.
 *
 *
 * @param shadowQuality The quality of the shadows. "none", "low", "middle" or "high"
 * @param mirrorQuality The quality of the mirrors. "none", "low", "middle" or "high"
 */
case class EffectsConfiguration( shadowQuality : String, mirrorQuality : String )

/**
 * This message signals a renderer connection to render the next frame.
 *
 * @author Stephan Rehfeld
 *
 * @param sender The sender of the message.
 */
case class RenderNextFrame( sender: Actor ) extends RendererMessage

/**
 * This message signals the renderer to change the auto render flag.
 *
 * @author Stephan Rehfeld
 *
 * @param autoRender The desired value for the auto render flag.
 * @param sender The sender of the message.
 */
case class ToggleAutoRender( sender: Actor, autoRender: Boolean ) extends RendererMessage

/**
 * This message signals the renderer to pause the rendering and only continue after a resume has been received.
 *
 * @author Stephan Rehfeld
 *
 * @param sender The sender of the message.
 */
case class PauseRenderer( sender: Actor ) extends RendererMessage

/**
 * This message signals a renderer to resume rendering.
 *
 * @author Stephan Rehfeld
 *
 * @param sender The sender of the message.
 */
case class ResumeRenderer( sender: Actor ) extends RendererMessage

/**
 * This message signals a renderer to notify the sender of the message about rendering steps.
 *
 * @author Stephan Rehfeld
 *
 * @param sender The sender of the message.
 */
case class SubscribeForRenderSteps( sender: Actor ) extends RendererMessage

/**
 * This message signals a renderer to not notify the sender anymore about rendering steps.
 *
 * @author Stephan Rehfeld
 *
 * @param sender The sender of the message.
 */
case class UnsubscribeForRenderSteps( sender: Actor ) extends RendererMessage

/**
 * This message is send by the renderer to signal that a new frame is now rendered.
 *
 * @author Stephan Rehfeld
 *
 * @param sender The sender of the message.
 */
case class StartingFrame( sender: Actor ) extends RendererMessage

/**
 * This message is send by the renderer to signal that a new frame has been rendered.
 *
 * @author Stephan Rehfeld
 *
 * @param sender The sender of the message.
 */
case class FinishedFrame( sender: Actor ) extends RendererMessage
