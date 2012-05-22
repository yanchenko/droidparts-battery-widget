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

import static org.droidparts.battery_widget.BatteryWidget.DESIGN_AWFULLY_COOL;
import static org.droidparts.battery_widget.BatteryWidget.PREFS;
import static org.droidparts.battery_widget.BatteryWidget.PREF_ACTIVITY_NAME;
import static org.droidparts.battery_widget.BatteryWidget.PREF_DESIGN_TYPE;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.appwidget.AppWidgetManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.TextView;

public class BatteryWidgetActivity extends Activity implements OnClickListener,
		android.content.DialogInterface.OnClickListener, Runnable {

	private static final int[] LEVELS = new int[] {1, 10, 20, 30, 40, 60, 80, 99, 100};
	private static final int[] MAPPING = new int[] {0, 3, 2, 1};

	private Handler mHandler = new Handler();
	private SharedPreferences mPrefs;
	private int mLevel;
	private int mDesign;
	
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.configuration);

		findViewById(R.id.link0).setOnClickListener(this);
		findViewById(R.id.link1).setOnClickListener(this);
		findViewById(R.id.button1).setOnClickListener(this);

		mPrefs = getApplication().getSharedPreferences(PREFS, MODE_PRIVATE);
	}

	protected void onPause() {
		super.onPause();
		schedulePreviewUpdate(false);
	}
	
	protected void onResume() {
		super.onResume();
		
		// update widgets
		BatteryService.requestWidgetUpdate(this);

		Intent intent = getIntent();
		Bundle extras = intent.getExtras();
		int mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
		if (extras != null) {
			mAppWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
		}

		if (mAppWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
			// set result OK
			Intent resultValue = new Intent();
			resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
			setResult(RESULT_OK, resultValue);
		}

		// update description
		String name = mPrefs.getString(PREF_ACTIVITY_NAME, null);
		TextView textView = (TextView) findViewById(R.id.assigned_activity_descr);
		textView.setText(name == null ? getString(R.string.txt_assigned_activity_descr) : name);

		mDesign = mPrefs.getInt(PREF_DESIGN_TYPE, DESIGN_AWFULLY_COOL);
		updateWidgetPreview(100);
		
		mHandler.postDelayed(this, 1000);
	}
	
 	private void updateWidgetPreview(int chargeLevel) {
 		int design = mDesign;
		
 		// lightning
 		ImageView img = (ImageView) findViewById(R.id.lightning);
 		img.setVisibility(chargeLevel < 100 ? View.VISIBLE : View.GONE);
 		
 		// design
		img = (ImageView) findViewById(R.id.battery);
		img.setImageLevel(BatteryWidget.getIconLevel(chargeLevel, design));

		TextView capacity_center = (TextView) findViewById(R.id.capacity_center);
		TextView capacity_right_bottom = (TextView) findViewById(R.id.capacity_right_bottom);

		String levelText = chargeLevel < 10 ? "0" + chargeLevel : String.valueOf(chargeLevel);

		if (BatteryWidget.isCapacityRightBottom(design)) { // right-bottom
			capacity_center.setVisibility(View.GONE);
			capacity_right_bottom.setText(levelText);
			capacity_right_bottom.setVisibility(chargeLevel < 100 ? View.VISIBLE : View.GONE);
		} else { // center
			capacity_right_bottom.setVisibility(View.GONE);
			capacity_center.setText(levelText);
			capacity_center.setVisibility(chargeLevel < 100 ? View.VISIBLE : View.GONE);
		}

		// text
		Resources res = getResources();
		String[] designNames = res.getStringArray(R.array.design_names);
		TextView text = (TextView) findViewById(R.id.design_descr);
		text.setText(designNames[MAPPING[design]]);
	}

	public void onClick(View view) {
		int id = view.getId();
		if (id == R.id.link0) { // choose design
			showDialog(0);
		} else if (id == R.id.link1) {
			Intent intent = new Intent(this, SettingsActivityList.class);
			startActivity(intent);
		} else if (id == R.id.button1) { // done
			finish();
		}
	}

	protected Dialog onCreateDialog(int id) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setItems(R.array.design_names, this);
		builder.setTitle(R.string.txt_select_widget_design);
		return builder.create();
	}

	public void onClick(DialogInterface dialog, int which) {
		dismissDialog(0);
		if (which > -1) {
			mDesign = MAPPING[which];
			mPrefs.edit().putInt(PREF_DESIGN_TYPE, mDesign).commit();
			schedulePreviewUpdate(true);
			BatteryService.requestWidgetUpdate(this);
		}
	}

	public void run() {
		if (++mLevel == LEVELS.length) {
			mLevel = 0;
		}
		updateWidgetPreview(LEVELS[mLevel]);
		mHandler.postDelayed(this, 700);
	}

	private void schedulePreviewUpdate(boolean active) {
		mHandler.removeCallbacks(this);
		if (active) {
			mHandler.post(this);
		}
	}
}
