package siris.components.renderer.setup

import siris.core.component.ComponentInitialization
import javax.media.opengl.GLProfile

/**
 * User: dwiebusch
 * Date: 26.05.11
 * Time: 09:58
 */

trait JOGLInit extends ComponentInitialization{
  protected def initComponent() {
    GLProfile.initSingleton(true)
  }
}