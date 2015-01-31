package mobi.wrt.android.smartcontacts.fragments;

import android.content.ContentValues;
import android.database.Cursor;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.net.Uri;
import android.provider.ContactsContract;
import android.support.v4.app.FragmentActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import java.util.List;

import by.istin.android.xcore.fragment.collection.RecyclerViewFragment;
import by.istin.android.xcore.model.CursorModel;
import by.istin.android.xcore.utils.ContentUtils;
import by.istin.android.xcore.utils.StringUtil;
import mobi.wrt.android.smartcontacts.R;
import mobi.wrt.android.smartcontacts.fragments.adapter.SmartAdapter;

/**
 * Created by IstiN on 31.01.2015.
 */
public class SmartFragment extends RecyclerViewFragment<SmartAdapter.Holder, SmartAdapter, SmartFragment.SmartModel>  {

    public static final String PHONE_DELIMETER = "!=!";
    public static final String COLUMN_PHONES = "phones";
    public static final String COLUMN_PRIMARY_PHONE = "primary_phone";

    public void initPhoneNumbers(ContentValues contentValues, Long id) {
        List<ContentValues> entities = ContentUtils.getEntities(getActivity(), new String[]{ContactsContract.CommonDataKinds.Phone.NUMBER, ContactsContract.CommonDataKinds.Phone.IS_SUPER_PRIMARY}, ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = " + id, null);
        if (entities != null) {
            contentValues.put(COLUMN_PHONES, StringUtil.joinAll(PHONE_DELIMETER, entities, ContactsContract.CommonDataKinds.Phone.NUMBER));
            if (entities.size() == 1) {
                contentValues.put(COLUMN_PRIMARY_PHONE, entities.get(0).getAsString(ContactsContract.CommonDataKinds.Phone.NUMBER));
            } else {
                for (ContentValues phoneValues : entities) {
                    if (phoneValues.getAsInteger(ContactsContract.CommonDataKinds.Phone.IS_SUPER_PRIMARY) > 0) {
                        contentValues.put(COLUMN_PRIMARY_PHONE, phoneValues.getAsString(ContactsContract.CommonDataKinds.Phone.NUMBER));
                    }
                }
            }
        }
    }

    public static class SmartModel extends CursorModel{

        public SmartModel(Cursor cursor) {
            super(cursor);
        }

        public SmartModel(Cursor cursor, boolean isMoveToFirst) {
            super(cursor, isMoveToFirst);
        }

    }

    public static final String[] PROJECTION = new String[]{ContactsContract.Contacts._ID, ContactsContract.Contacts.DISPLAY_NAME, ContactsContract.Contacts.PHOTO_URI};
    public static final String SELECTION = ContactsContract.Contacts.HAS_PHONE_NUMBER + "=1 AND " + ContactsContract.Contacts.STARRED + "=1";
    public static final String ORDER = ContactsContract.Contacts.DISPLAY_NAME + " ASC";

    @Override
    public void onViewCreated(View view) {
        super.onViewCreated(view);
        getCollectionView().addItemDecoration(new RecyclerView.ItemDecoration() {

            @Override
            public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
                outRect.left = 2;
                outRect.right = 2;
                outRect.bottom = 2;

                // Add top margin only for the first item to avoid double space between items
                int childPosition = parent.getChildPosition(view);
                if(childPosition < 2) {
                    outRect.top = 4;
                }
            }

        });
    }

    @Override
    public CursorModel.CursorModelCreator<SmartModel> getCursorModelCreator() {
        return new CursorModel.CursorModelCreator<SmartModel>() {
            @Override
            public SmartModel create(Cursor cursor) {
                return new SmartModel(cursor);
            }
        };
    }

    @Override
    public int getViewLayout() {
        return R.layout.fragment_smart;
    }

    @Override
    public String[] getProjection() {
        return PROJECTION;
    }

    @Override
    public SmartAdapter createAdapter(FragmentActivity fragmentActivity, SmartModel cursor) {
        return new SmartAdapter(cursor);
    }

    @Override
    public void swap(SmartAdapter smartAdapter, SmartModel cursor) {
        getAdapter().swap(cursor);
    }

    @Override
    public String getSelection() {
        return SELECTION;
    }

    @Override
    public String getOrder() {
        return ORDER;
    }

    @Override
    public Uri getUri() {
        return ContactsContract.Contacts.CONTENT_URI;
    }

    @Override
    protected RecyclerView.LayoutManager createLayoutManager() {
        return new GridLayoutManager(getActivity(), 2);
    }

    @Override
    public String getUrl() {
        return null;
    }

    @Override
    public String getProcessorKey() {
        return null;
    }

}