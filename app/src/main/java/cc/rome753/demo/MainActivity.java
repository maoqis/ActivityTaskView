package cc.rome753.demo;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.view.View;

import androidx.annotation.DrawableRes;
import androidx.annotation.RequiresApi;

import cc.rome753.activitytask.ShortcutUtils;

import static cc.rome753.activitytask.ShortcutUtils.hasShortcut;
import static cc.rome753.activitytask.ShortcutUtils.updateShortcut;

public class MainActivity extends BaseActivity {
    private static final String TAG = "MainActivity";


    protected void init() {
        setContentView(R.layout.activity_base);
        setTitle(getClass().getSimpleName());
        addCheckBoxes();

    }


    public void createShortcut(View v) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            addOrUpdateIcon(this, IntentActivity.class.getSimpleName(), R.drawable.ic_launcher, IntentActivity.class.getName());
        }
    }




    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public boolean addOrUpdateIcon(Context context, String name, @DrawableRes int iconId, String CLASS_NAME) {
        Bitmap icon = drawableToBitmap(context.getResources().getDrawable(iconId, null));
        Intent intent = ShortcutUtils.createIntent(context.getPackageName(), CLASS_NAME, null, false);
        if (hasShortcut(context, name)) {

            updateShortcut(context, name, icon, intent);

            return true;

        } else {

            ShortcutUtils.createShortcut(context,
                    name,
                    icon,
                    intent);


            return false;
        }

    }

    public static final Bitmap drawableToBitmap(Drawable drawable) {
        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(),
                drawable.getOpacity() != PixelFormat.OPAQUE ? Bitmap.Config.ARGB_8888 : Bitmap.Config.RGB_565);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
        drawable.draw(canvas);
        return bitmap;
    }
}
