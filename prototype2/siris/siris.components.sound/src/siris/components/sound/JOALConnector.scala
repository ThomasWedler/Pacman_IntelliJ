//package siris.components.sound
//
///**
// * @author dwiebusch
// * @date: 24.09.2010
// */
//
//
//
//import siris.core.svaractor.{SVar, SVarActorHW}
//import siris.core.entity.description._
//import siris.core.entity.Entity
//import siris.core.component.Component
//import actors.Actor
//import siris.core.svaractor.synclayer.SVarSyncLayer
//import siris.components.physics.jbullet.{JBRegisterForCollision, JBEntityCollided}
//import siris.mmi.network.SimpleDirtyNetworkConnection
//import siris.core.entity.component.Removability
//
//object OpenALConnector {
////  def main(args: Array[String]): Unit = {
////
////    val oAL = new OpenAL(6,6)
////    println("1")
////    // Initialize OpenAL and clear the error bit.
////    try {
////      if(oAL.initOpenAL() == ALConstants.AL_FALSE) {
////        System.err.println("Could not initialize");
////        System.exit(1);
////      }
////    } catch {
////      case e : ALException => {
////        e.printStackTrace();
////        System.exit(1);
////      }
////
////    }
////    println("2")
////    // Load the wav data.
////    if (oAL.loadData("sounds/FancyPants.wav",0,0,0, false) == ALConstants.AL_FALSE){
////      System.err.println("Could not load data");
////      System.exit(1);
////    }
////
////    if (oAL.loadData("sounds/Gun1.wav",1,1,0, false) == ALConstants.AL_FALSE){
////      System.err.println("Could not load data");
////      System.exit(1);
////    }
////    println("3")
////    oAL.setListenerValues()
////    println("4")
////    //ALut.alutInit();
////    //al.alGetError();
////
////    // addSource(0)
////
////    var c = Array[Char](0)
////    while (c(0) != 'q') {
////      try {
////        val buf =
////        new java.io.BufferedReader(new java.io.InputStreamReader(System.in));
////        System.out.println(
////          "Press a key and hit ENTER: \n"
////                  + "'p' to play, 's' to stop, " +
////                  "'h' to pause and 'q' to quit");
////        buf.read(c);
////        c(0) match {
////          case 'p' =>
////            // Pressing 'p' will begin playing the sample.
////            //addSource(0)
////            //al.alSourcePlay(sources(0));
////            oAL.playSource(0)
////          case 's' =>
////            oAL.playSource(1)
////          // Pressing 's' will stop the sample from playing.
////          //al.alSourceStop(sources(0));
////          case 'h' =>
////            // Pressing 'n' will pause (hold) the sample.
////            al.alSourcePause(sources(0));
////          case 'q' => killAllData();
////        }
////      } catch {
////        case e : java.io.IOException =>  System.exit(1);
////      }
////    }
////    println("finished")
////
////    //    println("1")
////    //    // Initialize OpenAL and clear the error bit.
////    //    try {
////    //      if(initOpenAL() == ALConstants.AL_FALSE) {
////    //        System.err.println("Could not initialize");
////    //        System.exit(1);
////    //      }
////    //    } catch {
////    //       case e : ALException => {
////    //         e.printStackTrace();
////    //         System.exit(1);
////    //       }
////    //
////    //    }
////    //    println("2")
////    //   // Load the wav data.
////    //    if (loadALData() == ALConstants.AL_FALSE){
////    //      System.err.println("Could not load data");
////    //      System.exit(1);
////    //    }
////    //    println("3")
////    //    setListenerValues()
////    //    println("4")
////    //    //ALut.alutInit();
////    //    //al.alGetError();
////    //
////    //   // addSource(0)
////    //
////    //    var c = Array[Char](0)
////    //    while (c(0) != 'q') {
////    //      try {
////    //        val buf =
////    //        new java.io.BufferedReader(new java.io.InputStreamReader(System.in));
////    //        System.out.println(
////    //          "Press a key and hit ENTER: \n"
////    //                  + "'p' to play, 's' to stop, " +
////    //                  "'h' to pause and 'q' to quit");
////    //        buf.read(c);
////    //        c(0) match {
////    //          case 'p' =>
////    //            // Pressing 'p' will begin playing the sample.
////    //            //addSource(0)
////    //            al.alSourcePlay(sources(0));
////    //          case 's' =>
////    //            // Pressing 's' will stop the sample from playing.
////    //            al.alSourcePlay(sources(1));
////    //          case 'h' =>
////    //            // Pressing 'n' will pause (hold) the sample.
////    //            al.alSourcePause(sources(0));
////    //          case 'q' => killAllData();
////    //        }
////    //      } catch {
////    //        case e : java.io.IOException =>  System.exit(1);
////    //      }
////    //    }
////    //    println("finished")
////  }
////
////  var alc : ALC = null
////  var al : AL = null
////  val NUM_BUFFERS = 6
////  val NUM_SOURCES = 6
////
////  // Buffers hold sound data.
////  val buffers = new Array[Int](NUM_BUFFERS);
////
////  // Sources are points emitting sound.
////  val sources = new Array[Int](NUM_SOURCES);
////
////  // Position of the source sound.
////  val sourcePos = (for (i <- 0 to NUM_SOURCES) yield new Array[Float](3)).toArray
////
////  // Velocity of the source sound.
////  val sourceVel = (for (i <- 0 to NUM_SOURCES) yield new Array[Float](3)).toArray
////
////  // Position of the listener.
////  val listenerPos = Array[Float]( 0.0f, 0.0f, 0.0f );
////
////  // Velocity of the listener.
////  val listenerVel = Array[Float]( 0.0f, 0.0f, 0.0f );
////
////  // Orientation of the listener. (first 3 elems are "at", second 3 are "up")
////  val listenerOri = Array[Float]( 0.0f, 0.0f, -1.0f, 0.0f, 1.0f, 0.0f );
////
////
////
////  def initOpenAL() : Int = {
////    alc = ALFactory.getALC()
////    al = ALFactory.getAL()
////
////    // var deviceName = "DirectSound3D"
////
////    // Get handle to device.
////    var device = alc.alcOpenDevice(null)
////
////    // Get the device specifier.
////    //    println("Using device " + deviceSpecifier);
////
////    // Create audio context.
////    var context = alc.alcCreateContext(device, null);
////
////    // Set active context.
////    alc.alcMakeContextCurrent(context);
////
////    // Check for an error.
////    if (alc.alcGetError(device) != ALCConstants.ALC_NO_ERROR)
////      return ALConstants.AL_FALSE;
////
////    return ALConstants.AL_TRUE;
////  }
////
////  def loadALData() : Int = {
////
////    // variables to load into
////    var format  = Array[Int](0);
////    var size    = Array[Int](0);
////    var data    = Array(java.nio.ByteBuffer.allocate(1024));
////    var freq    = Array[Int](0);
////    var loop    = Array[Int](0);
////
////    // Load wav data into buffers.
////    al.alGenBuffers(NUM_BUFFERS, buffers, 0)
////    if (al.alGetError() != ALConstants.AL_NO_ERROR)
////      return ALConstants.AL_FALSE;
////
////    ALut.alutLoadWAVFile(
////      "sounds/FancyPants.wav",
////      format,
////      data,
////      size,
////      freq,
////      loop);
////    al.alBufferData( buffers(0), format(0), data(0), size(0), freq(0));
////    //ALut.alutUnloadWAV(format(0), data(0), size(0), freq(0));
////
////    ALut.alutLoadWAVFile(
////      "sounds/Gun1.wav",
////      format,
////      data,
////      size,
////      freq,
////      loop);
////
////    al.alBufferData( buffers(1), format(0), data(0), size(0), freq(0));
////
////    // bind buffers into audio sources
////    al.alGenSources(NUM_SOURCES, sources, 0);
////
////    al.alSourcei(sources(0), ALConstants.AL_BUFFER, buffers(0));
////    al.alSourcef(sources(0), ALConstants.AL_PITCH, 1.0f);
////    al.alSourcef(sources(0), ALConstants.AL_GAIN, 1.0f);
////    al.alSourcefv(sources(0), ALConstants.AL_POSITION, sourcePos(0), 0);
////    al.alSourcefv(sources(0), ALConstants.AL_POSITION, sourceVel(0), 0);
////    al.alSourcei(sources(0), ALConstants.AL_LOOPING, ALConstants.AL_FALSE);
////
////    al.alSourcei(sources(1), ALConstants.AL_BUFFER, buffers(1));
////    al.alSourcef(sources(1), ALConstants.AL_PITCH, 1.0f);
////    al.alSourcef(sources(1), ALConstants.AL_GAIN, 1.0f);
////    al.alSourcefv(sources(1), ALConstants.AL_POSITION, sourcePos(1), 0);
////    al.alSourcefv(sources(1), ALConstants.AL_POSITION, sourceVel(1), 0);
////    al.alSourcei(sources(1), ALConstants.AL_LOOPING, ALConstants.AL_FALSE);
////
////    if (al.alGetError() != ALConstants.AL_NO_ERROR)
////      return ALConstants.AL_FALSE;
////
////    return ALConstants.AL_TRUE
////  }
////
//// def setListenerValues()  = {
////    al.alListenerfv(ALConstants.AL_POSITION, listenerPos, 0);
////    al.alListenerfv(ALConstants.AL_VELOCITY, listenerVel, 0);
////    al.alListenerfv(ALConstants.AL_ORIENTATION, listenerOri, 0);
////  }
////
////  def killAllData() = {
////    //TODO delete sources correctly
////    //al.alDeleteSources(1, sources, 0)
////    al.alDeleteBuffers(NUM_BUFFERS, buffers, 0);
////
////    ALut.alutExit();
////  }
//
//}
//
//case class PlaySoundRequest( reason : Symbol )
//
//object JOALAnywhere{
//  def main( args : Array[String] ) : Unit = {
//    println("starting joal server")
//    val net = new SimpleDirtyNetworkConnection(12340)
//    val connector = new OpenALConnector()
//    connector.start
//    net.init
//    net.read{ s : String =>
//      if (s.trim == "quit")
//        net.close
//      else
//        connector ! PlaySoundRequest( Symbol(s) )
//    }
//  connector.shutdown
//  }
//}
//
////case class OpenALCreateParam( entityName : Symbol) extends SVal
//
//class OpenALConnector( val componentName : Symbol = 'alSound ) extends SVarActorHW with Component{
//  val FIREBLAST  = 0
//  val ICEBLAST   = 1
//  val GHOSTCRIES = 2
//  val SHIELD     = 3
//  val BACKGROUND = 4
//
//  val oAL = new OpenAL(50,5)
//  // Initialize OpenAL and clear the error bit.
//
//
//
//  if(oAL.initOpenAL() == ALConstants.AL_FALSE) {
//    throw new Exception("Could not initialize")
//  }
//
//  // Load the wav data.
//  if (oAL.loadData("sounds/iceblast.wav",ICEBLAST,ICEBLAST,9, false) == ALConstants.AL_FALSE){
//    throw new Exception("Could not load wav-data");
//  }
//
//  if (oAL.loadData("sounds/fireblast.wav",FIREBLAST,FIREBLAST,9, false) == ALConstants.AL_FALSE){
//    throw new Exception("Could not load wav-data");
//  }
//
//  if (oAL.loadData("sounds/ghost.wav",GHOSTCRIES,GHOSTCRIES,9, false) == ALConstants.AL_FALSE){
//    throw new Exception("Could not load wav-data");
//  }
//
//    if (oAL.loadData("sounds/shield.wav",SHIELD,SHIELD,0, true) == ALConstants.AL_FALSE){
//    throw new Exception("Could not load wav-data");
//  }
//      if (oAL.loadData("sounds/background-thief.wav",BACKGROUND,BACKGROUND,0, true) == ALConstants.AL_FALSE){
//    throw new Exception("Could not load wav-data");
//  }
//
//  oAL.setListenerValues()
//  //oAL.playSource(BACKGROUND*10)
//
//  def removeFromLocalRep(e: Entity) = {}
//
//  override def shutdown = {
//    super.shutdown
//    oAL.exitOpenAL
//  }
//
//
//  override def startUp = {
//    println{"init sound"}
//    oAL.playSource(BACKGROUND*10)}
//
//  def handleNewSVar[T](sVarName: Symbol, sVar: SVar[T], e: Entity, cparam: NamedSValList) = {}
//
//  protected def entityConfigComplete(e: Entity, cParam: NamedSValList) = { }
//
//  addHandler[PlaySoundRequest]{ msg =>
//    msg.reason match {
//      case 'iceCollision => iceCollision
//      case 'fireCollision => fireCollision
//      case 'shieldOn => shieldOn
//      case 'shieldHit => shieldHit
//      case 'shieldOff => shieldOff
//      case 'ghostBurned => ghostBurned
//      case 'ghostFrozen => ghostFrozen
//      case 'castIceSpell => castIceSpell
//      case 'castFireSpell => castFireSpell
//      case _ =>
//    }
//  }
//
//  //TODO: fill the methods below with code
//  def ghostBurned : Unit = {
//    //println("burned")
//    var it = GHOSTCRIES*10
//    //println("fire")
//    while (!oAL.playSource(it)) {
//      it = it+1
//      if (it > GHOSTCRIES*10 + 9) return
//    }
//  }
//
//  def ghostFrozen : Unit = {
//
//  }
//
//  def shieldOn : Unit = {
//    oAL.playSource(SHIELD*10)
//  }
//
//  def shieldHit : Unit = {
//
//  }
//
//  def shieldOff : Unit = {
//    oAL.stopSource(SHIELD*10)
//  }
//
//  def iceCollision : Unit = {
//
//  }
//
//  def fireCollision : Unit = {
//
//  }
//
//  def castFireSpell : Unit = {
//    var it = FIREBLAST*10
//    //println("fire")
//    while (!oAL.playSource(it)) {
//      it = it+1
//      if (it > FIREBLAST*10 + 9) return
//    }
//  }
//
//  def castIceSpell : Unit = {
//    var it = ICEBLAST*10
//    //println("fire")
//     while (!oAL.playSource(it)) {
//       //println("play ice " + it)
//       it = it+1
//       if (it > ICEBLAST*10 + 9) return
//    }
//  }
//}
//
//class OpenAL(NUM_SOURCES : Int, NUM_BUFFERS : Int) {
//
//
//  var alc : ALC = null
//  var al : AL = null
//
//  // Buffers hold sound data.
//  val buffers = new Array[Int](NUM_BUFFERS);
//
//  // Sources are points emitting sound.
//  val sources = new Array[Int](NUM_SOURCES);
//
//  // Position of the source sound.
//  val sourcePos = Array[Float]( 0.0f, 0.0f, 0.0f );
//
//  // Velocity of the source sound.
//  val sourceVel = Array[Float]( 0.0f, 0.0f, 0.0f );
//
//  // Position of the listener.
//  val listenerPos = Array[Float]( 0.0f, 0.0f, 0.0f );
//
//  // Velocity of the listener.
//  val listenerVel = Array[Float]( 0.0f, 0.0f, 0.0f );
//
//  // Orientation of the listener. (first 3 elems are "at", second 3 are "up")
//  val listenerOri = Array[Float]( 0.0f, 0.0f, -1.0f, 0.0f, 1.0f, 0.0f );
//
//
//  def initOpenAL() : Int = {
//    alc = ALFactory.getALC()
//    al = ALFactory.getAL()
//
//    // var deviceName = "DirectSound3D"
//
//    // Get handle to device.
//    var device = alc.alcOpenDevice(null)
//
//    // Get the device specifier.
//    //    println("Using device " + deviceSpecifier);
//
//    // Create audio context.
//    var context = alc.alcCreateContext(device, null);
//
//    // Set active context.
//    alc.alcMakeContextCurrent(context);
//
//    // Check for an error.
//    if (alc.alcGetError(device) != ALCConstants.ALC_NO_ERROR)
//      return ALConstants.AL_FALSE;
//
//    al.alGenBuffers(NUM_BUFFERS, buffers, 0)
//    if (al.alGetError() != ALConstants.AL_NO_ERROR)
//      return ALConstants.AL_FALSE;
//
//    al.alGenSources(NUM_SOURCES, sources, 0);
//    if (al.alGetError() != ALConstants.AL_NO_ERROR)
//      return ALConstants.AL_FALSE;
//
//    return ALConstants.AL_TRUE;
//  }
//
//
//  def loadData(wavFile : String, bufferNo : Int, sourceNo : Int, offset : Int, isloop : Boolean) : Int = {
//    var loop = ALConstants.AL_FALSE
//    if(isloop) loop = ALConstants.AL_TRUE
//
//    // variable to load into
//    var wavData = WAVLoader.loadFromFile(wavFile)
//
//    // Load wav data into buffers.
//    al.alBufferData( buffers(bufferNo), wavData.format, wavData.data, wavData.size, wavData.freq)
//
//    for(i <- sourceNo*10 to (sourceNo*10 + offset)) {
//      // bind buffers into audio sources
//      al.alSourcei(sources(i), ALConstants.AL_BUFFER, buffers(bufferNo));
//      al.alSourcef(sources(i), ALConstants.AL_PITCH, 1.0f);
//      al.alSourcef(sources(i), ALConstants.AL_GAIN, 1.0f);
//      al.alSourcefv(sources(i), ALConstants.AL_POSITION, sourcePos, 0);
//      al.alSourcefv(sources(i), ALConstants.AL_POSITION, sourceVel, 0);
//      al.alSourcei(sources(i), ALConstants.AL_LOOPING, loop);
//
//    }
//
//    if (al.alGetError() != ALConstants.AL_NO_ERROR)
//      return ALConstants.AL_FALSE;
//
//    return ALConstants.AL_TRUE
//  }
//
//  def setListenerValues()  = {
//    al.alListenerfv(ALConstants.AL_POSITION, listenerPos, 0);
//    al.alListenerfv(ALConstants.AL_VELOCITY, listenerVel, 0);
//    al.alListenerfv(ALConstants.AL_ORIENTATION, listenerOri, 0);
//  }
//
//  def killAllData() = {
//    al.alDeleteSources(NUM_SOURCES, sources, 0)
//    al.alDeleteBuffers(NUM_BUFFERS, buffers, 0);
//  }
//
//  def exitOpenAL() : Unit = {
//    killAllData()
//
//    // Get the current context.
//    val curContext = alc.alcGetCurrentContext();
//
//    // Get the device used by that context.
//    val curDevice = alc.alcGetContextsDevice(curContext);
//
//    // Reset the current context to NULL.
//    alc.alcMakeContextCurrent(null);
//
//    // Release the context and the device.
//    alc.alcDestroyContext(curContext);
//    alc.alcCloseDevice(curDevice);
//  }
//
//  def playSource(sourceNo : Int) : Boolean = {
//    var state = Array[Int](1)
//    al.alGetSourcei(sources(sourceNo), ALConstants.AL_SOURCE_STATE, state, 0);
//    if (state(0) != ALConstants.AL_PLAYING) {
//      al.alSourcePlay(sources(sourceNo))
//      return true;
//    }
//    return false;
//  }
//  def stopSource(sourceNo : Int) = {
//    var state = Array[Int](1)
//    al.alGetSourcei(sources(sourceNo), ALConstants.AL_SOURCE_STATE, state, 0);
//    if (state(0) == ALConstants.AL_PLAYING) {
//      al.alSourceStop(sources(sourceNo))
//    }
//  }
//
//}