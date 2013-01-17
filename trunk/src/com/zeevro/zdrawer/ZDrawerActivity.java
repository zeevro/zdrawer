package com.zeevro.zdrawer;

import java.util.Collections;
import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.TextView;

public class ZDrawerActivity extends Activity {
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
    	
        GridLayout all_apps = (GridLayout)findViewById(R.id.gridLayout1);
        all_apps.setColumnCount(4);
        
        PackageManager manager = getPackageManager();
        Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);

        List<ResolveInfo> resolveInfos = manager.queryIntentActivities(mainIntent, 0);
        Collections.sort(resolveInfos, new ResolveInfo.DisplayNameComparator(manager));
        
        for (ResolveInfo info : resolveInfos) {
			ApplicationInfo applicationInfo = info.activityInfo.applicationInfo;
			 
			TextView tv = new TextView(all_apps.getContext());
			tv.setText(applicationInfo.loadLabel(manager));
			tv.setCompoundDrawablesWithIntrinsicBounds(null, applicationInfo.loadIcon(manager), null, null);
			int my_width = all_apps.getWidth();
			tv.setWidth(200);
			tv.setHeight(240);
			all_apps.addView(tv); 
        }
    }
}