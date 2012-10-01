package siris.core.component

import siris.core.SIRISApplication

/**
 * User: dwiebusch
 * Date: 26.05.11
 * Time: 09:59
 */

trait ComponentInitialization extends SIRISApplication{
  /**
   * do initialization of your component here
   */
  protected def initComponent()

  /**
   * This is to ensure other initialize functions are called, too
   */
  override protected def initialize() {
    super.initialize()
    initComponent()
  }
}