package mobi.wrt.android.smartcontacts.app;

import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.TypedArray;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AppCompatDelegate;
import android.util.TypedValue;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import java.lang.reflect.Field;
import java.util.List;

import by.istin.android.xcore.analytics.ITracker;
import by.istin.android.xcore.callable.ISuccess;
import by.istin.android.xcore.ui.DialogBuilder;
import by.istin.android.xcore.utils.StringUtil;
import by.istin.android.xcore.utils.UiUtil;
import mobi.wrt.android.smartcontacts.R;
import mobi.wrt.android.smartcontacts.helper.ContactHelper;
import mobi.wrt.android.smartcontacts.utils.ThemeUtils;

/**
 * Created by IstiN on 18.02.2015.
 */
public class BaseControllerActivity extends AppCompatActivity {

    public ITracker getTracker() {
        return ITracker.Impl.get(this);
    }

    public void setStatusBarColor(int color){
        if (UiUtil.hasKitKat()) {
            Window w = getWindow();
            w.setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS, WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            w.getDecorView().setBackgroundColor(color);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ThemeUtils.onActivityCreate(this);
        refreshStatusBar();
    }

    private void refreshStatusBar() {
        if (UiUtil.setTranslucentStatus(this)) {
            TypedArray a = getTheme().obtainStyledAttributes(new int[] {android.support.v7.appcompat.R.attr.colorPrimaryDark});
            int colorValue = a.getColor(0, 0);
            setStatusBarColor(colorValue);
            a.recycle();
        }
    }

    public void onContactMoreClick(View view) {
        final Object tag = view.getTag();
        if (tag instanceof String) {
            String phone = (String) tag;
            addContact(this, phone);
            getTracker().track("onContactMoreClick:new");
            return;
        }
        Long id = (Long) tag;
        openContact(id);
        getTracker().track("onContactMoreClick:open");
    }

    public static void addContact(Context context, String phone) {
        ContactHelper.get(context).removePhoneCache(phone);
        Intent intent = new Intent(Intent.ACTION_INSERT);
        intent.setType(ContactsContract.Contacts.CONTENT_TYPE);
        intent.putExtra(ContactsContract.Intents.Insert.PHONE, phone);
        context.startActivity(intent);
    }

    private void openContact(Long id) {
        Uri uri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI, String.valueOf(id));
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        startActivity(intent);
    }

    public void onRecentContactClick(View view) {
        Object tag = view.getTag();
        if (tag instanceof Long) {
            openContact((Long)tag);
            getTracker().track("onRecentContactClick:open");
            return;
        }
        makeCall(BaseControllerActivity.this, (String)tag);
        getTracker().track("onRecentContactClick:call");
    }

    public void onContactClick(final View view) {
        final Long id = (Long) view.getTag();
        new Thread(new Runnable() {
            @Override
            public void run() {
                initPhoneNumbers(id, new ISuccess<List<ContentValues>>() {
                    @Override
                    public void success(List<ContentValues> contentValueses) {
                        final String[] numbers = new String[contentValueses.size()];
                        for (int i = 0; i < contentValueses.size(); i++) {
                            numbers[i] = contentValueses.get(i).getAsString(ContactsContract.CommonDataKinds.Phone.NUMBER);
                        }
                        getTracker().track("onContactClick:options");
                        DialogBuilder.options(BaseControllerActivity.this, R.string.title_choose_number, numbers, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                getTracker().track("onContactClick:options:call");
                                makeCall(BaseControllerActivity.this, numbers[which]);
                            }
                        });
                    }

                }, new ISuccess<String>() {
                    @Override
                    public void success(String s) {
                        getTracker().track("onContactClick:call");
                        makeCall(BaseControllerActivity.this, s);
                    }
                });
            }
        }).start();
    }

    public void initPhoneNumbers(Long id, final ISuccess<List<ContentValues>> result, final ISuccess<String> success) {
        final List<ContentValues> entities = ContactHelper.get(this).getPhonesById(id);
        if (entities != null && !entities.isEmpty()) {
            if (entities.size() == 1) {
                final String phoneNumber = entities.get(0).getAsString(ContactsContract.CommonDataKinds.Phone.NUMBER);
                if (!StringUtil.isEmpty(phoneNumber)) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            success.success(phoneNumber);
                        }
                    });
                }
            } else {
                for (ContentValues phoneValues : entities) {
                    if (phoneValues.getAsInteger(ContactsContract.CommonDataKinds.Phone.IS_SUPER_PRIMARY) > 0) {
                        final String phone = phoneValues.getAsString(ContactsContract.CommonDataKinds.Phone.NUMBER);
                        if (!StringUtil.isEmpty(phone)) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    success.success(phone);
                                }
                            });
                        }
                        return;
                    }
                }
                runOnUiThread(
                        new Runnable() {
                            @Override
                            public void run() {
                                result.success(entities);
                            }
                        }
                );
            }
        }
    }

    public static void makeCall(Context context, String phone) {
        Intent intent = new Intent(Intent.ACTION_CALL, Uri.parse("tel:"+StringUtil.encode(phone)));
        context.startActivity(intent);
    }

    public static void sendSms(Context context, String phone) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse("sms:" + StringUtil.encode(phone)));
        context.startActivity(intent);
    }

    @Override
    public boolean supportRequestWindowFeature(int featureId) {
        boolean result = super.supportRequestWindowFeature(featureId);
        if (featureId == Window.FEATURE_ACTION_BAR) {
            Class<? extends AppCompatDelegate> aClass = getDelegate().getClass();
            Field mWindowNoTitle = getField(aClass, "mWindowNoTitle");
            if (mWindowNoTitle != null) {
                try {
                    mWindowNoTitle.setAccessible(true);
                    mWindowNoTitle.set(getDelegate(), false);
                    refreshStatusBar();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
        return result;
    }

    private static Field getField(Class clazz, String fieldName) {
        try {
            return clazz.getDeclaredField(fieldName);
        } catch (NoSuchFieldException e) {
            Class superClass = clazz.getSuperclass();
            if (superClass == null) {
                return null;
            } else {
                return getField(superClass, fieldName);
            }
        }
    }
}
