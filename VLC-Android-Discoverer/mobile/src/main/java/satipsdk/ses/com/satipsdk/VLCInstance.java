package satipsdk.ses.com.satipsdk;

import org.videolan.libvlc.LibVLC;

import java.util.ArrayList;

public class VLCInstance {

    private static LibVLC sLibVLC;

    /** A set of utility functions for the VLC application */
    public synchronized static LibVLC get() throws IllegalStateException {
        if (sLibVLC == null) {
//            if(!VLCUtil.hasCompatibleCPU(context)) {
//                Log.e(TAG, VLCUtil.getErrorMsg());
//                throw new IllegalStateException("LibVLC initialisation failed: " + VLCUtil.getErrorMsg());
//            }

            ArrayList<String> options = new ArrayList<>();
            options.add("-vv");
            sLibVLC = new LibVLC(SatIpApplication.get(), options);
        }
        return sLibVLC;
    }
}
