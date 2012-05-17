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

import static com.bwx.qs.battery.BatteryWidget.TAG;
import static com.bwx.qs.battery.BatteryWidgetProvider.EXT_UPDATE_WIDGETS;

import java.lang.reflect.Field;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

public class BatteryService extends Service {

	// cached values
	int mBatteryChargeLevel = -1; 
	boolean mChargerConnected;

	private ScreenStateService mScreenStateReceiver;


	private class BatteryStateReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();

			if (Intent.ACTION_BATTERY_CHANGED.equals(action)) {
				
				// see constants in BatteryManager
				
				int rawlevel = intent.getIntExtra("level", -1);
				int scale = intent.getIntExtra("scale", -1);
				int level = 0;
				if (rawlevel >= 0 && scale > 0) {
					level = (rawlevel * 100) / scale;
				}
				mBatteryChargeLevel = level;
				mChargerConnected = intent.getIntExtra("plugged", 0) > 0 && level < 100 /* not charging if 100%*/;
				
				Log.d(TAG, "battery state: level=" + level + ", charging=" + mChargerConnected);
			}
			
			BatteryWidget.updateWidgets(context, mBatteryChargeLevel, mChargerConnected);
		}
	}

	private class ScreenStateService extends BroadcastReceiver {
		private BatteryStateReceiver mBatteryStateReceiver;

		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (Intent.ACTION_SCREEN_ON.equals(action)) {
				Log.d(TAG, "screen is ON");
				registerBatteryReceiver(true, context);
			} else if (Intent.ACTION_SCREEN_OFF.equals(action)) {
				registerBatteryReceiver(false, context);
				Log.d(TAG, "screen is OFF");
			}
		}

		public void registerBatteryReceiver(boolean register, Context context) {
			if (register) {
				if (mBatteryStateReceiver == null) {
					mBatteryStateReceiver = new BatteryStateReceiver();
			        IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
					context.registerReceiver(mBatteryStateReceiver, filter);
				}
			} else if (mBatteryStateReceiver != null) {
				context.unregisterReceiver(mBatteryStateReceiver);
				mBatteryStateReceiver = null;
			}

			Log.d(TAG, "battery receiver " + (register ? "ON" : "OFF (sleeping)"));
		}

		public void registerScreenReceiver(boolean register, Context context) {
			if (register) {
				IntentFilter filter = new IntentFilter();
				filter.addAction(Intent.ACTION_SCREEN_ON);
				filter.addAction(Intent.ACTION_SCREEN_OFF);
				context.registerReceiver(this, filter);
			} else {
				registerBatteryReceiver(false, context);
				context.unregisterReceiver(this);
			}
		}
	}

	public void onStart(Intent intent, int startId) {

		if (mScreenStateReceiver == null) {
			mScreenStateReceiver = new ScreenStateService();

			if (isScreenOn(this)) {
				mScreenStateReceiver.registerBatteryReceiver(true, this);
			}

			mScreenStateReceiver.registerScreenReceiver(true, this);
			Log.d(TAG, "started");
		}

		Bundle ext = intent.getExtras();
		if (ext != null && ext.getBoolean(EXT_UPDATE_WIDGETS, false)) {
			BatteryWidget.updateWidgets(this, mBatteryChargeLevel, mChargerConnected);
		}

	}

	public void onDestroy() {

		if (mScreenStateReceiver != null) {
			mScreenStateReceiver.registerScreenReceiver(false, this);
			mScreenStateReceiver = null;
		}

		Log.d(TAG, "stopped");
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	public static void requestWidgetUpdate(Context context) {
		Intent serviceIntent = new Intent(context, BatteryService.class);
		serviceIntent.putExtra(EXT_UPDATE_WIDGETS, true);
		context.startService(serviceIntent);
	}

	private static boolean isScreenOn(Context context) {
		PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
		int sdkVersion = Integer.parseInt(Build.VERSION.SDK);
		try {
			if (sdkVersion >= 7) {
				// >= 2.1
				Boolean bool = (Boolean) PowerManager.class.getMethod("isScreenOn").invoke(pm);
				return bool.booleanValue();
			} else {
				// < 2.1
				Field field = PowerManager.class.getDeclaredField("mService");
				field.setAccessible(true);
				Object/* IPowerManager */service = field.get(pm);
				Long timeOn = (Long) service.getClass().getMethod("getScreenOnTime").invoke(service);
				return timeOn > 0;
			}
		} catch (Exception e) {
			Log.e(TAG, "cannot check whether screen is on", e);
			return true;
		}
	}
}
