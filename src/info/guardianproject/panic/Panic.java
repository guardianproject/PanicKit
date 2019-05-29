package info.guardianproject.panic;

import android.content.Intent;

public class Panic {

    public static final String ACTION_CONNECT = "info.guardianproject.panic.action.CONNECT";
    public static final String ACTION_DISCONNECT = "info.guardianproject.panic.action.DISCONNECT";
    public static final String ACTION_TRIGGER = "info.guardianproject.panic.action.TRIGGER";

    public static final String PACKAGE_NAME_NONE = "NONE";
    public static final String PACKAGE_NAME_DEFAULT = "DEFAULT";

    /**
     * Check the specified {@link Intent} to see if it is a
     * {@link #ACTION_TRIGGER} {@code Intent}.
     *
     * @param intent the {@code Intent} to check
     * @return whether {@code intent} has an action of {@code Panic.ACTION_TRIGGER}
     */
    public static boolean isTriggerIntent(Intent intent) {
        return intent != null && ACTION_TRIGGER.equals(intent.getAction());
    }
}
