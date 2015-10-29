package info.guardianproject.panic;

import android.app.Activity;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class PanicTrigger {

    private static final String SHARED_PREFS = "info.guardianproject.panic.PanicTrigger";
    private static final int SHARED_PREFS_MODE = Context.MODE_PRIVATE;
    private static final String CONNECTED = "connected";

    /**
     * Checks whether the provided {@link Activity} was started with the action
     * {@link Panic#ACTION_CONNECT}, and if so, processes that {@link Intent} ,
     * adding the sending app as the panic trigger.
     *
     * @param activity the {@code Activity} to check for the {@code Intent}
     * @return whether an {@code ACTION_DISCONNECT Intent} was received
     */
    public static boolean checkForConnectIntent(Activity activity) {
        boolean result = PanicUtils.checkForIntentWithAction(activity, Panic.ACTION_CONNECT);
        String packageName = PanicUtils.getCallingPackageName(activity);
        addConnectedResponder(activity, packageName);
        return result;
    }

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
        boolean result = PanicUtils.checkForIntentWithAction(activity, Panic.ACTION_DISCONNECT);
        String packageName = PanicUtils.getCallingPackageName(activity);
        removeConnectedResponder(activity, packageName);
        return result;
    }

    /**
     * Add a {@code packageName} to the list of connected responders.
     *
     * @param context
     * @param packageName the responder to add
     * @return whether it was successfully completed
     */
    public static boolean addConnectedResponder(Context context, String packageName) {
        SharedPreferences prefs = context.getSharedPreferences(SHARED_PREFS, SHARED_PREFS_MODE);
        // present in the prefs means connected
        return prefs.edit().putString(packageName, CONNECTED).commit();
    }

    /**
     * Remove a {@code packageName} from the list of connected responders.
     *
     * @param context
     * @param packageName the responder to remove
     * @return whether it was successfully removed
     */
    public static boolean removeConnectedResponder(Context context, String packageName) {
        SharedPreferences prefs = context.getSharedPreferences(SHARED_PREFS, SHARED_PREFS_MODE);
        // absent from the prefs means not connected
        return prefs.contains(packageName) && prefs.edit().remove(packageName).commit();
    }

    /**
     * Get the {@link Set} of {@code packageNames} of all {@link Activity}s that respond to
     * {@link Panic#ACTION_TRIGGER}.
     *
     * @see #getResponderServices(Context) to get the {@link Service}s
     */
    public static Set<String> getResponderActivities(Context context) {
        final PackageManager pm = context.getPackageManager();
        List<ResolveInfo> activitiesList = pm.queryIntentActivities(PanicUtils.triggerIntent, 0);
        Set<String> activities = new HashSet<String>();
        for (ResolveInfo resInfo : activitiesList) {
            activities.add(resInfo.activityInfo.packageName);
        }
        return activities;
    }

    /**
     * Get the {@link Set} of {@code packageNames} of all {@link Service}s that respond to
     * {@link Panic#ACTION_TRIGGER}.
     *
     * @see #getResponderActivities(Context) to get the {@link Activity}s
     */
    public static Set<String> getResponderServices(Context context) {
        final PackageManager pm = context.getPackageManager();
        List<ResolveInfo> servicesList = pm.queryIntentServices(PanicUtils.triggerIntent, 0);
        Set<String> services = new HashSet<String>();
        for (ResolveInfo resInfo : servicesList) {
            services.add(resInfo.serviceInfo.packageName);
        }
        return services;
    }

    /**
     * Get the {@link Set} of all {@code packageNames} of any {@link Activity}s
     * or {@link Service}s that respond to {@link Panic#ACTION_TRIGGER}
     * {@link Intent}s.
     */
    public static Set<String> getAllResponders(Context context) {
        List<String> packageNames = new ArrayList<>(getResponderActivities(context));
        packageNames.addAll(getResponderServices(context));
        return new HashSet<String>(packageNames);
    }

    /**
     * Get the {@link Set} of {@code packageNames} of any {@link Activity}s or
     * {@link Service}s that respond to {@link Panic#ACTION_TRIGGER} and have
     * been manually connected by the user to this app.
     *
     * @see #checkForConnectIntent(Activity)
     * @see #checkForDisconnectIntent(Activity)
     */
    public static Set<String> getConnectedResponders(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(SHARED_PREFS, SHARED_PREFS_MODE);
        Set<String> connectedAndInstalled = new HashSet<>();
        Set<String> all = getAllResponders(context);
        // present in the prefs means it has been connected
        for (Map.Entry<String, ?> entry : prefs.getAll().entrySet()) {
            String packageName = entry.getKey();
            if (all.contains(packageName)) {
                connectedAndInstalled.add(packageName);
            }
        }
        return connectedAndInstalled;
    }
}
