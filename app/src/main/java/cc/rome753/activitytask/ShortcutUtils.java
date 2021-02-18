package cc.rome753.activitytask;

import android.app.Activity;
import android.app.AppOpsManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.content.pm.ResolveInfo;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.RequiresApi;

import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import cc.rome753.demo.BuildConfig;

/**
 * 快捷方式类
 * 在8.0及以上手机，创建的快捷方式为PinShortcut。 一旦创建只能手动删除，不能用程序删除。目前没有增加DynamicShortcut
 * 另外，目前创建/更新/删除快捷方式的唯一键是shortcutName。也就是说，一旦创建后，在更新时如果想改变shortcutName，目前不支持。如果需要，后期可根据ShortcutInfo作为唯一键。
 */
public class ShortcutUtils {
    private static final String TAG = "ShortcutUtils";
    private final static String ACTION_INSTALL_SHORTCUT = "com.android.launcher.action.INSTALL_SHORTCUT";
    /**
     * 删除快捷方式。不用系统action，使用的是miui的action
     */
    private final static String ACTION_UNINSTALL_SHORTCUT = "com.miui.home.launcher.action.UNINSTALL_SHORTCUT";
    private final static String ACTION_UPDATE_SHORTCUT = "com.android.launcher.action.UPDATE_SHORTCUT";

    public final static String REF_SHORTCUT = "shortcut";
    public final static String PREF_VIDEO = "video";
    public final static String TV_REF_SHORTCUT = "TVshortcut";

    private static WeakReference<OnShortcutCallback> mSuccessCallback;

    public static void createShortcut(Context context, String shortcutName, Bitmap icon, Intent intent) {
        createShortcut(context, shortcutName, icon, intent, null);
    }

    /**
     * 创建快捷方式
     * OnShortcutCallback是在8.0及以上使用，并且只有成功回调，没有失败回调。在8.0以下，不会有任何回调。
     * intent可参考 createIntent方法
     *
     * @param context
     * @param shortcutName
     * @param icon
     * @param intent
     * @param callback     如果连续调用了两次，只有最后一次的callback会执行回调
     */
    public static void createShortcut(Context context, String shortcutName, Bitmap icon, Intent intent, OnShortcutCallback callback) {
        if (TextUtils.isEmpty(shortcutName)) {
            return;
        }
        if (callback != null) {
            mSuccessCallback = new WeakReference(callback);
        }

        SharedPreferences pref = getSharedPreference(context);
        Set<String> shortcuts = pref.getStringSet(REF_SHORTCUT, new HashSet<String>());
        shortcuts.add(shortcutName);
        pref.edit().putStringSet(REF_SHORTCUT, shortcuts).apply();

        if (equalAPI_26_OREO()) {
            createShortcut26(context, shortcutName, icon, intent);
        } else {
            createShortcut19(context, shortcutName, icon, intent);
        }
    }


    private static SharedPreferences getSharedPreference(Context context) {
        return context.getSharedPreferences(PREF_VIDEO, Context.MODE_PRIVATE);
    }

    /**
     * 更新快捷方式。 shortcutName作为唯一键，不允许更改。更新只能更新icon和intent
     * 如果想更新名称，可先删除，再添加
     *
     * @param context
     * @param shortcutName
     * @param icon
     * @param intent
     */
    public static void updateShortcut(Context context, String shortcutName, Bitmap icon, Intent intent) {
        if (TextUtils.isEmpty(shortcutName)) {
            return;
        }
        if (equalAPI_26_OREO()) {
            updateShortcut26(context, shortcutName, icon, intent);
        } else {
            deleteShortcut19(context, shortcutName);
            createShortcut19(context, shortcutName, icon, intent);
        }
    }


    public static void clearShortcut(Context context) {
        Set<String> shortCuts = getSharedPreference(context).getStringSet(REF_SHORTCUT, new HashSet<String>());
        if (shortCuts == null || shortCuts.size() == 0) {
            return;
        }
        for (String shortcut : shortCuts) {
            deleteShortcut(context, shortcut);
        }

        getSharedPreference(context).edit().putStringSet(REF_SHORTCUT, new HashSet<String>()).apply();

    }


