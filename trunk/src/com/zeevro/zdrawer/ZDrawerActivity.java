package com.zeevro.zdrawer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.PopupMenu.OnMenuItemClickListener;
import android.widget.TextView;
import android.widget.Toast;

public class ZDrawerActivity extends Activity {
    Context                      mContext      = this;

    TextView                     mCategoryLabel;
    GridView                     mAppsGrid;

    SharedPreferences            mPrefs;

    MyAppInfo[]                  mApps;
    HashMap<String, Integer>     mAppNames     = new HashMap<String, Integer>();

    Categories                   mCategories;
    HashMap<String, AppsAdapter> mCategoryApps = new HashMap<String, AppsAdapter>();
    String                       mCurrentCategory;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);

        loadApps();
        setContentView(R.layout.main);

        mCategoryLabel = (TextView)findViewById(R.id.mainCategoryLabel);
        mAppsGrid = (GridView)findViewById(R.id.mainAppsGrid);
        mPrefs = getPreferences(Context.MODE_PRIVATE);

        mCategories = new Categories(mPrefs.getString("categories", "System,General,Games").split(","));

        for (String cat : mCategories.getAll()) {
            mCategoryApps.put(cat, new AppsAdapter());
        }

        mCategoryApps.get("All").addAll(mApps);
        mCategoryApps.get("Unfiled").addAll(mApps);

        String appsCats = mPrefs.getString("appsCategories", "");
        if (appsCats.length() > 0) {
            for (String app_cat : appsCats.split(",")) {
                String[] appCatArr = app_cat.split(":");
                String app = appCatArr[0], cat = appCatArr[1];

                if (mAppNames.containsKey(app)) {
                    mApps[mAppNames.get(app)].categorize(cat);
                }
            }
        }

        mCurrentCategory = mPrefs.getString("currentCategory", "");
        if (!mCategories.allContains(mCurrentCategory)) {
            mCurrentCategory = "All";
        }

        mCategoryLabel.setText(mCurrentCategory);

        mAppsGrid.setAdapter(mCategoryApps.get(mCurrentCategory));

        bindListeners();
    }

    @Override
    protected void onPause() {
        super.onPause();

        Editor editor = mPrefs.edit();

        ArrayList<String> appsCats = new ArrayList<String>();
        for (MyAppInfo app : mApps) {
            appsCats.add(app.toString() + ":" + app.category);
        }

        editor.putString("categories", TextUtils.join(",", mCategories.getEditable()));
        editor.putString("appsCategories", TextUtils.join(",", appsCats));
        editor.putString("currentCategory", mCurrentCategory);

        editor.commit();
    }

    class Categories {
        ArrayList<String> mCategoriesList = new ArrayList<String>();

        public Categories() {
        }

        public Categories(String[] categories) {
            addAll(categories);
        }

        public boolean add(String category) {
            if (addNoSort(category)) {
                sort();
                return true;
            }
            return false;
        }

        public void addAll(String[] categories) {
            for (String category : categories) {
                addNoSort(category);
            }

            sort();
        }

        public boolean remove(String category) {
            return mCategoriesList.remove(category);
        }

        public boolean allContains(String category) {
            return category.equals("All") || category.equals("Unfiled") || mCategoriesList.contains(category);
        }

        public boolean categorizableContains(String category) {
            return category.equals("Unfiled") || mCategoriesList.contains(category);
        }

        @SuppressWarnings("unchecked")
        public ArrayList<String> getAll() {
            ArrayList<String> ret = (ArrayList<String>)mCategoriesList.clone();
            ret.add(0, "All");
            ret.add("Unfiled");

            return ret;
        }

        @SuppressWarnings("unchecked")
        public ArrayList<String> getCategorizable() {
            ArrayList<String> ret = (ArrayList<String>)mCategoriesList.clone();
            ret.add("Unfiled");

            return ret;
        }

        @SuppressWarnings("unchecked")
        public ArrayList<String> getEditable() {
            return (ArrayList<String>)mCategoriesList.clone();
        }

        private boolean addNoSort(String category) {
            if (category.isEmpty() || allContains(category)) {
                return false;
            }

            mCategoriesList.add(category);
            return true;
        }

        private void sort() {
            Collections.sort(mCategoriesList);
        }
    }

    class MyAppInfo {
        public final String   title;
        public final Drawable icon;
        public final Drawable smallIcon;
        public final Intent   intent;
        public String         category = "Unfiled";

        private final String  mPackage;
        private final String  mActivity;

        public MyAppInfo(PackageManager manager, ResolveInfo resolveInfo) {
            mPackage = resolveInfo.activityInfo.packageName;
            mActivity = resolveInfo.activityInfo.name;

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

        public boolean categorize(String newCategory) {
            if (!mCategories.categorizableContains(newCategory)) {
                return false;
            }

            mCategoryApps.get(category).remove(this);
            mCategoryApps.get(newCategory).add(this);
            mCategoryApps.get(newCategory).sort(new Comparator<MyAppInfo>() {
                @Override
                public int compare(MyAppInfo lhs, MyAppInfo rhs) {
                    return lhs.title.compareToIgnoreCase(rhs.title);
                }
            });

            category = newCategory;

            return true;
        }

        @Override
        public String toString() {
            return mPackage + "." + mActivity;
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

    class CategoriesAdapter extends ArrayAdapter<String> {
        public CategoriesAdapter() {
            super(mContext, 0, mCategories.getEditable());
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            String cat = getItem(position);

            TextView tv = new TextView(parent.getContext());

            tv.setText(cat);
            tv.setLines(1);
            tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 25);

            return tv;
        }
    }

    class AppsCategoriesAdapter extends AppsAdapter {
        public AppsCategoriesAdapter() {
            super(mApps);
        }

        @Override
        public View getView(int position, View convertView, final ViewGroup parent) {
            final MyAppInfo info = getItem(position);
            final LayoutInflater inflater = LayoutInflater.from(parent.getContext());

            final LinearLayout view = (LinearLayout)inflater.inflate(R.layout.app_categories, null);

            final TextView app = (TextView)view.findViewById(R.id.appCategoriesApp);
            app.setText(info.title);
            app.setCompoundDrawablesWithIntrinsicBounds(info.smallIcon, null, null, null);

            final TextView cat = (TextView)view.findViewById(R.id.appCategoriesCategory);
            cat.setText(info.category);

            return view;
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
            ArrayList<String> allCategories = mCategories.getAll();
            int current_position = allCategories.indexOf(mCurrentCategory);
            int next_position = current_position + 1;

            if (next_position >= allCategories.size()) {
                next_position = 0;
            }

            mCurrentCategory = allCategories.get(next_position);
            mCategoryLabel.setText(mCurrentCategory);

            mAppsGrid.setAdapter(mCategoryApps.get(mCurrentCategory));
        }
    }

    class EditButtonOnClick implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            final AlertDialog.Builder nameDialog = new AlertDialog.Builder(mContext);
            final ArrayList<String> categorizableCategories = mCategories.getCategorizable();

            ListView appsCategories = new ListView(nameDialog.getContext());
            appsCategories.setAdapter(new AppsCategoriesAdapter());
            appsCategories.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
            appsCategories.setOnItemClickListener(new OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, final View view, final int position, long id) {
                    final AlertDialog.Builder categoryChooser = new AlertDialog.Builder(parent.getContext());
                    categoryChooser.setTitle("Select category:");
                    categoryChooser.setItems(categorizableCategories.toArray(new String[0]), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            String category = categorizableCategories.get(which);
                            if (mApps[position].categorize(category)) {
                                ((TextView)view.findViewById(R.id.appCategoriesCategory)).setText(category);
                            }
                        }
                    });

                    categoryChooser.show();
                }
            });

            nameDialog.setTitle("Categories");
            nameDialog.setPositiveButton("OK", null);
            nameDialog.setView(appsCategories);

            nameDialog.show();
        }
    }

    class CategoryLabelOnClick implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            final Dialog categoriesDialog = new Dialog(mContext);
            categoriesDialog.setContentView(R.layout.categories);

            final ListView categoriesListView = (ListView)categoriesDialog.findViewById(R.id.categoriesListView);
            final Capsule<String> selectedCategory = new Capsule<String>();

            categoriesDialog.setTitle("Categories");

            categoriesListView.setAdapter(new CategoriesAdapter());
            categoriesListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                    selectedCategory.setValue(((TextView)arg1).getText().toString());
                }
            });

            ((Button)categoriesDialog.findViewById(R.id.categoriesOkButton)).setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    categoriesDialog.dismiss();
                }
            });

            ((Button)categoriesDialog.findViewById(R.id.categoriesNewButton)).setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    final AlertDialog.Builder nameDialog = new AlertDialog.Builder(categoriesDialog.getContext());
                    final EditText nameEditor = new EditText(nameDialog.getContext());
                    nameEditor.setFreezesText(true);

                    nameDialog.setTitle("New category:");
                    nameDialog.setView(nameEditor);
                    nameDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            ((InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE)).toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0);
                        }
                    });
                    nameDialog.setPositiveButton("Add", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            String categoryName = nameEditor.getText().toString();
                            if (!mCategories.add(categoryName)) {
                                toast("Invalid category name");
                                selectedCategory.setValue(null);
                                ((InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE)).toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0);
                                return;
                            }

                            categoriesListView.setAdapter(new CategoriesAdapter());
                            mCategoryApps.put(categoryName, new AppsAdapter());

                            selectedCategory.setValue(null);
                        }
                    });

                    nameDialog.show();

                    nameEditor.requestFocus();

                    ((InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE)).toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
                }
            });

            ((Button)categoriesDialog.findViewById(R.id.categoriesEditButton)).setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (selectedCategory.getValue() == null) {
                        return;
                    }

                    final AlertDialog.Builder nameDialog = new AlertDialog.Builder(categoriesDialog.getContext());
                    final EditText nameEditor = new EditText(nameDialog.getContext());
                    final String oldName = selectedCategory.getValue();
                    nameEditor.setFreezesText(true);
                    nameEditor.setText(oldName);
                    nameEditor.selectAll();

                    nameDialog.setTitle("New name:");
                    nameDialog.setView(nameEditor);
                    nameDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            ((InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE)).toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0);
                        }
                    });
                    nameDialog.setPositiveButton("Rename", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            String newName = nameEditor.getText().toString();

                            if (!mCategories.add(newName)) {
                                toast("Invalid category name");
                                selectedCategory.setValue(null);
                                ((InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE)).toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0);
                                return;
                            }

                            toast("Renaming " + oldName + " --> " + newName);
                            mCategories.remove(oldName);

                            categoriesListView.setAdapter(new CategoriesAdapter());
                            mCategoryApps.put(newName, new AppsAdapter());
                            for (MyAppInfo app : mApps) {
                                if (app.category == oldName) {
                                    app.categorize(newName);
                                    toast("Categorized " + app.title + ": " + oldName + " --> " + newName);
                                }
                            }
                            mCategoryApps.remove(oldName);
                            if (mCurrentCategory == oldName) {
                                mCurrentCategory = newName;
                                mCategoryLabel.setText(mCurrentCategory);
                                mAppsGrid.setAdapter(mCategoryApps.get(mCurrentCategory));
                            }

                            selectedCategory.setValue(null);

                            ((InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE)).toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0);

                            toast("Done.");
                        }
                    });

                    nameDialog.show();

                    nameEditor.requestFocus();

                    ((InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE)).toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
                }
            });

            ((Button)categoriesDialog.findViewById(R.id.categoriesRemoveButton)).setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    String categoryName = selectedCategory.getValue();
                    if (!mCategories.remove(categoryName)) {
                        selectedCategory.setValue(null);
                        return;
                    }
                    categoriesListView.setAdapter(new CategoriesAdapter());
                    for (MyAppInfo app : mApps) {
                        if (app.category == categoryName) {
                            app.categorize("Unfiled");
                            toast("Categorized " + app.title + ": " + categoryName + " --> Unfiled");
                        }
                    }
                    mCategoryApps.remove(categoryName);
                    if (mCurrentCategory == categoryName) {
                        mCurrentCategory = "All";
                        mCategoryLabel.setText(mCurrentCategory);
                        mAppsGrid.setAdapter(mCategoryApps.get(mCurrentCategory));
                    }
                    selectedCategory.setValue(null);
                }
            });

            categoriesDialog.show();
        }
    }

    class MenuButtonOnClick implements View.OnClickListener, OnMenuItemClickListener {
        @Override
        public void onClick(View v) {
            PopupMenu menu = new PopupMenu(v.getContext(), v);

            menu.setOnMenuItemClickListener(this);
            menu.inflate(R.menu.main);

            menu.show();
        }

        @Override
        public boolean onMenuItemClick(MenuItem item) {
            switch (item.getItemId()) {
                case R.id.itemEditApps:
                    new EditButtonOnClick().onClick(null);
                    break;

                case R.id.itemEditCategories:
                    new CategoryLabelOnClick().onClick(null);
                    break;
            }

            return true;
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

        for (int i = 0; i < mApps.length; i++) {
            mAppNames.put(mApps[i].toString(), i);
        }
    }

    private void bindListeners() {
        mAppsGrid.setOnItemClickListener(new AppsOnItemClick());
        mAppsGrid.setOnItemLongClickListener(new AppsOnItemLongClick());

        mCategoryLabel.setOnClickListener(new CategoryLabelOnClick());

        View home_button = findViewById(R.id.mainHomeButton);
        home_button.setOnClickListener(new HomeButtonOnClick());

        View edit_button = findViewById(R.id.mainEditButton);
        edit_button.setOnClickListener(new EditButtonOnClick());

        View menu_button = findViewById(R.id.mainMenuButton);
        menu_button.setOnClickListener(new MenuButtonOnClick());
    }

    class Capsule<T> {
        private T inner = null;

        public Capsule() {
        }

        public Capsule(T x) {
            inner = x;
        }

        public void setValue(T x) {
            inner = x;
        }

        public T getValue() {
            return inner;
        }
    }

    private void toast(CharSequence text) {
        Toast.makeText(mContext, text, Toast.LENGTH_SHORT).show();
    }
}
