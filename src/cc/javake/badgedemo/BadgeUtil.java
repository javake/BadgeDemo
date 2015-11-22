
package cc.javake.badgedemo;

import java.lang.reflect.Field;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;

//BadgeUtil provides static utility methods to set "badge count" on Launcher (by Samsung, LG). 
//Currently, it's working from Android 4.0. 
//But some devices, which are released from the manufacturers, are not working.

public class BadgeUtil {

    private static final String ACTION_BADGE_COUNT_UPDATE = "android.intent.action.BADGE_COUNT_UPDATE";
    private static final String ACTION_BADGE_COUNT_UPDATE_SONY = "com.sonyericsson.home.action.UPDATE_BADGE";

    private static final String EXTRA_BADGE_COUNT = "badge_count";

    private static final String EXTRA_BADGE_COUNT_PACKAGE_NAME = "badge_count_package_name";

    private static final String EXTRA_BADGE_COUNT_CLASS_NAME = "badge_count_class_name";

    /**
     * Set badge count
     * 
     * @param context The context of the application package.
     * @param count Badge count to be set
     */
    public static void setBadgeCount(Context context, int count) {
    	String facName = TextUtils.isEmpty(Build.MANUFACTURER) ? "" : Build.MANUFACTURER.trim().toLowerCase();
    	if (facName.contains("xiaomi")) {
    		sendToXiaoMi(context, count);
    	} else if (facName.contains("sony")) {
    		setBadgeSony(context, count);
    	} else { // samsung 、 LG     其他的没法子了
    		sendToSamsungLG(context, count);
    	}
    }

    /**
     * Reset badge count. The badge count is set to "0"
     * 
     * @param context The context of the application package.
     */
    public static void resetBadgeCount(Context context) {
        setBadgeCount(context, 0);
    }
    
    private static void setBadgeSony(Context context, int count) {
        Intent intent = new Intent(ACTION_BADGE_COUNT_UPDATE_SONY);
        intent.putExtra("com.sonyericsson.home.intent.extra.badge.ACTIVITY_NAME", getLauncherClassName(context));
        intent.putExtra("com.sonyericsson.home.intent.extra.badge.SHOW_MESSAGE", count > 0 ? true : false);
        intent.putExtra("com.sonyericsson.home.intent.extra.badge.MESSAGE", String.valueOf(count));
        intent.putExtra("com.sonyericsson.home.intent.extra.badge.PACKAGE_NAME", context.getPackageName());
        context.sendBroadcast(intent);
    }
    
    private static void sendToSamsungLG(Context context, int count) {
    	Intent badgeIntent = new Intent(ACTION_BADGE_COUNT_UPDATE);
        badgeIntent.putExtra(EXTRA_BADGE_COUNT, count);
        badgeIntent.putExtra(EXTRA_BADGE_COUNT_PACKAGE_NAME, context.getPackageName());
        badgeIntent.putExtra(EXTRA_BADGE_COUNT_CLASS_NAME, getLauncherClassName(context));
        context.sendBroadcast(badgeIntent);
    }
    
    // 必须使用，Activity启动页
	@SuppressWarnings("rawtypes")
	private static void sendToXiaoMi(Context context, int count) {
		NotificationManager nm = (NotificationManager) context
				.getSystemService(Context.NOTIFICATION_SERVICE);
		Notification notification = null;
		boolean isMiUIV6 = true;
		try {
			NotificationCompat.Builder builder = new NotificationCompat.Builder(
					context);
			builder.setContentTitle("您有" + count + "未读消息");
			builder.setTicker("您有" + count + "未读消息");
			builder.setAutoCancel(true);
			builder.setSmallIcon(R.drawable.ic_launcher);
			builder.setDefaults(Notification.DEFAULT_LIGHTS);
			notification = builder.build();
			Class miuiNotificationClass = Class
					.forName("android.app.MiuiNotification");
			Object miuiNotification = miuiNotificationClass.newInstance();
			Field field = miuiNotification.getClass().getDeclaredField(
					"messageCount");
			field.setAccessible(true);
			field.set(miuiNotification, count);// 设置信息数
			field = notification.getClass().getField("extraNotification");
			field.setAccessible(true);
			field.set(notification, miuiNotification);
		} catch (Exception e) {
			e.printStackTrace();
			// miui 6之前的版本
			isMiUIV6 = false;
			Intent localIntent = new Intent("android.intent.action.APPLICATION_MESSAGE_UPDATE");
			localIntent.putExtra("android.intent.extra.update_application_component_name",
					context.getPackageName() + "/" + context.getClass().getName());
			localIntent.putExtra(
					"android.intent.extra.update_application_message_text", count);
			context.sendBroadcast(localIntent);
		} finally {
			if (notification != null && isMiUIV6) { // miui6以上版本需要使用通知发送
				nm.notify(101010, notification);
			}
		}
	}
    
    /**
     * Retrieve launcher activity name of the application from the context
     *
     * @param context The context of the application package.
     * @return launcher activity name of this application. From the
     *         "android:name" attribute.
     */
    private static String getLauncherClassName(Context context) {
        PackageManager packageManager = context.getPackageManager();

        Intent intent = new Intent(Intent.ACTION_MAIN);
        // To limit the components this Intent will resolve to, by setting an
        // explicit package name.
        intent.setPackage(context.getPackageName());
        intent.addCategory(Intent.CATEGORY_LAUNCHER);

        // All Application must have 1 Activity at least.
        // Launcher activity must be found!
        ResolveInfo info = packageManager
                .resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY);

        // get a ResolveInfo containing ACTION_MAIN, CATEGORY_LAUNCHER
        // if there is no Activity which has filtered by CATEGORY_DEFAULT
        if (info == null) {
            info = packageManager.resolveActivity(intent, 0);
        }
        return info.activityInfo.name;
    }
}