    /**
     * 8.0以上不允许删除快捷方式。所以删除快捷方式只有在8.0以下才可以
     *
     * @param context
     * @param shortcutName
     */
    public static void deleteShortcut(Context context, String shortcutName) {
        if (TextUtils.isEmpty(shortcutName)) {
            return;
        }
        if (!equalAPI_26_OREO()) {
            deleteShortcut19(context, shortcutName);
        }
    }

    /**
     * 创建快捷方式，8.0以下
     *
     * @param context
     * @param shortcutName
     * @param icon
     * @param intent
     */
    public static void createShortcut19(Context context, String shortcutName, Bitmap icon, Intent intent) {
        if (TextUtils.isEmpty(shortcutName)) {
            return;
        }
        Intent addShortcutIntent = new Intent(ACTION_INSTALL_SHORTCUT);
        // 是否允许重复创建
        addShortcutIntent.putExtra("duplicate", false);
        // 快捷方式的标题
        addShortcutIntent.putExtra(Intent.EXTRA_SHORTCUT_NAME, shortcutName);
        // 快捷方式的图标
        addShortcutIntent.putExtra(Intent.EXTRA_SHORTCUT_ICON, icon);
        // 快捷方式的动作
        addShortcutIntent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, intent);
        context.sendBroadcast(addShortcutIntent);

    }

    /**
     * 更新快捷方式。 shortcutName作为唯一键，不允许更改。更新只能更新icon和intent
     * 如果想更新名称，可先删除，再添加
     *
     * @param context
     * @param shortcutName
     * @param icon
     * @param intent
     */
    public static void updateShortcut19(Context context, String shortcutName, Bitmap icon, Intent intent) {
        if (TextUtils.isEmpty(shortcutName)) {
            return;
        }
        Intent shortcut = new Intent(ACTION_UPDATE_SHORTCUT);
        // 快捷方式名称
        shortcut.putExtra(Intent.EXTRA_SHORTCUT_NAME, shortcutName);
        shortcut.putExtra(Intent.EXTRA_SHORTCUT_ICON, icon);
        shortcut.putExtra(Intent.EXTRA_SHORTCUT_INTENT, intent);
        context.sendBroadcast(shortcut);
    }

    /**
     * 删除快捷方式
     *
     * @param context
     * @param shortcutName
     */
    public static void deleteShortcut19(Context context, String shortcutName) {
        if (TextUtils.isEmpty(shortcutName)) {
            return;
        }
        Intent shortcut = new Intent(ACTION_UNINSTALL_SHORTCUT);
        // 快捷方式名称
        shortcut.putExtra(Intent.EXTRA_SHORTCUT_NAME, shortcutName);
        Intent intent = new Intent();
        shortcut.putExtra(Intent.EXTRA_SHORTCUT_INTENT, intent);
        context.sendBroadcast(shortcut);
    }

    /**
     * 创建快捷方式，8.0及以上
     *
     * @param context
     * @param shortcutName
     * @param icon
     * @param intent
     */
    @RequiresApi(api = Build.VERSION_CODES.N_MR1)
    public static void createShortcut26(Context context, String shortcutName, Bitmap icon, Intent intent) {
        if (TextUtils.isEmpty(shortcutName)) {
            return;
        }

        if (!checkPermission(context)) {
            jumpToSystemSettingPage(context);
            return;
        }

        Context appContext = context.getApplicationContext();
        ShortcutManager shortcutManager = (ShortcutManager) appContext.getSystemService(Context.SHORTCUT_SERVICE);
        if (shortcutManager == null) {
            return;
        }

        shortcutManager.isRateLimitingActive();
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (shortcutManager.isRequestPinShortcutSupported()) {
                    ShortcutInfo shortcutInfo = getShortcutInfo(appContext, shortcutName, icon, intent);
                    //当添加快捷方式的确认弹框弹出来时，将被回调
                    PendingIntent shortcutCallbackIntent = PendingIntent.getBroadcast(appContext, 0, new Intent(context, OnSuccessReceiver.class), PendingIntent.FLAG_UPDATE_CURRENT);
                    shortcutManager.requestPinShortcut(shortcutInfo, shortcutCallbackIntent.getIntentSender());
                }
            }
        } catch (Exception e) {
            //防止没有activity 和 service 情况 MIVIDEO-1248
            Log.e(TAG, "createShortcut26: ", e);
        }
    }

    /**
     * 更新快捷方式  8.0及以上
     *
     * @param context
     * @param shortcutName
     * @param icon
     * @param intent
     */
    @RequiresApi(api = Build.VERSION_CODES.N_MR1)
    public static void updateShortcut26(Context context, String shortcutName, Bitmap icon, Intent intent) {
        if (TextUtils.isEmpty(shortcutName)) {
            return;
        }

        ShortcutManager shortcutManager = (ShortcutManager) context.getSystemService(Context.SHORTCUT_SERVICE);

        ShortcutInfo.Builder builder = new ShortcutInfo
                .Builder(context, shortcutName)
                .setShortLabel(shortcutName)
                .setIcon(Icon.createWithBitmap(icon))
                .setIntent(intent);
        if (intent.getComponent() != null) {
            builder.setActivity(intent.getComponent());
        }
        ShortcutInfo shortcutInfo = builder

                .build();
        List<ShortcutInfo> list = new ArrayList<>();
        list.add(shortcutInfo);
        shortcutManager.updateShortcuts(list);
    }

    @RequiresApi(api = Build.VERSION_CODES.N_MR1)
    private static ShortcutInfo getShortcutInfo(Context context, String shortcutName, Bitmap icon, Intent intent) {
        if (TextUtils.isEmpty(shortcutName)) {
            return null;
        }
        ShortcutInfo.Builder builder = new ShortcutInfo
                .Builder(context, shortcutName)
                .setShortLabel(shortcutName)
                .setIcon(Icon.createWithBitmap(icon))
                .setIntent(intent);

        if (intent.getComponent() != null) {
            builder.setActivity(intent.getComponent());
        }
        ShortcutInfo shortcutInfo = builder
                .build();
        return shortcutInfo;
    }

    /**
     * 查询是否存在某个shortcutName的快捷方式
     *
     * @param context
     * @return
     */
    public static boolean hasShortcut(Context context, String shortcutName) {
        String authority = getAuthorityFromPermission(context);
        if (authority == null)
            return false;
        String url = "content://" + authority + "/favorites?notify=true";
        Cursor cursor = null;
        try {
            Uri CONTENT_URI = Uri.parse(url);
            cursor = context.getContentResolver().query(CONTENT_URI, null, " iconPackage= ? ", new String[]{BuildConfig.APPLICATION_ID}, null);
            while (cursor != null && cursor.moveToNext()) {
                String name = cursor.getString(cursor.getColumnIndex("title")).toString();
                if (TextUtils.isEmpty(name))
                    continue;
                if (TextUtils.equals(shortcutName, name))
                    return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null && !cursor.isClosed())
                cursor.close();
        }
        return false;
    }

    public static boolean hasShortcut(Context context, String shortcutName, String strInIntent) {
        String authority = getAuthorityFromPermission(context);
        boolean exist = hasShortcut(context, shortcutName, strInIntent, authority);

        if (exist) {
            return true;
        }

        String authority1 = getAuthorityFromPermission1(context, "com.android.launcher.permission.READ_SETTINGS");
        return hasShortcut(context, shortcutName, strInIntent, authority1);
    }

    /**
     * 查询是否存在某个shortcutName的快捷方式
     *
     * @param context
     * @param shortcutName 名称
     * @param strInIntent  intent中包含的字符串,双重判断。因为不同语言下，shortcutName可能会不同
     * @return
     */
    public static boolean hasShortcut(Context context, String shortcutName, String strInIntent, String authority) {
//        String authority = getAuthorityFromPermission(context);
        if (authority == null)
            return false;
        String url = "content://" + authority + "/favorites?notify=true";
        Cursor cursor = null;
        try {
            Uri CONTENT_URI = Uri.parse(url);
            cursor = context.getContentResolver().query(CONTENT_URI, null, " iconPackage= ? ", new String[]{BuildConfig.APPLICATION_ID}, null);
            while (cursor != null && cursor.moveToNext()) {
                String name = cursor.getString(cursor.getColumnIndex("title")).toString();
                if (TextUtils.isEmpty(name))
                    continue;
                if (TextUtils.equals(shortcutName, name))
                    return true;
                String intent = cursor.getString(cursor.getColumnIndex("intent"));
                if (TextUtils.isEmpty(intent))
                    return false;
                if (intent.contains(strInIntent)) {
                    return true;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null && !cursor.isClosed())
                cursor.close();
        }
        return false;
    }

    public static String getAuthorityFromPermission(Context context) {
        // 先得到默认的Launcher
        try {
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_HOME);
            PackageManager mPackageManager = context.getPackageManager();
            ResolveInfo resolveInfo = mPackageManager.resolveActivity(intent, 0);
            if (resolveInfo == null)
                return null;
            List<ProviderInfo> info = mPackageManager.queryContentProviders(resolveInfo.activityInfo.packageName, resolveInfo.activityInfo.applicationInfo.uid, PackageManager.GET_PROVIDERS);
            if (info != null) {
                for (int j = 0; j < info.size(); j++) {
                    ProviderInfo provider = info.get(j);
                    if (provider.readPermission == null)
                        continue;
                    if (Pattern.matches(".*launcher.*READ_SETTINGS", provider.readPermission))
                        return provider.authority;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String getAuthorityFromPermission1(Context context, String permission) {
        if (TextUtils.isEmpty(permission)) {
            return null;
        }
        List<PackageInfo> packInfos = context.getPackageManager().getInstalledPackages(PackageManager.GET_PROVIDERS);
        if (packInfos == null) {
            return null;
        }
        for (PackageInfo info : packInfos) {
            ProviderInfo[] providers = info.providers;
            if (providers != null) {
                for (ProviderInfo provider : providers) {
                    if (permission.equals(provider.readPermission) || permission.equals(provider.writePermission)) {
                        return provider.authority;
                    }
                }
            }
        }
        return null;
    }

    /**
     * 检查是否有创建快捷方式权限   8.0及以上使用
     *
     * @param context
     * @return
     */
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public static boolean checkPermission(Context context) {
        AppOpsManager appOpsManager = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
        int result = AppOpsManager.MODE_DEFAULT;
        try {
            Class<?> appOpsManagerClass = Class.forName("android.app.AppOpsManager");
            Class[] args = new Class[3];
            args[0] = int.class;
            args[1] = int.class;
            args[2] = String.class;
            Method checkOP = appOpsManagerClass.getDeclaredMethod("checkOp", args);
            checkOP.setAccessible(true);
            result = (int) checkOP.invoke(appOpsManager, 10017, Binder.getCallingUid(), context.getPackageName());
        } catch (Exception e) {
        }
        if (result != AppOpsManager.MODE_ALLOWED) {
            return false;
        }
        return true;
    }

    /**
     * 创建要跳转的intent
     *
     * @param packageName 包名
     * @param className   类名（全名）
     * @param link        跳转链接
     * @return
     */
    public static Intent createIntent(String packageName, String className, String link) {
        return createIntent(packageName, className, link, true);
    }

    public static Intent createIntent(String packageName, String className, String link, boolean clearTop) {
        Log.i(TAG, "createIntent() called with: packageName = [" + packageName + "], className = [" + className + "], link = [" + link + "], clearTop = [" + clearTop + "]");
        if (TextUtils.isEmpty(packageName)) {
            throw new NullPointerException("craete shortcut intent. The packageName is null or the className is null");
        }
        Intent intent = new Intent();
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (clearTop) {
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        }

        intent.setAction(Intent.ACTION_VIEW);
        intent.setPackage(packageName);
        if (className != null) {
            intent.setComponent(new ComponentName(packageName, className));
        }
        if (link != null) {
            intent.setData(Uri.parse(link));
        }
        return intent;
    }

    /**
     * 跳转设置页。 手动添加权限时使用
     *
     * @param context
     */
    public static void jumpToSystemSettingPage(Context context) {
        if (context instanceof Activity) {
            Intent intent1 = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            Uri uri = Uri.fromParts("package", context.getPackageName(), null);
            intent1.setData(uri);
            context.startActivity(intent1);
        }
    }

    /**
     * 8.0及以上添加快捷方式回调是以广播形式接收
     */
    public class OnSuccessReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (mSuccessCallback == null) {
                return;
            }

            OnShortcutCallback callback = mSuccessCallback.get();
            if (callback == null) {
                return;
            }

            callback.onSuccess();
            mSuccessCallback.clear();
            mSuccessCallback = null;
        }
    }

    public interface OnShortcutCallback {
        void onSuccess();
    }


    /**
     * 检测系统版本是否是Android 8.0(API_26)以上版本
     *
     * @return
     */
    public static boolean equalAPI_26_OREO() {
        return Build.VERSION.SDK_INT >= 26;
    }

}
