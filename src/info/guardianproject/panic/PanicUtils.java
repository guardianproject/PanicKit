package info.guardianproject.panic;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.text.TextUtils;
import android.util.Log;

public class PanicUtils {

    static final Intent TRIGGER_INTENT = new Intent(Panic.ACTION_TRIGGER);

    static String getCallingPackageName(Activity activity) {
        // getCallingPackage() was unstable until android-18, use this
        ComponentName componentName = activity.getCallingActivity();
        if (componentName == null)
            return null;
        String packageName = componentName.getPackageName();
        if (TextUtils.isEmpty(packageName)) {
            packageName = activity.getIntent().getPackage();
        }
        if (TextUtils.isEmpty(packageName)) {
            Log.e(activity.getPackageName(),
                    "Received blank Panic.ACTION_DISCONNECT Intent, it must be sent using startActivityForResult()!");
        }
        return packageName;
    }

    static boolean checkForIntentWithAction(Activity activity, String action) {
        if (TextUtils.equals(activity.getIntent().getAction(), action))
            return true;
        else
            return false;
    }

    static void throwNotTriggerIntent() {
        throw new IllegalArgumentException("The provided Intent must have an action of "
                + Panic.ACTION_TRIGGER);
    }
}
