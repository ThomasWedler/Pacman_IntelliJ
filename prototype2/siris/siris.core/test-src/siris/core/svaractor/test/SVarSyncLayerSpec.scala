package siris.core.svaractor.test

import org.scalatest.matchers.ShouldMatchers
import org.scalatest.Spec
import actors.Actor
import siris.core.svaractor._
import synclayer._
import concurrent.SyncVar

/**
 * Created by IntelliJ IDEA.
 * User: stephan_rehfeld
 * Date: 25.08.2010
 * Time: 12:29:51
 * To change this template use File | Settings | File Templates.
 */

case class Failed( sender : Actor, reason : String )
case class Finished( sender : Actor )
case class StartTest2( sender : Actor )
case class SendValue[T]( allSVars : List[SVar[Int]], toCover : List[SVar[Int]], value : T )
case class PublishSvars( sender : Actor, sVars :  List[SVar[Int]])
case class ObserveRequest( sender : Actor,  toObserve : SVarActorImpl with SVarSyncLayer )
case class IgnoreRequest( sender : Actor, toIgnore : SVarActorImpl with SVarSyncLayer )

abstract class TestActor(numberOfSyncs : Int = 2000, numberOfSVars : Int = 250) extends SVarActorHW with SVarSyncLayer {
  type SVarList =  List[SVar[Int]]

  var numberOfSynchedSVars = 0
  var observerGotSyncNr = 0
  var currentCount = 0

  var tmpMap = Map[SVar[Int], Int]()
  var maxVal = 0
  var maxValAtIgnore = 0
  var someSVars = new SyncVar[SVarList]()
  var moreSVars = new SyncVar[SVarList]()
  var myParent : Actor = this

  val observingActor = SVarActorImpl.actor{
    SVarActorImpl.self.addHandler[PublishSvars]{
      case PublishSvars(sender, sVars) =>
        myParent = sender
        sVars.foreach( (a) => a.observe{
          (i : Int) =>
            tmpMap = tmpMap.updated(a, i)
            maxVal = i
            if (maxValAtIgnore > 0 && maxVal > maxValAtIgnore + numberOfSyncs)
              myParent ! Finished( Actor.self )
        } )
    }

    SVarActorImpl.self.addHandler[ObserveRequest]{
      case ObserveRequest(sender, toObserve) =>
        toObserve.observe
    }

    SVarActorImpl.self.addHandler[IgnoreRequest]{
      case IgnoreRequest(sender, toIgnore) =>        
        tmpMap = Map[SVar[Int], Int]()
        maxValAtIgnore = maxVal
        toIgnore.ignore
    }

    SVarActorImpl.self.addHandler[SyncMessage]{
      case SyncMessage( sender ) =>
        observerGotSyncNr += 1
        if (! tmpMap.foldLeft((true, tmpMap.head._2))( (b, a) => (a._2 == b._2 && b._1, a._2) )._1 )
          myParent ! Failed(Actor.self, "failed because of inconsistend world state " + observerGotSyncNr)
        if (observerGotSyncNr == numberOfSVars){
          myParent ! Finished( Actor.self )
          observerGotSyncNr = 0
        }
    }
  }

  addHandler[StartTest]{
    case StartTest( sender ) => {
      someSVars.set((for ( i <- 0 until numberOfSVars/2 ) yield SVarImpl(0)).toList)
      moreSVars.set((for ( i <- 0 until numberOfSVars/2 ) yield SVarImpl(0)).toList)
      val allSVars = someSVars.get ::: moreSVars.get
      Actor.self ! SendValue(allSVars, allSVars, 0)
    }
  }

  addHandler[SendValue[_]]{
    case SendValue(allVars, head :: tail, value : Int ) => {
      head.set(value)
      Actor.self ! SendValue(allVars, tail, value)
    }
    case SendValue(allVars, Nil, value : Int ) =>
      push
      Actor.self ! SendValue(allVars, allVars, value + 1)
  }
}

class SVarSyncLayerSpec extends Spec with ShouldMatchers {
  private def waitForResult = Actor.self.receive {
    case Failed(sender, reason) => fail( reason )
    case Finished(sender)       => assert( true )
  }

  var testActor = new TestActor() with PluggableSyncLayer

