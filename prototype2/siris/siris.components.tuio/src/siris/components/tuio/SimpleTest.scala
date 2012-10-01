package siris.components.tuio

import TUIO._

/*
* Created by IntelliJ IDEA.
* User: martin
* Date: 6/24/11
* Time: 9:47 AM
*/

object SimpleTest {

  def main(args: Array[String]) {

    val client = new TuioClient
	  client.addTuioListener(new PrintlnListener)
	  client.connect()
  }
}


class PrintlnListener extends TuioListener {

  /**
   *  this method is called after each bundle,
   *  use it to repaint your screen for example
   */
  def refresh(ftime: TuioTime) {}

  /**
   * a cursor was removed from the table
   */
  def removeTuioCursor(tcur: TuioCursor) {
    println("[Cur][rem]" + tcur.getX + ", " + tcur.getY)
  }

  /**
   * a cursor was moving on the table surface
   */
  def updateTuioCursor(tcur: TuioCursor) {
    println("[Cur][upd]" + tcur.getX + ", " + tcur.getY)
  }

  /**
   * this is called when a new cursor is detected
   */
  def addTuioCursor(tcur: TuioCursor) {
    println("[Cur][add]" + tcur.getX + ", " + tcur.getY)
  }

  /**
   * an object was removed from the table
   */
  def removeTuioObject(tobj: TuioObject) {
    println("[Obj][rem]" + tobj.getX + ", " + tobj.getY)
  }

  /**
   * an object was moved on the table surface
   */
  def updateTuioObject(tobj: TuioObject) {
    println("[Obj][upd]" + tobj.getX + ", " + tobj.getY)
  }

  /**
   * this is called when an object becomes visible
   */
  def addTuioObject(tobj: TuioObject) {
    println("[Obj][add]" + tobj.getX + ", " + tobj.getY)
  }
}