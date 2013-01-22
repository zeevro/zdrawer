package com.zeevro.zdrawer;

import java.util.ArrayList;
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
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

public class ZDrawerActivity extends Activity {
    Context                      mContext        = this;

    TextView                     mCategoryLabel;
    GridView                     mAppsGrid;

    SharedPreferences            mPrefs;

    MyAppInfo[]                  mApps;

    ArrayList<String>            mCategoryNames  = new ArrayList<String>();
    HashMap<String, String>      mAppsCategories = new HashMap<String, String>();
    HashMap<String, AppsAdapter> mCategoryApps   = new HashMap<String, AppsAdapter>();
    String                       mCurrentCategory;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.main);

        mCategoryLabel = (TextView)findViewById(R.id.mainCategoryLabel);
        mAppsGrid = (GridView)findViewById(R.id.mainAppsGrid);
        mPrefs = getPreferences(Context.MODE_PRIVATE);

        loadApps();

        mCategoryNames.add("All");
        String cats = mPrefs.getString("categories", "System,General,Games");
        if (cats.length() > 0) {
            for (String cat : cats.split(",")) {
                mCategoryNames.add(cat);
            }
        }
        mCategoryNames.add("Unfiled");

        for (String cat : mCategoryNames) {
            mCategoryApps.put(cat, new AppsAdapter());
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

        mAppsGrid.setAdapter(new AppsAdapter(mApps));

        bindListeners();
    }

    class MyAppInfo {
        public final String   title;
        public final Drawable icon;
        public final Drawable smallIcon;
        public final Intent   intent;

        public MyAppInfo(PackageManager manager, ResolveInfo resolveInfo) {
            Resizer big_resizer = new Resizer(100);
            Resizer small_resizer = new Resizer(50, TypedValue.COMPLEX_UNIT_SP);

            title = (String)resolveInfo.loadLabel(manager);

            Drawable orig_icon = resolveInfo.activityInfo.loadIcon(manager);
            icon = big_resizer.resize(orig_icon);
            smallIcon = small_resizer.resize(orig_icon);

            intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_LAUNCHER);
            intent.setComponent(new ComponentName(resolveInfo.activityInfo.applicationInfo.packageName, resolveInfo.activityInfo.name));
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        }
    }

    class Resizer {
        private final float mPixels;

        public Resizer(float size) {
            this(size, TypedValue.COMPLEX_UNIT_DIP);
        }

        public Resizer(float size, int unit) {
            DisplayMetrics metrics = getResources().getDisplayMetrics();
            mPixels = TypedValue.applyDimension(unit, size, metrics);

            // DisplayMetrics metrics = new DisplayMetrics();
            // getWindowManager().getDefaultDisplay().getMetrics(metrics);
            // mPixels = TypedValue.applyDimension(unit, size, metrics);
        }

        public float getPixels() {
            return mPixels;
        }

        public Drawable resize(Drawable image) {
            Bitmap d = ((BitmapDrawable)image).getBitmap();
            Bitmap bitmapOrig = Bitmap.createScaledBitmap(d, (int)mPixels, (int)mPixels, false);
            return new BitmapDrawable(bitmapOrig);
        }
    }

    class AppsAdapter extends ArrayAdapter<MyAppInfo> {
        public AppsAdapter() {
            super(mContext, 0);
        }

        public AppsAdapter(MyAppInfo[] apps) {
            super(mContext, 0, apps);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            MyAppInfo info = getItem(position);

            TextView tv = new TextView(parent.getContext());
            tv.setText(info.title);
            tv.setCompoundDrawablesWithIntrinsicBounds(null, info.icon, null, null);
            tv.setGravity(Gravity.CENTER);
            tv.setLines(2);
            tv.setPadding(0, 15, 0, 10);

            return tv;
        }
    }

    class AppsCategoriesAdapter extends AppsAdapter {
        public AppsCategoriesAdapter() {
            super(mApps);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            MyAppInfo info = getItem(position);
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());

            LinearLayout view = (LinearLayout)inflater.inflate(R.layout.app_categories, null);
            TextView tv = (TextView)view.findViewById(R.id.appCategoriesText);
            tv.setText(info.title);
            tv.setCompoundDrawablesWithIntrinsicBounds(info.smallIcon, null, null, null);

            return view;
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

    class EditButtonOnClick implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            AlertDialog.Builder nameDialog = new AlertDialog.Builder(mContext);

            nameDialog.setAdapter(new AppsCategoriesAdapter(), null);

            nameDialog.show();
        }
    }

    class CategoryLabelOnClick implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            final Dialog dialog = new Dialog(mContext);
            dialog.setContentView(R.layout.categories);
            dialog.setTitle("Categories");

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

    private void loadApps() {
        PackageManager manager = getPackageManager();
        Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> resolveInfos = manager.queryIntentActivities(mainIntent, 0);
        Collections.sort(resolveInfos, new ResolveInfo.DisplayNameComparator(manager));

        ArrayList<MyAppInfo> apps = new ArrayList<MyAppInfo>();

        for (ResolveInfo info : resolveInfos) {
            apps.add(new MyAppInfo(manager, info));
        }

        mApps = apps.toArray(new MyAppInfo[0]);
    }

    private void bindListeners() {
        mAppsGrid.setOnItemClickListener(new AppsOnItemClick());
        mAppsGrid.setOnItemLongClickListener(new AppsOnItemLongClick());

        mCategoryLabel.setOnClickListener(new CategoryLabelOnClick());

        View home_button = findViewById(R.id.mainHomeButton);
        home_button.setOnClickListener(new HomeButtonOnClick());

        View edit_button = findViewById(R.id.mainEditButton);
        edit_button.setOnClickListener(new EditButtonOnClick());
    }
}