package cc.javake.badgedemo;

import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

public class MainActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
	}

	@Override
	protected void onPause() {
		super.onPause();
		EditText countView = (EditText)findViewById(R.id.badge_count);
		int count = 0;
		try {
			count = Integer.valueOf(countView.getText().toString());
		} catch (Exception e) {
		}
		BadgeUtil.setBadgeCount(this.getApplicationContext(), count);
	}

	public void getFacName(View v) {
    	String facName = TextUtils.isEmpty(Build.MANUFACTURER) ? 
    			"" : Build.MANUFACTURER.trim().toLowerCase();
    	((TextView)findViewById(R.id.show_msg)).setText(facName);
	}
	
}
