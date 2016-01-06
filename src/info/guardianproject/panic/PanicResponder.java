
package info.guardianproject.panic;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Build;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class PanicResponder {

    public static final String PREF_TRIGGER_PACKAGE_NAME = "panicResponderTriggerPackageName";

    /**
     * Checks whether the provided {@link Activity} was started with the action
     * {@link Panic#ACTION_DISCONNECT}, and if so, processes that {@link Intent}
     * , removing the sending app as the panic trigger if it is currently
     * configured to be so.
     *
     * @param activity the {@code Activity} to check for the {@code Intent}
     * @return whether an {@code ACTION_DISCONNECT Intent} was received
     */
    public static boolean checkForDisconnectIntent(Activity activity) {
        boolean result = false;
        if (PanicUtils.checkForIntentWithAction(activity, Panic.ACTION_DISCONNECT)) {
            result = true;
            if (TextUtils.equals(PanicUtils.getCallingPackageName(activity),
                    getTriggerPackageName(activity))) {
                setTriggerPackageName(activity, null);
            }
        }
        return result;
    }

    /**
     * Get the {@code packageName} of the currently configured panic trigger
     * app, or {@code null} if none.
     *
     * @param context the app's {@link Context}
     * @return the {@code packageName} or null
     */
    public static String getTriggerPackageName(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getString(PREF_TRIGGER_PACKAGE_NAME, null);
    }

    /**
     * Set the currently configured panic trigger app using the {@link Activity}
     * that received a {@link Panic#ACTION_CONNECT} {@link Intent}. If that
     * {@code Intent} was not set with either
     * {@link Activity#startActivityForResult(Intent, int)} or
     * {@link Intent#setPackage(String)}, then this will result in no panic
     * trigger app being active.
     * <p>
     * When the user changes the panic app config, then the current app needs to
     * send {@link Intent}s to the previous app, and the currently configured
     * app to let them know about the changes. This is done by sending an
     * {@code ACTION_DISCONNECT Intent} to the previous app, and an
     * {@code ACTION_CONNECT Intent} to the newly configured app.
     * <p>
     * When this is called with an {@code Activity} in the same responder app
     * that called the {@link Activity#startActivityForResult(Intent, int)},
     * then it will <b>not</b> change the existing trigger app setting! For
     * example, if the responder app launches its Panic Config {@code Activity}.
     *
     * @param activity the {@link Activity} that received an
     *                 {@link Panic#ACTION_CONNECT} {@link Intent}
     */
    public static void setTriggerPackageName(Activity activity) {
        String intentPackageName = activity.getIntent().getPackage();
        String callingPackageName = PanicUtils.getCallingPackageName(activity);
        if (intentPackageName == null && callingPackageName == null) {
            // ignored
        } else {
            setTriggerPackageName(activity, callingPackageName);
        }
    }

    /**
     * Set the {@code packageName} as the currently configured panic trigger
     * app. Set to {@code null} to have no panic trigger app active.
     * <p>
     * When the user changes the panic app config, then the current app needs to
     * send {@link Intent}s to the previous app, and the currently configured
     * app to let them know about the changes. This is done by sending an
     * {@code ACTION_DISCONNECT Intent} to the previous app, and an
     * {@code ACTION_CONNECT Intent} to the newly configured app.
     *
     * @param activity    the current {@link Activity}
     * @param packageName the app to set as the panic trigger
     */
    public static void setTriggerPackageName(Activity activity, String packageName) {
        final PackageManager pm = activity.getPackageManager();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
        String existingPackageName = prefs.getString(PREF_TRIGGER_PACKAGE_NAME, null);
        if (!TextUtils.isEmpty(existingPackageName)) {
            Intent intent = new Intent(Panic.ACTION_DISCONNECT);
            intent.setPackage(existingPackageName);
            List<ResolveInfo> resInfos = pm.queryIntentActivities(intent, 0);
            if (resInfos.size() > 0)
                activity.startActivityForResult(intent, 0);
        }
        if (TextUtils.isEmpty(packageName) || packageName.equals(Panic.PACKAGE_NAME_DEFAULT)) {
            prefs.edit().remove(PREF_TRIGGER_PACKAGE_NAME).apply();
        } else {
            prefs.edit().putString(PREF_TRIGGER_PACKAGE_NAME, packageName).apply();
            Intent intent = new Intent(Panic.ACTION_CONNECT);
            intent.setPackage(packageName);
            List<ResolveInfo> resInfos = pm.queryIntentActivities(intent, 0);
            if (resInfos.size() > 0)
                activity.startActivityForResult(intent, 0);
        }
    }

    /**
     * Get a list of resolved {@link Activity}s that can send panic trigger
     * {@link Intent}s.
     *
     * @param pm a {@link PackageManager} instance from the app's {@link Context}
     * @return {@link List} of {@link ResolveInfo} instances for each app that
     * responds to {@link Panic#ACTION_CONNECT} but not {@link Panic#ACTION_TRIGGER}
     */
    public static List<ResolveInfo> resolveTriggerApps(PackageManager pm) {
        /*
         * panic trigger apps respond to ACTION_CONNECT, but only send
         * ACTION_TRIGGER, so they won't be resolved for ACTION_TRIGGER
         */
        List<ResolveInfo> connects = pm.queryIntentActivities(new Intent(Panic.ACTION_CONNECT), 0);
        if (connects.size() == 0)
            return connects;
        ArrayList<ResolveInfo> triggerApps = new ArrayList<ResolveInfo>(connects.size());

        List<ResolveInfo> triggers = pm.queryIntentActivities(new Intent(Panic.ACTION_TRIGGER), 0);
        HashSet<String> haveTriggers = new HashSet<String>(triggers.size());
        for (ResolveInfo resInfo : triggers)
            haveTriggers.add(resInfo.activityInfo.packageName);

        for (ResolveInfo connect : connects)
            if (!haveTriggers.contains(connect.activityInfo.packageName))
                triggerApps.add(connect);

        return triggerApps;
    }

    /**
     * Check whether the provided {@link Activity} has received an {@link Intent}
     * that has an action of {@link Panic#ACTION_TRIGGER} and is from the
     * panic trigger app that is currently connected to this app.
     * <p>
     * <strong>WARNING</strong>: If the {@code Activity} has
     * {@code android:launchMode="singleInstance"} or {@code "singleTask"}, then
     * this method will always return {@code false} because it is not possible
     * to get the calling {@code Activity}, as set by
     * {@link Activity#startActivityForResult(Intent, int)}
     *
     * @param activity the {@code Activity} to get for an {@code Intent}
     * @return boolean
     */
    public static boolean receivedTriggerFromConnectedApp(Activity activity) {
        if (!Panic.isTriggerIntent(activity.getIntent())) {
            return false;
        }

        String packageName = PanicUtils.getCallingPackageName(activity);
        return !TextUtils.isEmpty((packageName))
                && TextUtils.equals(packageName, getTriggerPackageName(activity));
    }

    /**
     * Check whether the provided {@link Activity} has received an {@link Intent}
     * that has an action of {@link Panic#ACTION_TRIGGER} and is not from the
     * currently configured panic trigger app, or, there is no panic trigger app
     * configured.
     *
     * @param activity the {@code Activity} to get for an {@code Intent}
     * @return boolean
     */
    public static boolean shouldUseDefaultResponseToTrigger(Activity activity) {
        if (!Panic.isTriggerIntent(activity.getIntent())) {
            return false;
        }

        String packageName = PanicUtils.getCallingPackageName(activity);
        return TextUtils.isEmpty(packageName)
                || "DEFAULT".equals(packageName)
                || !packageName.equals(getTriggerPackageName(activity));
    }

    @TargetApi(21)
    public static void deleteAllAppData(final Context context) {
        // SharedPreferences can hold onto values and write them out later
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().clear().apply();

        HashSet<File> dirs = new HashSet<File>(3);
        dirs.add(context.getFilesDir().getParentFile()); // root of the app's /data/data
        dirs.add(context.getCacheDir());
        dirs.add(context.getExternalCacheDir());
        if (Build.VERSION.SDK_INT >= 19) {
            for (File f : context.getExternalCacheDirs()) {
                dirs.add(f);
            }
        }
        if (Build.VERSION.SDK_INT >= 21) {
            for (File f : context.getExternalMediaDirs()) {
                dirs.add(f);
            }
        }
        for (File dir : dirs) {
            try {
                if (dir != null && dir.exists()) {
                    deleteRecursive(dir);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        try {
            // this will force close this app, so run last
            if (Build.VERSION.SDK_INT >= 19) {
                ((ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE))
                        .clearApplicationUserData();
            } else {
                Runtime.getRuntime().exec(String.format("pm clear %s", context.getPackageName()));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void deleteRecursive(File f) {
        if (f == null) {
            return;
        }
        if (f.isDirectory()) {
            for (String child : f.list()) {
                deleteRecursive(new File(f, child));
            }
        }
        f.delete();
    }
}
