package com.zeevro.zdrawer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ListView;
import android.widget.TextView;

public class ZDrawerActivity extends Activity {
    Context                      mContext        = this;

    TextView                     mCategoryLabel;
    GridView                     mAppsGrid;

    SharedPreferences            mPrefs;

    ArrayList<String>            mCategoryNames  = new ArrayList<String>();
    HashMap<String, String>      mAppsCategories = new HashMap<String, String>();
    HashMap<String, AppsAdapter> mCategoryApps   = new HashMap<String, AppsAdapter>();
    String                       mCurrentCategory;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.main);

        mCategoryLabel = (TextView)findViewById(R.id.textView1);
        mAppsGrid = (GridView)findViewById(R.id.gridView1);
        mPrefs = getPreferences(Context.MODE_PRIVATE);

        mAppsGrid.setOnItemClickListener(new AppsOnItemClick());
        mAppsGrid.setOnItemLongClickListener(new AppsOnItemLongClick());

        mCategoryLabel.setOnClickListener(new CategoryLabelOnClick());

        mCategoryNames.add("All");
        String cats = mPrefs.getString("categories", "");
        if (cats.length() > 0) {
            for (String cat : cats.split(",")) {
                mCategoryNames.add(cat);
            }
        }
        mCategoryNames.add("Unfiled");

        for (String cat : mCategoryNames) {
            mCategoryApps.put(cat, new AppsAdapter(mAppsGrid.getContext()));
        }

        String apps_cats = mPrefs.getString("appsCategories", "");
        if (apps_cats.length() > 0) {
            for (String app_cat : apps_cats.split(",")) {
                String[] app_cat_arr = app_cat.split(":");
                String app = app_cat_arr[0], cat = app_cat_arr[1];

                if (mCategoryNames.contains(cat)) {
                    mAppsCategories.put(app, cat);
                }
            }
        }

        mCurrentCategory = mPrefs.getString("currentCategory", "All");
        if (!mCategoryApps.containsKey(mCurrentCategory)) {
            mCurrentCategory = "All";
        }

        mCategoryLabel.setText(mCurrentCategory);

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

        View home_button = findViewById(R.id.imageView1);
        home_button.setOnClickListener(new HomeButtonOnClick());
    }

    class MyAppInfo {
        public CharSequence title;
        public Drawable     icon;
        public Intent       intent;

        public MyAppInfo(PackageManager manager, ResolveInfo resolveInfo) {
            title = resolveInfo.loadLabel(manager);
            icon = resize(resolveInfo.activityInfo.loadIcon(manager));
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
            MyAppInfo info = getItem(position);

            TextView tv = new TextView(parent.getContext());
            tv.setText(info.title);
            tv.setCompoundDrawablesWithIntrinsicBounds(null, info.icon, null, null);
            tv.setLines(2);
            tv.setGravity(Gravity.CENTER);
            tv.setPadding(0, 15, 0, 10);

            return tv;
        }
    }

    class CategoriesAdapter extends ArrayAdapter<String> {
        public CategoriesAdapter(Context context, String[] categories) {
            super(context, 0);

            for (String cat : categories) {
                if (cat != "All" && cat != "Unfiled") {
                    add(cat);
                }
            }
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            String cat = getItem(position);

            TextView tv = new TextView(parent.getContext());

            tv.setText(cat);
            tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 25);
            tv.setLines(1);

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

    class AppsOnItemLongClick implements AdapterView.OnItemLongClickListener {
        @Override
        public boolean onItemLongClick(AdapterView<?> parent, View v, int position, long id) {
            AlertDialog.Builder dlgAlert = new AlertDialog.Builder(parent.getContext());

            MyAppInfo app = ((AppsAdapter)parent.getAdapter()).getItem(position);

            dlgAlert.setMessage("You have long-clicked on " + app.title + ". This will be used to assign categories.");
            dlgAlert.setTitle(app.title);
            dlgAlert.setPositiveButton("GTFO!!", null);
            dlgAlert.setCancelable(true);
            dlgAlert.create().show();

            return false;
        }
    }

    class HomeButtonOnClick implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            int current_position = mCategoryNames.indexOf(mCurrentCategory);
            int next_position = current_position + 1;

            if (next_position >= mCategoryNames.size()) {
                next_position = 0;
            }

            mCurrentCategory = mCategoryNames.get(next_position);
            mCategoryLabel.setText(mCurrentCategory);
        }
    }

    class CategoryLabelOnClick implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            final Dialog dialog = new Dialog(mContext);
            dialog.setContentView(R.layout.categories);

            ((ListView)dialog.findViewById(R.id.categoriesListView)).setAdapter(new CategoriesAdapter(mContext, mCategoryNames.toArray(new String[0])));
            ((Button)dialog.findViewById(R.id.categoriesOkButton)).setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    dialog.dismiss();
                }
            });

            dialog.show();
        }
    }

    private AppsAdapter[] loadApps(Collection<String> categories) {
        ArrayList<AppsAdapter> adapters = new ArrayList<AppsAdapter>();

        return (AppsAdapter[])adapters.toArray();
    }
}