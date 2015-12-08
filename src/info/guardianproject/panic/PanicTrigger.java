package info.guardianproject.panic;

import android.app.Activity;
import android.content.ActivityNotFoundException;
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
     * @param context     the app's {@link Context}
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
     * @param context     the app's {@link Context}
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
     * @param context the app's {@link Context}
     * @return the set of {@code packageNames} of responder {@code Activity}s
     * @see #getResponderServices(Context) to get the {@link android.app.Service}s
     * @see #getResponderBroadcastReceivers(Context) to get the {@link android.content.BroadcastReceiver}s
     */
    public static Set<String> getResponderActivities(Context context) {
        final PackageManager pm = context.getPackageManager();
        List<ResolveInfo> activitiesList = pm.queryIntentActivities(PanicUtils.TRIGGER_INTENT, 0);
        Set<String> activities = new HashSet<String>();
        for (ResolveInfo resInfo : activitiesList) {
            activities.add(resInfo.activityInfo.packageName);
        }
        return activities;
    }

    /**
     * Get the {@link Set} of {@code packageNames} of all
     * {@link android.content.BroadcastReceiver}s that respond to
     * {@link Panic#ACTION_TRIGGER}.  Unlike with {@link android.app.Service}s and
     * {@link Activity}s, a {@code BroadcastReceiver} cannot verify which app
     * sent this {@link Intent} to it.
     * <p>
     * {@link android.content.BroadcastReceiver}s are not able to verify which app sent this.
     *
     * @param context the app's {@link Context}
     * @return the set of {@code packageNames} of responder {@code BroadcastReceiver}s
     * @see #getResponderActivities(Context) to get the {@link Activity}s
     * @see #getResponderServices(Context) to get the {@link android.app.Service}s
     */
    public static Set<String> getResponderBroadcastReceivers(Context context) {
        final PackageManager pm = context.getPackageManager();
        List<ResolveInfo> receiversList = pm.queryBroadcastReceivers(PanicUtils.TRIGGER_INTENT, 0);
        Set<String> broadcastReceivers = new HashSet<String>();
        for (ResolveInfo resInfo : receiversList) {
            broadcastReceivers.add(resInfo.activityInfo.packageName);
        }
        return broadcastReceivers;
    }

    /**
     * Get the {@link Set} of {@code packageNames} of all {@link android.app.Service}s
     * that respond to {@link Panic#ACTION_TRIGGER}.
     * <p>
     * {@link android.app.Service}s are not able to verify which app sent this.
     *
     * @param context the app's {@link Context}
     * @return the set of {@code packageNames} of responder {@code Service}s
     * @see #getResponderActivities(Context) to get the {@link Activity}s
     * @see #getResponderBroadcastReceivers(Context) to get the {@link android.content.BroadcastReceiver}s
     */
    public static Set<String> getResponderServices(Context context) {
        final PackageManager pm = context.getPackageManager();
        List<ResolveInfo> servicesList = pm.queryIntentServices(PanicUtils.TRIGGER_INTENT, 0);
        Set<String> services = new HashSet<String>();
        for (ResolveInfo resInfo : servicesList) {
            services.add(resInfo.serviceInfo.packageName);
        }
        return services;
    }

    /**
     * Get the {@link Set} of all {@code packageNames} of any {@link Activity}s,
     * {@link android.content.BroadcastReceiver}s, or {@link android.app.Service}s
     * that respond to {@link Panic#ACTION_TRIGGER} {@link Intent}s.
     *
     * @param context the app's {@link Context}
     * @return the set of {@code packageNames} of responder apps
     * @see #getResponderActivities(Context) to get the {@link Activity}s
     * @see #getResponderBroadcastReceivers(Context) to get the {@link android.content.BroadcastReceiver}s
     * @see #getResponderServices(Context) to get the {@link android.app.Service}s
     */
    public static Set<String> getAllResponders(Context context) {
        List<String> packageNames = new ArrayList<String>(getResponderActivities(context));
        packageNames.addAll(getResponderBroadcastReceivers(context));
        packageNames.addAll(getResponderServices(context));
        return new HashSet<String>(packageNames);
    }

    /**
     * Get the {@link Set} of {@code packageNames} of any {@link Activity}s or
     * {@link android.app.Service}s that respond to {@link Panic#ACTION_TRIGGER}
     * and have been manually connected by the user to this app.
     *
     * @param context the app's {@link Context}
     * @return the set of {@code packageNames} of responder apps that are
     * currently to this trigger app
     * @see #checkForConnectIntent(Activity)
     * @see #checkForDisconnectIntent(Activity)
     */
    public static Set<String> getConnectedResponders(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(SHARED_PREFS, SHARED_PREFS_MODE);
        Set<String> connectedAndInstalled = new HashSet<String>();
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

    /**
     * Get all of the responders that are able to make a full connection using
     * {@link Panic#ACTION_CONNECT}, which is used to configure the response an
     * app makes.  For destructive responses, it is essential that the trigger
     * and responder are connected in order to prevent random apps from making
     * responders destroy things.  Apps can also only respond to
     * {@link Panic#ACTION_TRIGGER} with a non-destructive response which does
     * not require the apps to connect or the user to configure anything.
     *
     * @param context the app's {@link Context}
     * @return the set of {@code packageNames} of responder apps that can connect
     * to a trigger app
     */
    public static Set<String> getRespondersThatCanConnect(Context context) {
        List<ResolveInfo> connectInfos = context.getPackageManager().queryIntentActivities(
                PanicUtils.CONNECT_INTENT, 0);
        final Set<String> connectPackageNameList = new HashSet<String>(connectInfos.size());
        for (ResolveInfo resolveInfo : connectInfos) {
            if (resolveInfo.activityInfo == null)
                continue;
            connectPackageNameList.add(resolveInfo.activityInfo.packageName);
        }
        return connectPackageNameList;
    }

    /**
     * Send a basic {@link Panic#ACTION_TRIGGER} {@link Intent} to all
     * configured panic receivers.  See {@link #sendTrigger(Activity, Intent)}
     * if you want to use a custom {@code Intent} that can include things
     * like a text message, email addresses, phone numbers, etc.
     * <p>
     * Only the receiving {@link Activity}s will be able to verify which app sent this,
     * {@link android.app.Service}s and {@link android.content.BroadcastReceiver}s
     * will not.
     *
     * @param activity the {@code Activity} that will send the trigger {@code Intent}
     * @throws IllegalArgumentException if not a {@link Panic#ACTION_TRIGGER}
     *                                  {@code Intent}
     */
    public static void sendTrigger(Activity activity) {
        sendTrigger(activity, PanicUtils.TRIGGER_INTENT);
    }

    /**
     * Send the {@link Intent} to all configured panic receivers.  It must have
     * an {@code action} of {@link Panic#ACTION_TRIGGER} or a
     * {@link IllegalArgumentException} will be thrown.  The {@code Intent} can
     * include things like a text message, email addresses, phone numbers, etc.
     * which a panic receiver app can use to send the message.
     * <p>
     * Only the receiving {@link Activity}s will be able to verify who sent this,
     * {@link android.app.Service}s and {@link android.content.BroadcastReceiver}s
     * will not.
     *
     * @param activity the {@code Activity} that will send the trigger {@code Intent}
     * @param intent   the {@code Intent} to send to panic responders
     * @throws IllegalArgumentException if not a {@link Panic#ACTION_TRIGGER}
     *                                  {@code Intent}
     */
    public static void sendTrigger(Activity activity, Intent intent) {
        if (!Panic.isTriggerIntent(intent)) {
            PanicUtils.throwNotTriggerIntent();
        }
        try {
            // Activitys
            for (String packageName : getResponderActivities(activity)) {
                intent.setPackage(packageName);
                activity.startActivityForResult(intent, 0);
            }
            // BroadcastReceivers
            for (String packageName : getResponderBroadcastReceivers(activity)) {
                intent.setPackage(packageName);
                activity.sendBroadcast(intent);
            }
            //Services
            for (String packageName : getResponderServices(activity)) {
                intent.setPackage(packageName);
                activity.startService(intent);
            }
        } catch (ActivityNotFoundException e) {
            // intent-filter without DEFAULT category makes the Activity be detected but not found
            e.printStackTrace();
        } catch (SecurityException e) {
            // if we don't have permission to start the Service
            e.printStackTrace();
        }
    }
}
