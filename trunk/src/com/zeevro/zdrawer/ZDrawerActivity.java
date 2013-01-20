package com.zeevro.zdrawer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Dictionary;
import java.util.List;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.TextView;

public class ZDrawerActivity extends Activity {
    TextView                              mCategoryLabel;
    GridView                              mAppsGrid;

    int                                   mCurrentCategory = 0;
    CharSequence[]                        mAllCategories   = { "All" };
    Dictionary<CharSequence, AppsAdapter> mCategories;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.requestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.main);

        mCategoryLabel = (TextView)findViewById(R.id.textView1);
        mCategoryLabel.setText(mAllCategories[mCurrentCategory]);

        mAppsGrid = (GridView)findViewById(R.id.gridView1);
        PackageManager manager = getPackageManager();
        Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> resolveInfos = manager.queryIntentActivities(mainIntent, 0);
        Collections.sort(resolveInfos, new ResolveInfo.DisplayNameComparator(manager));
        AppsAdapter aa = new AppsAdapter(mAppsGrid.getContext());
        mAppsGrid.setAdapter(aa);
        for (ResolveInfo info : resolveInfos) {
            aa.add(new MyAppInfo(manager, info));
        }

        mAppsGrid.setOnItemClickListener(new AppsOnItemClick());

        View home_button = findViewById(R.id.imageView1);
        home_button.setOnClickListener(new HomeButtonOnClick());
    }

    class MyAppInfo {
        public CharSequence title;
        public Drawable     icon;
        public Intent       intent;

        public MyAppInfo(PackageManager manager, ResolveInfo resolveInfo) {
            title = resolveInfo.loadLabel(manager);
            icon = resize(resolveInfo.activityInfo.applicationInfo.loadIcon(manager));
            intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_LAUNCHER);
            intent.setComponent(new ComponentName(resolveInfo.activityInfo.applicationInfo.packageName, resolveInfo.activityInfo.name));
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        }

        private Drawable resize(Drawable image) {
            Bitmap d = ((BitmapDrawable)image).getBitmap();
            Bitmap bitmapOrig = Bitmap.createScaledBitmap(d, 200, 200, false);
            return new BitmapDrawable(bitmapOrig);
        }
    }

    class AppsAdapter extends ArrayAdapter<MyAppInfo> {
        public AppsAdapter(Context context) {
            super(context, 0);
        }

        public AppsAdapter(Context context, MyAppInfo[] apps) {
            super(context, 0, apps);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            MyAppInfo info = this.getItem(position);

            TextView tv = new TextView(parent.getContext());
            tv.setText(info.title);
            tv.setCompoundDrawablesWithIntrinsicBounds(null, info.icon, null, null);
            tv.setLines(2);
            tv.setGravity(Gravity.CENTER);
            tv.setPadding(0, 15, 0, 10);

            return tv;
        }
    }

    class AppsOnItemClick implements AdapterView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
            Intent intent = ((AppsAdapter)parent.getAdapter()).getItem(position).intent;
            startActivity(intent);
        }
    }

    class HomeButtonOnClick implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            if (mCategoryLabel.getText() == "All") {
                mCategoryLabel.setText("General");
            } else {
                mCategoryLabel.setText("All");
            }
        }
    }

    private AppsAdapter[] loadApps(CharSequence[] categories) {
        ArrayList<AppsAdapter> adapters = new ArrayList<AppsAdapter>();

        return (AppsAdapter[])adapters.toArray();
    }
}