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

import static org.droidparts.battery_widget.BatteryWidget.TAG;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

public class BatteryWidgetProvider extends AppWidgetProvider {
    
	public static final String EXT_UPDATE_WIDGETS = "updateWidgets";
	private static final String BATTERY_SERVICE_ACTION = "org.droidparts.battery_widget.BatteryService";
	
	public void onEnabled(Context context) {
		Log.d(TAG, "provider.enabled");
		
		Intent intent = new Intent(BATTERY_SERVICE_ACTION);
		context.startService(intent);
	}

	public void onDisabled(Context context) {
		Log.d(TAG, "provider.disabled");
		
		// stop service
		Intent intent = new Intent(BATTERY_SERVICE_ACTION);
		context.stopService(intent);
		
		// remove configuration
		SharedPreferences prefs = context.getSharedPreferences(BatteryWidget.PREFS, Context.MODE_WORLD_READABLE);
		prefs.edit().remove(BatteryWidget.PREF_ACTIVITY_NAME).commit();
	}

	public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
		Log.d(TAG, "provider.update");
		BatteryService.requestWidgetUpdate(context);
	}
}
