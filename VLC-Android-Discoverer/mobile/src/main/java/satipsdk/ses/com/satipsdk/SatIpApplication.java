package satipsdk.ses.com.satipsdk;

import android.app.Application;
import android.content.Context;

public class SatIpApplication extends Application {

    private static SatIpApplication sInstance;
    @Override
    public void onCreate() {
        super.onCreate();
        sInstance = this;
    }

    public static Context get() {
        return sInstance;
    }
}
