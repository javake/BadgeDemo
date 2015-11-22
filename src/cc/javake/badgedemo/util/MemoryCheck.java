package cc.javake.badgedemo.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.StringTokenizer;

import android.app.ActivityManager;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.PixelFormat;
import android.os.Debug;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.view.Gravity;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.TextView;

/**
 * ********************
 *   ��ǰ�����ڴ�����
 *   	
 *     <uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />
 **********************
 */
public class MemoryCheck extends Service {

	public static boolean isNeedStart = false;
	private static final int UPDATE_TEXTVIEW = 1;
	private static ServiceConnection mMemoryCheckConnection;

	private ActivityManager mActivityManager;
	private WindowManager wManager;
	WindowManager.LayoutParams wmParams;
	private TextView textView;
	private String mShowSize;
	private TextView cpuRateTextView;
    private String mCpuRate;
	private boolean isDoThread;
	private Debug.MemoryInfo[] memoryInfo;
	int[] myMempid;
	private int memSize;

	private Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case UPDATE_TEXTVIEW:
				if (null == wManager) {
					return;
				}
				if (textView != null && mShowSize != null) {
					textView.setText(mShowSize);
				}
				if (cpuRateTextView != null && mCpuRate != null) {
					cpuRateTextView.setText(mCpuRate);
				}
				break;
			default:
				break;
			}
		}
	};

	/**
	 * ����ʾ�ڴ渡������
	 * 
	 * @param context �󶨷��������context
	 * @return �Ƿ�󶨳ɹ�
	 */
	public static boolean bindMemoryCheckService(Context context) {
		if (!isNeedStart) {
			return false;
		}
		if (mMemoryCheckConnection == null) {
			mMemoryCheckConnection = new ServiceConnection() {
				public void onServiceConnected(ComponentName name,
						IBinder service) {
				}

				public void onServiceDisconnected(ComponentName name) {

				}
			};
		}
		Intent i = new Intent(context, MemoryCheck.class);
		context.bindService(i, mMemoryCheckConnection, BIND_AUTO_CREATE);
		return isNeedStart;
	}

	/**
	 * �������ʾ�ڴ渡������
	 * 
	 * @param context
	 *            ����󶨷��������context
	 * @return �Ƿ�󶨳ɹ�
	 */
	public static boolean unbindMemoryCheckService(Context context) {
		if (isNeedStart) {
			context.unbindService(mMemoryCheckConnection);
		}
		mMemoryCheckConnection = null;
		return isNeedStart;
	}

	@Override
	public IBinder onBind(Intent intent) {
		isDoThread = true;
		mActivityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
		wManager = (WindowManager) getApplicationContext().getSystemService(
				Context.WINDOW_SERVICE);
		myMempid = new int[] { android.os.Process.myPid() };
		wmParams = new WindowManager.LayoutParams();

		wmParams.type = LayoutParams.TYPE_PHONE; // ����window type
		wmParams.format = PixelFormat.RGBA_8888; // ����ͼƬ��ʽ��Ч��Ϊ����͸��
		/*
		 * �����flags���Ե�Ч����ͬ���������� ���������ɴ������������κ��¼�,ͬʱ��Ӱ�������¼���Ӧ��
		 */
		wmParams.flags = LayoutParams.FLAG_NOT_TOUCH_MODAL
				| LayoutParams.FLAG_NOT_FOCUSABLE
				| LayoutParams.FLAG_NOT_TOUCHABLE;

		// ���������������Ҳ��м�
		wmParams.gravity = Gravity.RIGHT | Gravity.TOP;
		// ����Ļ���Ͻ�Ϊԭ�㣬����x��y��ʼֵ
		wmParams.x = 0;
		wmParams.y = 0;

		// �����������ڳ�������
		wmParams.width = LayoutParams.WRAP_CONTENT;
		wmParams.height = LayoutParams.WRAP_CONTENT;
		textView = new TextView(this);
		cpuRateTextView = new TextView(this);

		// ���viewû�б����뵽ĳ��������У������WindowManager��
		if (textView.getParent() == null) {
			wManager.addView(textView, wmParams);
		}
		if (cpuRateTextView.getParent() == null) {
			float density = getApplication().getResources().getDisplayMetrics().density;
		    wmParams.y = (int) (16 * density + 0.5f);
            wManager.addView(cpuRateTextView, wmParams);
        }

		new Thread(new Runnable() {
			@Override
			public void run() {
				while (isDoThread) {
					try {
						memoryInfo = mActivityManager
								.getProcessMemoryInfo(myMempid);
						memSize = memoryInfo[0].getTotalPrivateDirty();
						mShowSize = memSize + "KB";
						mCpuRate = getCpuRateForLinux(myMempid[0]);
						mHandler.sendEmptyMessage(UPDATE_TEXTVIEW);
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		}).start();
		return null;
	}

	@Override
	public boolean onUnbind(Intent intent) {
		stopThread();
		return super.onUnbind(intent);
	}

	@Override
	public void onDestroy() {
		stopThread();
		super.onDestroy();
	}

	private void stopThread() {
		isDoThread = false;
		if (wManager == null) {
			return;
		}
		if (textView != null) {
			wManager.removeView(textView);
		}
		if (cpuRateTextView != null) {
			wManager.removeView(cpuRateTextView);
		}
		wManager = null;
		textView = null;
		cpuRateTextView = null;
	}
	
    @SuppressWarnings("resource")
	public static String getCpuRateForLinux(int pid) {
		InputStream is = null;
		InputStreamReader isr = null;
		BufferedReader brStat = null;
		StringTokenizer tokenStat = null;
		try {
			Process process = Runtime.getRuntime().exec("top -n 1");
			is = process.getInputStream();
			isr = new InputStreamReader(is);
			brStat = new BufferedReader(isr);
			String line;
			String cpuRate;
			while ((line = brStat.readLine()) != null) {
				tokenStat = new StringTokenizer(line);
				if (!tokenStat.hasMoreTokens()) {
					continue;
				}
				if (tokenStat.nextToken().equals(pid + "")) {
					while (tokenStat.hasMoreTokens()) {
						cpuRate = tokenStat.nextToken();
						if (cpuRate.contains("%")) {
							return cpuRate;
						}
					}
				}
			}
			return "";
		} catch (IOException ioe) {
			System.out.println(ioe.getMessage());
			freeResource(is, isr, brStat);
			return "";
		} finally {
			freeResource(is, isr, brStat);
		}
	}

    private static void freeResource(InputStream is, InputStreamReader isr,
			BufferedReader br) {
		try {
			if (is != null)
				is.close();
			if (isr != null)
				isr.close();
			if (br != null)
				br.close();
		} catch (IOException ioe) {
			System.out.println(ioe.getMessage());
		}
	}
    
}