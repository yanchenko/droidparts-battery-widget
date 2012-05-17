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

package org.droidparts.battery_widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;


public class BatteryWidget {

	public static final String TAG = "droidparts-battery-widget";

	public static final String PREFS = "common";
	public static final String PREF_PACKAGE_NAME = "package";
	public static final String PREF_CLASS_NAME = "class";
	public static final String PREF_ACTIVITY_NAME = "name";
	public static final String PREF_DESIGN_TYPE = "design-type";

	public static final int DESIGN_COOL = 0;
	public static final int DESIGN_AWFUL = 1;
	public static final int DESIGN_AWFULLY_COOL = 2;
	public static final int DESIGN_DESIGN_COLORFULL = 3;

    private static final String MIME = "org.droidparts.battery_widget/widget";
	
	public static void updateWidgets(Context context, int chargeLevel, boolean chargerConnected) {

		SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_WORLD_READABLE);
		int design = prefs.getInt(PREF_DESIGN_TYPE, DESIGN_AWFULLY_COOL);
		
		String level = chargeLevel < 10 ? "0" + chargeLevel : String.valueOf(chargeLevel);
		
		// create views
		RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.battery_widget);
		
		// update level
		PendingIntent pendingIntent = getPendingIntent(context);
		views.setOnClickPendingIntent(R.id.battery, pendingIntent);
		views.setInt(R.id.battery, "setImageLevel", getIconLevel(chargeLevel, design));
		
		// update charge level
		if (isCapacityRightBottom(design)) { // right-bottom
			// hide center capacity
			views.setViewVisibility(R.id.capacity_center, View.GONE);
			// update visible capacity
			views.setTextViewText(R.id.capacity_right_bottom, level);
			views.setViewVisibility(R.id.capacity_right_bottom, chargeLevel < 100 ? View.VISIBLE : View.GONE);
		} else { // center
			// hide center capacity
			views.setViewVisibility(R.id.capacity_right_bottom, View.GONE);
			// update visible capacity
			views.setTextViewText(R.id.capacity_center, level);
			views.setViewVisibility(R.id.capacity_center, chargeLevel < 100 ? View.VISIBLE : View.GONE);
		}
		
		// update lightning visibility
		views.setViewVisibility(R.id.lightning, chargerConnected ? View.VISIBLE : View.GONE);
		
		// update widgets
		AppWidgetManager widgetManager = AppWidgetManager.getInstance(context);
		ComponentName componentName = new ComponentName(context, BatteryWidgetProvider.class);
		widgetManager.updateAppWidget(componentName, views);
		
		Log.d(TAG, "widgets updated");
	}

	private static PendingIntent getPendingIntent(Context context) {

		SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_WORLD_READABLE);
		String name = prefs.getString(PREF_ACTIVITY_NAME, null);
		
		if (name == null) {
			Intent intent = new Intent(Intent.ACTION_VIEW);
			intent.setType(MIME);
			return PendingIntent.getActivity(context, 0, intent, 0);
		} else {
			String className = prefs.getString(PREF_CLASS_NAME, null);
			String packageName = prefs.getString(PREF_PACKAGE_NAME, null);
			Intent intent = new Intent();
			intent.setClassName(packageName, className);
			return PendingIntent.getActivity(context, 0, intent, 0);
			
		}
	}
	
	public static int getIconLevel(int chargeLevel, int design) {
		switch (design) {
			case DESIGN_COOL: return chargeLevel;
			case DESIGN_AWFUL:
			case DESIGN_AWFULLY_COOL: return 200 + chargeLevel;
			case DESIGN_DESIGN_COLORFULL: return 400 + chargeLevel;
			default: return chargeLevel;
		}
	}

	public static boolean isCapacityRightBottom(int design) {
		return design != DESIGN_AWFUL;
	}
}
