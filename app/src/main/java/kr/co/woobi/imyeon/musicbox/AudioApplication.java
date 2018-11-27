package kr.co.woobi.imyeon.musicbox;

import android.app.Application;

public class AudioApplication extends Application {
    private static AudioApplication mInstance;
    private AudioServiceInterface mInterface;

    @Override
    public void onCreate() {
        super.onCreate();
        mInstance=this;
        mInterface=new AudioServiceInterface(getApplicationContext());
    }
    public static  AudioApplication getmInstance(){
        return  mInstance;
    }

    public AudioServiceInterface getServiceInterface(){
        return mInterface;
    }
}
