/*
 * Copyright (C) 2010 Sergej Shafarenka, beworx.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bwx.qs.battery;

import static com.bwx.qs.battery.BatteryWidget.PREFS;
import static com.bwx.qs.battery.BatteryWidget.PREF_ACTIVITY_NAME;
import static com.bwx.qs.battery.BatteryWidget.PREF_CLASS_NAME;
import static com.bwx.qs.battery.BatteryWidget.PREF_PACKAGE_NAME;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import android.app.ExpandableListActivity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.BaseExpandableListAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

public class SettingsActivityList extends ExpandableListActivity implements OnClickListener {

	static class Group {
	    Group(int titleTextId) {this.titleTextId = titleTextId;}
	    int titleTextId;
	    ArrayList<ResolveInfo> children = new ArrayList<ResolveInfo>();
	}
	
	static class FeaturedActivity {
	    FeaturedActivity(String className, String packageName) {
	        this.className = className;
	        this.packageName = packageName;
	    }
	    String className;
	    String packageName;
	}
	
	class ExpandableListAdapter extends BaseExpandableListAdapter {

	    private final ArrayList<Group> mGroups = new ArrayList<Group>();
	    private final LayoutInflater mInflater;
	    
	    public ExpandableListAdapter(LayoutInflater inflater) {
	        mInflater = inflater;
	        
	        // create featured activities
	        String[] params;
	        FeaturedActivity activity;
	        ArrayList<String> classNames = new ArrayList<String>();
	        String[] activities = SettingsActivityList.this.getResources().getStringArray(R.array.featured_activities);
	        for (int i=0; i<activities.length; i++) {
	            params = TextUtils.split(activities[i], "/");
	            mFeaturedActivities.add(activity = new FeaturedActivity(params[1], params[0]));
	            classNames.add(activity.className);
	        }
	        mFeaturedClassNames = classNames.toArray(new String[classNames.size()]);
	        Arrays.sort(mFeaturedClassNames);
	    }
	    
        public Object getChild(int groupPosition, int childPosition) {
            return mGroups.get(groupPosition).children.get(childPosition);
        }

        public long getChildId(int groupPosition, int childPosition) {
            return childPosition;
        }

        public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView,
                ViewGroup parent) {
            
            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.activity_item, parent, false);
            }

            Group group = mGroups.get(groupPosition);
            
            ActivityInfo activityInfo = group.children.get(childPosition).activityInfo;
            
            ImageView icon = (ImageView) convertView.findViewById(R.id.icon);
            TextView text1 = (TextView) convertView.findViewById(R.id.text1);
            Button button = (Button) convertView.findViewById(R.id.button1);
            button.setEnabled(true);
            button.setTag(activityInfo);
            button.setOnClickListener(SettingsActivityList.this);

            View item = convertView.findViewById(R.id.item);
            item.setTag(activityInfo);
            item.setOnClickListener(SettingsActivityList.this);

            PackageManager pm = mPackageManager;
            icon.setImageDrawable(activityInfo.loadIcon(pm));
            text1.setText(activityInfo.loadLabel(pm));

            return convertView;
        }

        public int getChildrenCount(int groupPosition) {
            return mGroups.get(groupPosition).children.size();
        }

        public Object getGroup(int groupPosition) {
            return mGroups.get(groupPosition);
        }

        public int getGroupCount() {
            return mGroups.size();
        }

        public long getGroupId(int groupPosition) {
            return groupPosition;
        }

        public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
            
            if (convertView == null) {
                convertView = mInflater.inflate(android.R.layout.simple_expandable_list_item_1, parent, false);
            }
            
            Group group = mGroups.get(groupPosition);
            ((TextView)convertView.findViewById(android.R.id.text1)).setText(group.titleTextId);
            
            return convertView;
        }

        public boolean hasStableIds() {
            return true;
        }

        public boolean isChildSelectable(int groupPosition, int childPosition) {
            return true;
        }
	    
	}
	
	
	class CollectActivitiesTask extends AsyncTask<Void, Group, Void> {

		@Override
		protected void onPreExecute() {
			setProgressBarIndeterminateVisibility(true);
		}

		@Override
		protected void onPostExecute(Void param) {
			setProgressBarIndeterminateVisibility(false);
		}

		protected void onProgressUpdate(Group...group) {
		    ExpandableListAdapter adapter = mAdapter;
			adapter.mGroups.add(group[0]);
			adapter.notifyDataSetChanged();
		}

		@Override
		protected Void doInBackground(Void... params) {

		    Group group;		    
		    
		    // get check all recommended activities
		    { 
    		    final Intent intent = new Intent();
    		    group = new Group(R.string.txt_recommended);
                
    		    FeaturedActivity activity;
    		    ArrayList<FeaturedActivity> activities = mFeaturedActivities;
                int size = activities.size();
                for (int i = 0; i < size; i++) {
                    activity = activities.get(i); 
                    intent.setClassName(activity.packageName, activity.className);
                    List<ResolveInfo> infos = mPackageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
                    for (ResolveInfo info : infos) {
                        group.children.add(info);
                    }
                }
    		    
                if (group.children.size() > 0) {
                    publishProgress(group);
                }
		    }
		    
		    // get other activities
		    {
                group = new Group(R.string.txt_other);
                
                Intent queryIntent = new Intent(Intent.ACTION_MAIN);
                queryIntent.addCategory(Intent.CATEGORY_LAUNCHER);
                List<ResolveInfo> list = mPackageManager.queryIntentActivities(queryIntent, 0);
    
                // Sort the list
                Collections.sort(list, new ResolveInfo.DisplayNameComparator(mPackageManager));
    
                String className;
                for(ResolveInfo item : list) {
                    
                    // remove featured activities from the list
                    className = item.activityInfo.name;
                    int index = Arrays.binarySearch(mFeaturedClassNames, className);
                    if (index < 0) { // not in the list of featured activities
                        group.children.add(item);
                    }
                    
                }
                
                if (group.children.size() > 0) {
                    publishProgress(group);
                }
		    }
		    
			return null;
		}

	}

	PackageManager mPackageManager;
	ExpandableListAdapter mAdapter;
    ArrayList<FeaturedActivity> mFeaturedActivities = new ArrayList<FeaturedActivity>();
    String[] mFeaturedClassNames; // search dictionary

	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		setProgressBarVisibility(true);

		setListAdapter(mAdapter = new ExpandableListAdapter(getLayoutInflater()));
		mPackageManager = getPackageManager();

		new CollectActivitiesTask().execute();
		getExpandableListView().setItemsCanFocus(true);
	}

	public void onClick(View view) {

		if (view.getId() == R.id.item) {

			ActivityInfo activityInfo = (ActivityInfo) view.getTag();

			// store configuration
			SharedPreferences prefs = getApplication().getSharedPreferences(PREFS, MODE_WORLD_WRITEABLE);
			prefs.edit().putString(PREF_CLASS_NAME, activityInfo.name).putString(PREF_PACKAGE_NAME,
					activityInfo.packageName).putString(PREF_ACTIVITY_NAME,
					activityInfo.loadLabel(mPackageManager).toString()).commit();

			// exit activity
			finish();
		} else {

			ActivityInfo activityInfo = (ActivityInfo) view.getTag();
			String packageName = activityInfo.packageName;
			String className = activityInfo.name;

			Intent intent = new Intent();
			intent.setClassName(packageName, className);
			startActivity(intent);
		}
	}
}
