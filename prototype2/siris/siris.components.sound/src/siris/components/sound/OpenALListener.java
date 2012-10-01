package siris.components.sound;

import org.lwjgl.BufferUtils;
import org.lwjgl.openal.AL10;

import java.nio.FloatBuffer;

public class OpenALListener{

    public static FloatBuffer listenerOri = (FloatBuffer) BufferUtils.createFloatBuffer(6).put(new float[] { 0.0f, 0.0f, -1.0f,  0.0f, 1.0f, 0.0f }).rewind();

  public static int getPropi(int prop){
    return AL10.alGetListeneri(prop);
  }

  public static float getPropf(int prop){
    return AL10.alGetListenerf(prop);
  }

  public static void setPropi(int prop, int value){
    AL10.alListeneri(prop, value);
  }

  public static void setPropf(int prop, float value){
    AL10.alListenerf(prop, value);
  }

  public static void setProp3f(int prop, float value1, float value2, float value3){
    AL10.alListener3f(prop, value1, value2, value3);
  }

  public static void setPos(float x, float y, float z){
    AL10.alListener3f(AL10.AL_POSITION, x, y, z);
  }

     //(first 3 elements are "at", second 3 are "up")
   public static void setOrientation(float upX, float upY, float upZ, float atX, float atY, float atZ){


        listenerOri.put(0,upX);
        listenerOri.put(1,upY);
        listenerOri.put(2,upZ);
        listenerOri.put(3,atX);
        listenerOri.put(4,atY);
        listenerOri.put(5,atZ);

      AL10.alListener(AL10.AL_ORIENTATION, listenerOri);

  }
}
