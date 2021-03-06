package siris.core.svaractor.test

import org.scalatest.matchers.ShouldMatchers
import org.scalatest.Spec
import actors.Actor
import siris.core.svaractor.{SVar, SVarObjectInterface, SVarImpl, SVarActorHW}

/**
 * Created by IntelliJ IDEA.
 * User: stephan_rehfeld
 * Date: 25.08.2010
 * Time: 12:29:01
 * To change this template use File | Settings | File Templates.
 */




class SVarActorSpec extends Spec with ShouldMatchers {


  val sVarObject : SVarObjectInterface = SVarImpl
  val sVarInitValue : SVarDataType = "Hallo"
  val sVarWriteValue : SVarDataType = "Stephan"

  class SVarActorImplementation extends SVarActorHW
  type SVarDataType = String

  val ownerChangeRetryLimit: Int = 10000
  val waitForPongLimit: Int = 10000

  case class OwnerChangeRetry( sender: Actor, count: Int, sVars: List[SVar[SVarDataType]], a: Actor )
  case class WaitForPong( sender: Actor, count: Int )
  case class Ping( sender: Actor, reply: Actor )
  case class Pong( sender: Actor )

  describe( "An SVarActor" ) {

    describe( "should be shutdownable" ) {

      describe( "by it self" ) {

        it( "and give away all SVars" ) {
          class TestActor extends SVarActorImplementation{

            case class Create( sender: Actor )
            case class SVarList( sVars: List[SVar[SVarDataType]])
            case class Observe( sender: Actor, sVars: List[SVar[SVarDataType]] )
            case class Observed( sVars: List[SVar[SVarDataType]] )

            var a1: SVarActorImplementation = null
            var a2: SVarActorImplementation = null
            var s: Actor = null

            addHandler[StartTest]{
              case StartTest( sender ) =>


                class TestActor1 extends SVarActorImplementation {
                  addHandler[Create](
                    {
                      case Create( sender ) =>
                        val sVars = for(  x <- List.range(1, 10 ) ) yield sVarObject( sVarInitValue )
                        sender ! SVarList( sVars )

                    }
                    )
                }

                class TestActor2 extends SVarActorImplementation {
                  addHandler[Observe](
                    {
                      case Observe( sender, sVars ) =>
                        for( sVar <- sVars) sVar.observe(
                          (v: SVarDataType) => {
                          }
                          )
                        sender ! Observed( sVars )

                    }
                    )
                }

                a1 = new TestActor1
                a1.start

                a2 = new TestActor2
                a2.start

                a1 ! Create( Actor.self )
                s = sender
            }

            addHandler[SVarList]{
              case SVarList( sVars ) =>
                a2 ! Observe( Actor.self, sVars )
            }

            addHandler[Observed]{
              case Observed( sVars ) =>
                a1.shutdown
                Actor.self ! OwnerChangeRetry( s, ownerChangeRetryLimit, sVars, a2 )
            }

            addHandler[OwnerChangeRetry]{
              case OwnerChangeRetry( sender, 0, sVars, a ) =>
                println( "Owners not changed" )
                sender ! TestFinished( Actor.self, false )


              case OwnerChangeRetry( sender, count, sVars, a ) =>
                var allOkay = true
                for( sVar <- sVars )
                  if( sVar.owner != a ) {
                    allOkay = false
                  }
                if( allOkay ) {
                  sender ! TestFinished( Actor.self, true )
                } else
                  Actor.self ! OwnerChangeRetry( sender, count-1, sVars, a )


            }

          }
          val sVarActor = new TestActor
          sVarActor.start
          sVarActor ! StartTest( Actor.self )
          Actor.self.receive(
            {
              case TestFinished( sender, successful ) => assert( successful )
            }
            )
        }

        it( "remove all handler and not react any more" ) {
          class TestActor extends SVarActorImplementation {

            case class Create( sender: Actor )
            case class SVarList( sVars: List[SVar[SVarDataType]])
            case class Observe( sender: Actor, sVars: List[SVar[SVarDataType]] )
            case class Observed( sVars: List[SVar[SVarDataType]] )



            var a1: SVarActorImplementation = null
            var a2: SVarActorImplementation = null
            var s: Actor = null

            addHandler[StartTest]
                      {
                        case StartTest( sender ) =>


                          class TestActor1 extends SVarActorImplementation {
                            addHandler[Create](
                              {
                                case Create( sender ) =>
                                  val sVars = for(  x <- List.range(1, 10 ) ) yield sVarObject( sVarInitValue )
                                  sender ! SVarList( sVars )
                              }
                              )

                            addHandler[Ping](
                              {
                                case Ping( sender, reply ) =>
                                  reply ! Pong( sender )
                              }
                              )
                          }

                          class TestActor2 extends SVarActorImplementation {
                            addHandler[Observe](
                              {
                                case Observe( sender, sVars ) =>
                                  for( sVar <- sVars) sVar.observe(
                                    (v: SVarDataType) => {
                                    }
                                    )
                                  sender ! Observed( sVars )

                              }
                              )
                          }

                          a1 = new TestActor1
                          a1.start

                          a2 = new TestActor2
                          a2.start

                          a1 ! Create( Actor.self )
                          s = sender
                      }
            addHandler[SVarList]{
              case SVarList( sVars ) =>
                a2 ! Observe( Actor.self, sVars )
            }

            addHandler[Observed]{
              case Observed( sVars ) =>
                a1.shutdown
                Actor.self ! OwnerChangeRetry( s, ownerChangeRetryLimit, sVars, a2 )
            }

            addHandler[OwnerChangeRetry]{
              case OwnerChangeRetry( sender, 0, sVars, a ) =>
                println( "Owners not changed" )
                sender ! TestFinished( Actor.self, false )


              case OwnerChangeRetry( sender, count, sVars, a ) =>
                var allOkay = true
                for( sVar <- sVars )
                  if( sVar.owner != a ) {
                    allOkay = false
                  }
                if( allOkay ) {

                  a1 ! Ping( sender, Actor.self )
                  Actor.self ! WaitForPong( sender, waitForPongLimit )
                } else
                  Actor.self ! OwnerChangeRetry( sender, count-1, sVars, a )
            }

            addHandler[Pong]{
              case Pong( sender ) =>
                sender ! TestFinished( Actor.self, false )
            }

            addHandler[WaitForPong]{
              case WaitForPong( sender, 0 ) =>
                sender ! TestFinished( Actor.self, true )

              case WaitForPong( sender, count ) =>
                Actor.self ! WaitForPong( sender, count-1 )
            }
          }

          val sVarActor = new TestActor
          sVarActor.start
          sVarActor ! StartTest( Actor.self )
          Actor.self.receive(
            {
              case TestFinished( sender, successful ) => assert( successful )
            }
            )
        }
      }
      describe( "from outside" ) {
        it( "and give away all SVars" ) {
          class TestActor extends SVarActorImplementation {
            addHandler[StartTest]{
              case StartTest( sender ) =>
                val a = new SVarActorImplementation
                a.start
                val sVars = for(  x <- List.range(1, 10 ) ) yield sVarObject( sVarInitValue, a )
                for( sVar <- sVars) sVar.observe(
                  (v: SVarDataType) => {
                  }
                  )

                a.shutdown
                Actor.self ! OwnerChangeRetry( sender, ownerChangeRetryLimit, sVars, Actor.self )
            }
            addHandler[OwnerChangeRetry]{
              case OwnerChangeRetry( sender, 0, sVars, a ) =>
                println( "Owners not changed" )
                sender ! TestFinished( Actor.self, false )


              case OwnerChangeRetry( sender, count, sVars, a ) =>
                var allOkay = true
                for( sVar <- sVars )
                  if( sVar.owner != a ) {
                    allOkay = false
                    sVar.set( sVarWriteValue )
                  }
                if( allOkay ) {
                  sender ! TestFinished( Actor.self, true )
                } else
                  Actor.self ! OwnerChangeRetry( sender, count-1, sVars, a )


            }
          }
          val sVarActor = new TestActor
          sVarActor.start
          sVarActor ! StartTest( Actor.self )
          Actor.self.receive(
            {
              case TestFinished( sender, successful ) => assert( successful )
            }
            )
        }

        it( "remove all handler and not react any more" ) {
          class TestActor extends SVarActorImplementation {
            addHandler[StartTest]{
              case StartTest( sender ) =>
                class TestActor1 extends SVarActorImplementation {
                  addHandler[Ping](
                    {
                      case Ping( sender, reply ) =>
                        reply ! Pong( sender )
                    }
                    )
                }

                val a = new TestActor1
                a.start

                val sVars = for(  x <- List.range(1, 10 ) ) yield sVarObject( sVarInitValue, a )
                for( sVar <- sVars) sVar.observe(
                  (v: SVarDataType) => {
                  }
                  )

                a.shutdown
                Actor.self ! OwnerChangeRetry( sender, ownerChangeRetryLimit, sVars, Actor.self )
            }
            addHandler[OwnerChangeRetry]{
              case OwnerChangeRetry( sender, 0, sVars, a ) =>
                println( "Owners not changed" )
                sender ! TestFinished( Actor.self, false )


              case OwnerChangeRetry( sender, count, sVars, a ) =>
                var allOkay = true
                for( sVar <- sVars )
                  if( sVar.owner != a ) {
                    allOkay = false
                    sVar.set( sVarWriteValue )
                  }
                if( allOkay ) {
                  sender ! TestFinished( Actor.self, true )
                  a ! Ping( sender, Actor.self )
                  Actor.self ! ( sender, waitForPongLimit )
                } else
                  Actor.self ! OwnerChangeRetry( sender, count-1, sVars, a )
            }
            addHandler[Pong]{
              case Pong( sender ) =>
                sender ! TestFinished( Actor.self, false )
            }
            addHandler[WaitForPong]{
              case WaitForPong( sender, 0 ) =>
                sender ! TestFinished( Actor.self, true )

              case WaitForPong( sender, count ) =>
                Actor.self ! WaitForPong( sender, count-1 )
            }
          }

          val sVarActor = new TestActor
          sVarActor.start
          sVarActor ! StartTest( Actor.self )
          Actor.self.receive(
            {
              case TestFinished( sender, successful ) => assert( successful )
            }
            )
        }
      }
    }
  }
}