  describe( "A SVarActor with Sync Layer" ) {
    testActor.start
    testActor.observingActor.start
    testActor ! StartTest( Actor.self )

    it( "should be observable and push consistent worldstates") {
      testActor.observingActor ! PublishSvars( Actor.self, testActor.someSVars.get )
      Thread.sleep(200)
      testActor.observingActor ! ObserveRequest( Actor.self, testActor )
      waitForResult
    }
    it("should accept more SVars, which are immedeately synched, too") {
      testActor.observingActor ! ObserveRequest( Actor.self, testActor )
      Thread.sleep(200)
      testActor.observingActor ! PublishSvars( Actor.self, testActor.moreSVars.get )
      waitForResult      
    }
    it("should be able to use ignore functionality and still receive (unsynched) messages") {
      testActor.observingActor ! IgnoreRequest( Actor.self, testActor )
      waitForResult
    }
    it ("should be able to shutdown ;-)"){
      testActor.observingActor.shutdown
      testActor.shutdown
      assert( true )
    }
  }
}




//
//    it( "should be ignoreable after observation" )  {
//      class TestActor extends SVarActorImplementation {
//
//        var writeAllowed : Boolean = true;
//
//        var error : Boolean = false
//
//        var a : SVarActor with SVarSyncLayer = null
//        var sVarCorrect: Int = 0
//        var sVars : List[SVar[SVarDataType]] = null
//
//
//        addHandler(
//          {
//            case StartTest( sender ) =>
//              class TestActor1 extends SVarActorImplementation {
//
//                addHandler( {
//                  case StartPush( sender ) =>
//                    push
//                }
//                  )
//
//              }
//
//              a = new TestActor1
//              a.start
//              sVars = for(  x <- List.range(1, sVarsAmount ) ) yield sVarObject( sVarInitValue, a )
//              for( sVar <- sVars ) sVar.observe(
//                (v: SVarDataType) => {
//                  println( "Written" )
//                  if( !writeAllowed ) {
//                    error = true
//                  } else {
//                    if( v == sVarWriteValue ) {
//                      sVarCorrect += 1
//                    }
//                  }
//                }
//                )
//              a.observe
//              writeAllowed = false
//              for(  x <- List.range(1, sVarsAmount ) ) {
//                sVars( x-1 ).set( sVarWriteValue )
//              }
//              Actor.self ! WaitForWrite( sender, writeAllowedLimit )
//
//            case WaitForWrite( sender, 0 ) =>
//              println( "WaitForWrite( sender, 0 )" )
//              if( error ) {
//                println( "Test failed because the write notification were send before the push (2)" )
//                sender ! TestFinished( Actor.self, false )
//              } else {
//                writeAllowed = true
//                a ! StartPush( Actor.self )
//                Actor.self ! WaitForCompletePush( sender, waitForCompletePushLimit )
//              }
//
//            case WaitForWrite( sender, count ) =>
//              println( "WaitForWrite( sender, count )" )
//              if( error ) {
//                println( "Test failed because the write notification were send before the push (1)" )
//                sender ! TestFinished( Actor.self, false )
//              } else {
//                Actor.self ! WaitForWrite( sender, count-1 )
//              }
//
//            case WaitForCompletePush( sender, 0 ) =>
//              println( "Hallo" )
//              if( sVarCorrect == sVarsAmount ) {
//                sVarCorrect = 0
//                a.ignore
//                writeAllowed = true
//                for(  x <- List.range(1, sVarsAmount ) ) {
//                  sVars( x-1 ).set( sVarInitValue )
//                  sVars( x-1 ).set( sVarWriteValue )
//                }
//                Actor.self ! WaitForCompleteWrite( sender, waitForCompleteWriteLimit )
//              } else {
//                println( "Test failed because not all state variables has been written" )
//                sender ! TestFinished( Actor.self, false )
//              }
//
//            case WaitForCompletePush( sender, count ) =>
//              if( sVarCorrect == sVarsAmount ) {
//                sVarCorrect = 0
//                a.ignore
//                writeAllowed = true
//                for(  x <- List.range(1, sVarsAmount ) ) {
//                  sVars( x-1 ).set( sVarInitValue )
//                  sVars( x-1 ).set( sVarWriteValue )
//                }
//                Actor.self ! WaitForCompleteWrite( sender, waitForCompleteWriteLimit )
//
//              } else {
//                Actor.self ! WaitForCompletePush( sender, count-1 )
//              }
//
//            case WaitForCompleteWrite( sender, 0 ) =>
//              sender ! TestFinished( Actor.self, sVarCorrect == 10 )
//
//            case WaitForCompleteWrite( sender, count ) =>
//              if( sVarCorrect == sVarsAmount ) {
//                sender ! TestFinished( Actor.self, true )
//              } else {
//                Actor.self ! WaitForCompleteWrite( sender, count-1 )
//              }
//
//          }
//          )
//      }
//
//      val sVarActor = new TestActor
//      sVarActor.start
//      sVarActor ! StartTest( Actor.self )
//      Actor.self.receive(
//        {
//          case TestFinished( sender, successful ) => assert( successful )
//        }
//        )
//    }
//

