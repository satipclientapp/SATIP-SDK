package satipsdk.ses.com.satipsdk.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import java.util.Collection;

import satipsdk.ses.com.satipsdk.SatIpApplication;

public class Util {

    public static boolean hasLANConnection() {
        boolean networkEnabled = false;
        ConnectivityManager connectivity = (ConnectivityManager) (SatIpApplication.get().getSystemService(Context.CONNECTIVITY_SERVICE));
        if (connectivity != null) {
            NetworkInfo networkInfo = connectivity.getActiveNetworkInfo();
            if (networkInfo != null && networkInfo.isConnected() &&
                    (networkInfo.getType() != ConnectivityManager.TYPE_MOBILE)) {
                networkEnabled = true;
            }
        }
        return networkEnabled;
    }

    public static int getPosition(Collection collection, Object object) {
        int counter = 0;
        for (Object item : collection) {
            ++counter;
            if (item.equals(object))
                return counter;
        }
        return -1;
    }

    public static boolean isCollectionEmpty(Collection collection) {
        return collection == null || collection.isEmpty();
    }
}
