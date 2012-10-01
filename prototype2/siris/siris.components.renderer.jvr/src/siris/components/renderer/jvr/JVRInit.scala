package siris.components.renderer.jvr

import siris.components.renderer.setup.JOGLInit
import siris.core.svaractor.SVarActorImpl

/**
 * User: dwiebusch
 * Date: 26.05.11
 * Time: 11:01
 */

trait JVRInit extends JOGLInit{
  protected def exitOnClose( renderer : JVRConnector, shutdownMethod : Function0[Unit] ) {
    val self = SVarActorImpl.self
    renderer ! NotifyOnClose( self )
    self.addHandler[JVRRenderWindowClosed] {
      msg => shutdownMethod()
    }
  }
}