package siris.components.sound;

import org.lwjgl.openal.AL;
import org.lwjgl.openal.AL10;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;

/**
 * Contains static methodes control openAL.
 */
public class OpenALUtils{

  /**
   * Initialise OpenAL.
   */
  public static void alInit(){
    // create openAL
    try{
      AL.create();
    } catch(Exception e){
      System.out.println("Init openAL failed!");
      e.printStackTrace();
      System.exit(-1);
    }

    //AL10.alDistanceModel(AL10.AL_REFERENCE_DISTANCE);
//    AL.alListener3f(AL.AL_ORIENTATION, );
//    System.out.println("AL_VERSION = " + AL.alGetString(AL.AL_VERSION));
//    System.out.println("AL_RENDERER = " + AL.alGetString(AL.AL_RENDERER));
//    System.out.println("AL_VENDOR = " + AL.alGetString(AL.AL_VENDOR));
//    System.out.println("AL_EXTENSIONS = " + AL.alGetString(AL.AL_EXTENSIONS));
//    System.out.println("AL_CHANNELS = " + AL.alGetListeneri(AL.AL_CHANNELS));

    // allocate all available sources
    OpenALSource.alGenSoureces();
  }

  /**
   * Shutdowns OpenAL.
   */
  public static void alExit(){
    // free all allocated sources
    OpenALSource.alDeleteSources();

    AL.destroy();
  }

  /**
   * Creates an integer buffer to hold specified ints
   * - strictly a utility method.
   *
   * @param size how many int to contain
   * @return created IntBuffer
   */
  protected static IntBuffer createIntBuffer(int size){
    ByteBuffer temp = ByteBuffer.allocateDirect(4 * size);
    temp.order(ByteOrder.nativeOrder());

    return temp.asIntBuffer();
  }

  protected static void checkError(){
    int lastError;
    if((lastError = AL10.alGetError()) != AL10.AL_NO_ERROR){
      exit(lastError);
    }
  }

  /**
   * Exits the test NOW, printing errorcode to stdout.
   *
   * @param error Error code causing exit
   */
  protected static void exit(int error){
    System.out.println("OpenAL Error: " + AL10.alGetString(error));
    alExit();
    System.exit(-1);
  }
}
