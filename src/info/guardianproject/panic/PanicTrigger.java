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

    private static final int SHARED_PREFS_MODE = Context.MODE_PRIVATE;
    private static final String CONNECTED_SHARED_PREFS = "info.guardianproject.panic.PanicTrigger.CONNECTED";
    private static final String ENABLED_SHARED_PREFS = "info.guardianproject.panic.PanicTrigger.ENABLED";

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
     * @see #removeConnectedResponder(Context, String)
     */
    public static boolean addConnectedResponder(Context context, String packageName) {
        SharedPreferences prefs = context.getSharedPreferences(CONNECTED_SHARED_PREFS, SHARED_PREFS_MODE);
        // present in the prefs means connected
        return prefs.edit().putBoolean(packageName, true).commit();
    }

    /**
     * Remove a {@code packageName} from the list of connected responders.
     *
     * @param context     the app's {@link Context}
     * @param packageName the responder to remove
     * @return whether it was successfully removed
     * @see #addConnectedResponder(Context, String)
     */
    public static boolean removeConnectedResponder(Context context, String packageName) {
        SharedPreferences prefs = context.getSharedPreferences(CONNECTED_SHARED_PREFS, SHARED_PREFS_MODE);
        // absent from the prefs means not connected
        return prefs.contains(packageName) && prefs.edit().remove(packageName).commit();
    }

    /**
     * Add a {@code packageName} to the list of responders that will receive a trigger from this app.
     *
     * @param context     the app's {@link Context}
     * @param packageName the responder to add
     * @return whether it was successfully completed
     * @see #disableResponder(Context, String)
     */
    public static boolean enableResponder(Context context, String packageName) {
        SharedPreferences prefs = context.getSharedPreferences(ENABLED_SHARED_PREFS, SHARED_PREFS_MODE);
        return prefs.edit().putBoolean(packageName, true).commit();
    }

    /**
     * Remove a {@code packageName} to the list of responders that will receive a trigger from this app.
     *
     * @param context     the app's {@link Context}
     * @param packageName the responder to add
     * @return whether it was successfully completed
     * @see #enableResponder(Context, String)
     */
    public static boolean disableResponder(Context context, String packageName) {
        SharedPreferences prefs = context.getSharedPreferences(ENABLED_SHARED_PREFS, SHARED_PREFS_MODE);
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
     * Get the {@link Set} of all {@code packageNames} of installed apps that include
     * any {@link Activity}, {@link android.content.BroadcastReceiver}, or
     * {@link android.app.Service} that responds to {@link Panic#ACTION_TRIGGER}
     * {@link Intent}s.
     *
     * @param context the app's {@link Context}
     * @return the set of {@code packageNames} of responder apps
     * @see #getResponderActivities(Context) to get the {@link Activity}s
     * @see #getResponderBroadcastReceivers(Context) to get the {@link android.content.BroadcastReceiver}s
     * @see #getResponderServices(Context) to get the {@link android.app.Service}s
     * @see #getEnabledResponders(Context)
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
     * @see #getAllResponders(Context)
     * @see #getEnabledResponders(Context)
     */
    public static Set<String> getConnectedResponders(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(CONNECTED_SHARED_PREFS, SHARED_PREFS_MODE);
        Set<String> connectedAndInstalled = new HashSet<String>();
        Set<String> all = getAllResponders(context);
        // present in the connected prefs means it has been connected
        for (Map.Entry<String, ?> entry : prefs.getAll().entrySet()) {
            String packageName = entry.getKey();
            if (all.contains(packageName)) {
                connectedAndInstalled.add(packageName);
            }
        }
        return connectedAndInstalled;
    }

    /**
     * Get the {@link Set} of {@code packageNames} of any {@link Activity}s or
     * {@link android.app.Service}s that respond to {@link Panic#ACTION_TRIGGER}
     * and have been enabled by the user
     *
     * @param context the app's {@link Context}
     * @return the set of {@code packageNames} of responder apps that are
     * currently to this trigger app
     * @see #checkForConnectIntent(Activity)
     * @see #checkForDisconnectIntent(Activity)
     * @see #getAllResponders(Context)
     * @see #getConnectedResponders(Context)
     */
    public static Set<String> getEnabledResponders(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(ENABLED_SHARED_PREFS, SHARED_PREFS_MODE);
        Map<String, ?> allPrefs = prefs.getAll();
        Set<String> enabledAndInstalled = new HashSet<String>();
        Set<String> all = getAllResponders(context);
        if (allPrefs.isEmpty()) {
            // make sure allPrefs is not empty if the user disables all apps
            prefs.edit().putBoolean("hasBeenInited", true).apply();
            // the default is enabled, so write this this out
            for (Map.Entry<String, ?> entry : allPrefs.entrySet()) {
                enableResponder(context, entry.getKey());
            }
            return all;
        } else {
            // present in the enabled prefs means it is currently enabled
            for (Map.Entry<String, ?> entry : allPrefs.entrySet()) {
                String packageName = entry.getKey();
                if (all.contains(packageName)) {
                    enabledAndInstalled.add(packageName);
                }
            }
            return enabledAndInstalled;
        }
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
     * configured panic receivers.  See {@link #sendTrigger(Context, Intent)}
     * if you want to use a custom {@code Intent} that can include things
     * like a text message, email addresses, phone numbers, etc.
     * <p/>
     * If the receiving apps must be able to verify which app sent this
     * {@code Intent}, then {@code context} <b>must</b> be an instance of
     * {@link Activity}.
     * <p/>
     * Only the receiving {@code Activity}s will be able to verify which app sent this,
     * {@link android.app.Service}s and {@link android.content.BroadcastReceiver}s
     * will not.
     *
     * @param context the {@code Context} that will send the trigger {@code Intent},
     *                If this is an instance of {@code Activity}, then the receiving
     *                apps will be able to verify which app sent the {@code Intent}
     * @throws IllegalArgumentException if not a {@link Panic#ACTION_TRIGGER}
     *                                  {@code Intent}
     */
    public static void sendTrigger(Context context) {
        sendTrigger(context, PanicUtils.TRIGGER_INTENT);
    }

    /**
     * Send the {@link Intent} to all configured panic receivers.  It must have
     * an {@code action} of {@link Panic#ACTION_TRIGGER} or a
     * {@link IllegalArgumentException} will be thrown.  The {@code Intent} can
     * include things like a text message, email addresses, phone numbers, etc.
     * which a panic receiver app can use to send the message.
     * <p/>
     * If the receiving apps must be able to verify which app sent this
     * {@code Intent}, then {@code context} <b>must</b> be an instance of
     * {@link Activity}.
     * <p/>
     * Only receiving {@code Activity}s will be able to verify who sent this,
     * {@link android.app.Service}s and {@link android.content.BroadcastReceiver}s
     * will not.
     *
     * @param context the {@code Context} that will send the trigger {@code Intent},
     *                If this is an instance of {@code Activity}, then the receiving
     *                apps will be able to verify which app sent the {@code Intent}
     * @param intent  the {@code Intent} to send to panic responders
     * @throws IllegalArgumentException if not a {@link Panic#ACTION_TRIGGER}
     *                                  {@code Intent}
     */
    public static void sendTrigger(Context context, Intent intent) {
        if (!Panic.isTriggerIntent(intent)) {
            PanicUtils.throwNotTriggerIntent();
        }
        Set<String> enabled = getEnabledResponders(context);
        for (String s : enabled)
        try {
            // Activitys
            for (String packageName : getResponderActivities(context)) {
                if (enabled.contains(packageName)) {
                    intent.setPackage(packageName);
                    if (context instanceof Activity) {
                        Activity activity = (Activity) context;
                        activity.startActivityForResult(intent, 0);
                    } else {
                        // startActivityForResult() comes from Activity, so use an
                        // alternate method of sending that Context supports. This
                        // currently will send an Intent which the receiver will
                        // not be able to verify which app sent it. That requires
                        // including an IntentSender or some other hack like that
                        // https://dev.guardianproject.info/issues/6260
                        context.startActivity(intent);
                    }
                }
            }
            // BroadcastReceivers
            for (String packageName : getResponderBroadcastReceivers(context)) {
                if (enabled.contains(packageName)) {
                    intent.setPackage(packageName);
                    context.sendBroadcast(intent);
                }
            }
            //Services
            for (String packageName : getResponderServices(context)) {
                if (enabled.contains(packageName)) {
                    intent.setPackage(packageName);
                    context.startService(intent);
                }
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
