package per.pqy.apktool;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.app.Service;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.text.ClipboardManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

@SuppressWarnings("deprecation")
@SuppressLint("HandlerLeak")
public class MainActivity extends Activity {
	static int count = 0;
	MyHandler myHandler = new MyHandler();
	ProgressDialog myDialog;
	private TextView tvpath;
	private ListView lvFiles;
	PowerManager powerManager = null;
	WakeLock wakeLock = null;
	String apicode = String.valueOf(android.os.Build.VERSION.SDK_INT);
	String shell = new String();
	private static final int DECODE = 1;
	private static final int COMPILE = 2;
	private static final int DEODEX = 3;
	private static final int DECDEX = 4;
	private static final int LONGPRESS = 5;
	private static final int UNPACKIMG = 6;
	private static final int REPACKIMG = 7;
//	private static final int TASK = 8;
//	private static final int JAVA = 9;
//	private static final int CLASS = 10;
	
	

	enum fileType {
		FOLDER, NFILE, APKFILE, ODEXFILE, IMGFILE, ZIPFILS, TXTFILE,
		JARFILE, TARFILE, DEXFILS, JAVAFILE, CLASSFILE, SHELLFILE
	};

	private static final int decode_all =0,decode_dex=1,
			decode_res=2,sign_apk=3,make_odex=4,zipalign=5,
			install=6,delete_dex=7,extract_sign=8,delete_sign=9,
			add_sign=10,import_fw=11,dex2jar=12,jar2dex=13,decode_cancel=14;
	
//	boolean tasks[] = new boolean[] { false, false, false, false };
//	ProgressDialog dialogs[] = new ProgressDialog[4];

	public String uri;
	File currentParent;
	File[] currentFiles;

	class MyHandler extends Handler {
		public void doWork(String str, final Bundle bundle) {
			
			if (bundle.getBoolean("isTemp")) {  
				myDialog.setMessage(bundle.getString("op"));
			} else {
				SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
				if (settings.getBoolean("vibration", false) != false) {
					Vibrator v = (Vibrator) getApplication().getSystemService(
							Service.VIBRATOR_SERVICE);
					v.vibrate(new long[] { 0, 200, 100, 200 }, -1);
				}
				if (settings.getBoolean("notify", false) != false) {
					NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
					Notification notification = new Notification(
							R.drawable.ic_launcher,
							getString(R.string.op_done),
							System.currentTimeMillis());
					Context context = getApplicationContext();
					CharSequence contentTitle = bundle.getString("filename");
					CharSequence contentText = getString(R.string.op_done);
					Intent notificationIntent = MainActivity.this.getIntent();
					PendingIntent contentIntent = PendingIntent.getActivity(
							MainActivity.this, 0, notificationIntent, 0);
					notification.setLatestEventInfo(context, contentTitle,
							contentText, contentIntent);
					notification.flags |= Notification.FLAG_AUTO_CANCEL;
					mNotificationManager.notify(count++, notification);
				}
				myDialog.dismiss();
//				int num = bundle.getInt("tasknum");
//				tasks[num] = false;
//				dialogs[num].dismiss();
				Toast.makeText(MainActivity.this, str, Toast.LENGTH_LONG)
						.show();

				AlertDialog.Builder msgDialog = new AlertDialog.Builder(
						MainActivity.this);
				String tmp_str = bundle.getString("filename") + "\n"
						+ getString(R.string.cost_time);

				long time = (System.currentTimeMillis() - bundle.getLong("time")) / 1000;
				if (time > 3600) {
					tmp_str += Integer.toString((int) (time / 3600))
							+ getString(R.string.hour)
							+ Integer.toString((int) (time % 3600) / 60)
							+ getString(R.string.minute)
							+ Integer.toString((int) (time % 60))
							+ getString(R.string.second);
				} else if (time > 60) {
					tmp_str += Integer.toString((int) (time % 3600) / 60)
							+ getString(R.string.minute)
							+ Integer.toString((int) (time % 60))
							+ getString(R.string.second);
				} else {
					tmp_str += Integer.toString((int) time)
							+ getString(R.string.second);
				}
				if (settings.getBoolean("wrap_msg", true) == false) {
					HorizontalScrollView hscv = new HorizontalScrollView(
							MainActivity.this);
					ScrollView scv = new ScrollView(MainActivity.this);
					TextView tv;

					tv = new TextView(MainActivity.this);

					tv.setText(bundle.getString("output"));
					scv.addView(tv);
					hscv.addView(scv);
					msgDialog.setView(hscv);
				} else
					msgDialog.setMessage(bundle.getString("output"));
				msgDialog.setTitle(tmp_str)
						.setPositiveButton(getString(R.string.ok), null)
						.setNeutralButton((getString(R.string.copy)),
								new DialogInterface.OnClickListener() {
									@Override
									public void onClick(DialogInterface dialog,
											int which) {
										// TODO Auto-generated method stub
										ClipboardManager cmb = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
										cmb.setText(bundle.getString("output"));
									}
								}).create().show();
				currentFiles = currentParent.listFiles();
				inflateListView(currentFiles);
			}
		}

		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			final Bundle bundle = msg.getData();
			switch (bundle.getInt("what")) {
			case 0:
				doWork(getString(R.string.decompile_all_finish), bundle);
				break;
			case 1:
				doWork(getString(R.string.sign_finish), bundle);
				break;
			case 2:
				doWork(getString(R.string.recompile_finish), bundle);
				break;
			case 3:
				doWork(getString(R.string.decompile_dex_finish), bundle);
				break;
			case 4:
				doWork(getString(R.string.decompile_res_finish), bundle);
				break;
			case 5:
				doWork(getString(R.string.decompile_odex_finish), bundle);
				break;
			case 6:
				doWork(getString(R.string.op_done), bundle);
				break;
			case 7:
				doWork(getString(R.string.import_finish), bundle);
				break;
			case 8:
				doWork(getString(R.string.align_finish), bundle);
				break;
			case 9:
				doWork(getString(R.string.add_finish), bundle);
				break;
			case 10:
				doWork(getString(R.string.delete_finish), bundle);
				break;
			
			}
		}
	}

	public void threadWork(Context context, String message,
			final String command, final int what) {
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
		if (settings.getBoolean("root", false) != false) 
			shell = "su ";
		else {
			shell = "sh ";
		}
		/*
		int freeTask = -1;
		if (!tasks[0])
			freeTask = 0;
		else if (!tasks[1])
			freeTask = 1;
		else if (!tasks[2])
			freeTask = 2;
		else if (!tasks[3])
			freeTask = 3;
		if (freeTask == -1) {
			Toast.makeText(MainActivity.this, getString(R.string.nofreetask),
					Toast.LENGTH_SHORT).show();
			return;
		}
		*/
		Thread thread = new Thread() {
			public void run() {
				java.lang.Process process = null;
				DataOutputStream os = null;
				InputStream proerr = null;
				InputStream proin = null;
				try {
					Bundle tb = new Bundle();
					tb.putString("filename", new File(uri).getName());
					tb.putInt("what", what);
					tb.putLong("time", System.currentTimeMillis());
					tb.putBoolean("isTemp", false);
					process = Runtime.getRuntime().exec(shell);
					os = new DataOutputStream(process.getOutputStream());
					proerr = process.getErrorStream();
					proin = process.getInputStream();
					os.writeBytes(new String(
							"LD_LIBRARY_PATH=/data/data/per.pqy.apktool/apktool/openjdk/lib/i386:/data/data/per.pqy.apktool/apktool/openjdk/lib/i386/jli:/data/data/per.pqy.apktool/apktool/openjdk/lib/arm:/data/data/per.pqy.apktool/apktool/openjdk/lib/arm/jli:$LD_LIBRARY_PATH ")     	//Note:This line is very important!
							+command + "\n");
					os.writeBytes("exit\n");
					os.flush();
					BufferedReader br1 = new BufferedReader(
							new InputStreamReader(proerr));
					String str = "";
					String totalstr = "";
					while ((str = br1.readLine()) != null) {
						Message msg = new Message();
						Bundle bundle = new Bundle();
						totalstr += str + "\n";
						bundle.putString("op", str);
						bundle.putInt("what", what);
						bundle.putBoolean("isTemp", true);
//						bundle.putInt("tasknum", tasknum);
						msg.setData(bundle);
						myHandler.sendMessage(msg);
					}
					process.waitFor();
					Message tmsg = new Message();
					tb.putString("output",
							totalstr + RunExec.inputStream2String(proin, "utf-8"));
					tmsg.setData(tb);
					myHandler.sendMessage(tmsg);
				} catch (Exception e) {
					Log.d("*** DEBUG ***", "ROOT REE" + e.getMessage());
				} finally {
					try {
						if (os != null) {
							os.close();
						}
						process.destroy();
					} catch (Exception e) {
					}
				}
			}
		};

		thread.start();
		myDialog = new ProgressDialog(context);
		myDialog.setMessage(message);
		myDialog.setIndeterminate(true);
		myDialog.setCancelable(false);
		
		myDialog.setButton(DialogInterface.BUTTON_POSITIVE,
				getString(R.string.put_background),
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						myDialog.dismiss();
					}
				});
		/*
		  myDialog.setButton(DialogInterface.BUTTON_NEGATIVE,
		  getString(R.string.cancel), new DialogInterface.OnClickListener() {
		  
		  @Override public void onClick(DialogInterface dialog, int which) {
			  RunExec.Cmd(shell, "sh /data/data/per.pqy.apktool/apktool/killjob.sh");
		  
		  
		  } });
		 */
//		dialogs[freeTask] = myDialog;
//		tasks[freeTask] = true;
		myDialog.show();
	}

	protected Dialog onCreateDialog(int id) {

		switch (id) {
		case DECODE:
			return new AlertDialog.Builder(MainActivity.this).setItems(
					R.array.dec_array, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							String apktoolVersion = PreferenceManager.getDefaultSharedPreferences(MainActivity.this)
																		   .getString("apktool_version", "2.0");
							String diff = "";
							if(apktoolVersion.equals("2.0")){
								apktoolVersion = "sh /data/data/per.pqy.apktool/apktool/apktool2.sh ";
								diff = " -o ";
							}else
								apktoolVersion = "sh /data/data/per.pqy.apktool/apktool/apktool.sh ";
							switch (which) {
							case decode_all:
								final String command = apktoolVersion+
										" d -f "
										+ "'"
										+ uri
										+ "' "
										+ diff
										+" '"
										+ uri.substring(0, uri.length() - 4)										
										+ "_src'";
								threadWork(MainActivity.this,
										getString(R.string.decompiling),
										command, 0);
								break;
								
								case decode_dex:
									final	String command2 = apktoolVersion+ "d -f -r " 
										+ "'" 
										+ uri 
										+ "' "
										+diff
										+" '" 
										+ uri.substring(0, uri.length() - 4) 
										+ "_src'";
									threadWork(MainActivity.this, 
									    getString(R.string.decompiling), 
										command2, 3);
									break;	
								case decode_res:
									final String command3 = apktoolVersion+" d -f -s " 
										+ "'" + uri + "' "+ diff+" '" + uri.substring(0, uri.length() - 4) +"_src'";
									threadWork(MainActivity.this, getString(R.string.decompiling), command3, 4);								
									break;							
								case sign_apk:		
									final String command4 = new String("sh /data/data/per.pqy.apktool/apktool/signapk.sh ") 
										+ "'" + uri + "' '" + uri.substring(0, uri.length() - 4) + "_sign.apk'";
									threadWork(MainActivity.this, getString(R.string.signing), command4, 1);					
									break;
								case make_odex:								
									final String command5 = new String(" /data/data/per.pqy.apktool/apktool/openjdk/bin/dexopt-wrapper ") 
										+ "'" + uri + "' '" + uri.substring(0, uri.length() - 3) + "odex'";
									threadWork(MainActivity.this, getString(R.string.making), command5, 6);	
									break;

								case zipalign:
									final String command7 = new String("/data/data/per.pqy.apktool/apktool/openjdk/bin/zipalign -f 4 ") + "'" + uri + "' '" + uri.substring(0, uri.length() - 4) + "_zipalign.apk'";
									threadWork(MainActivity.this, getString(R.string.aligning), command7, 8);
									break;
								case install:
									Intent intent = new Intent(Intent.ACTION_VIEW);  
									final Uri apkuri = Uri.fromFile(new File(uri));  
									intent.setDataAndType(apkuri, "application/vnd.android.package-archive");  
									startActivity(intent);
									break;
									
									
								case delete_dex:
									
									final String command9 = new String("sh /data/data/per.pqy.apktool/apktool/7z.sh '")+new File(uri).getParent()+"' d -tzip '" + uri + "' classes.dex";
									threadWork(MainActivity.this, getString(R.string.deleting), command9, 10);
									break;
								case extract_sign:
									
									if (!new File(new File(uri).getParent() + "/META-INF").exists())
									{
										final String command10 = new String("sh /data/data/per.pqy.apktool/apktool/7z.sh '")  + new File(uri).getParent() + "' x -tzip '" + uri+ "' META-INF";
										threadWork(MainActivity.this, getString(R.string.unpacking), command10, 6);
									}
									else
										Toast.makeText(MainActivity.this, getString(R.string.dir_exist), Toast.LENGTH_LONG).show();
									break;
								case delete_sign:
									
									final String command11 = new String("sh /data/data/per.pqy.apktool/apktool/7z.sh '")+new File(uri).getParent()+"' d -tzip " + "'" + uri + "'" + " META-INF";
									threadWork(MainActivity.this, getString(R.string.deleting), command11, 10);
									break;
								case add_sign:
									String str = new File(uri).getParent();
									if (new File(str + "/META-INF").exists())
									{
										str = new File(uri).getParent();
										final String command12 = new String("sh /data/data/per.pqy.apktool/apktool/7z.sh '")+str+"' a -tzip '" + uri + "' '" + str + "/META-INF'";
										threadWork(MainActivity.this, getString(R.string.adding), command12, 9);}
									else
										Toast.makeText(MainActivity.this, getString(R.string.dir_not_exist), Toast.LENGTH_LONG).show();
									break;
								case import_fw:
									final String command13 = apktoolVersion+" if " + "'" + uri + "'";								
									threadWork(MainActivity.this, getString(R.string.importing_framework), command13, 7);
									break;
								
							case dex2jar:
								if(uri.endsWith(".apk")){
								final String command15 = new String("sh /data/data/per.pqy.apktool/apktool/dex2jar/d2j-dex2jar.sh ")
								+ "'"
								+uri
								+"' -o '"
								+uri.substring(0,uri.length()-3)
								+"jar'";
								threadWork(MainActivity.this,getString(R.string.making),command15,6);
								}
								break;
							case jar2dex:
								if(uri.endsWith(".jar")){
									final String command16 = new String("sh /data/data/per.pqy.apktool/apktool/dex2jar/d2j-jar2dex.sh ")
									+ "'"
									+uri
									+"' -o '"
									+uri.substring(0,uri.length()-3)
									+"dex'";
									threadWork(MainActivity.this,getString(R.string.making),command16,6);
									}
									break;
							case decode_cancel:
								return;
							}
						}
					}).create();
		case COMPILE:
			return new AlertDialog.Builder(MainActivity.this).setItems(
					R.array.comp_array, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							String apktoolVersion = PreferenceManager.getDefaultSharedPreferences(MainActivity.this)
									   .getString("apktool_version", "2.0");
							String aaptVersion = PreferenceManager.getDefaultSharedPreferences(MainActivity.this)
									   .getString("aapt_version", "4.4");
							if(aaptVersion.equals("/system/bin/aapt"))
								aaptVersion = " /system/bin/aapt ";
							else
								aaptVersion = " /data/data/per.pqy.apktool/apktool/openjdk/bin/aapt" + aaptVersion;
								
							String diff = "";
							if(apktoolVersion.equals("2.0")){
								diff = " -o ";							
								apktoolVersion = "sh /data/data/per.pqy.apktool/apktool/apktool2.sh ";
							}else
								apktoolVersion = "sh /data/data/per.pqy.apktool/apktool/apktool.sh ";
							switch (which) {
							case 0:
								if (uri.endsWith("_src")) {
									final String command0 = apktoolVersion+" b -f -a "
								                            + aaptVersion+" '"+ uri + "' "+diff+" '" + uri + ".apk'";
									threadWork(MainActivity.this,
											getString(R.string.recompiling),
											command0, 2);
								} else if (uri.endsWith("_odex")) {
									final String command0 = new String(
											"sh /data/data/per.pqy.apktool/apktool/smali.sh -a ")
											+ apicode
											+ " '"
											+ uri
											+ "' -o '"
											+ uri.substring(0, uri.length() - 5)
											+ ".dex'";
									threadWork(MainActivity.this,
											getString(R.string.recompiling),
											command0, 2);
								} else if (uri.endsWith("_dex")) {
									final String command0 = new String(
											"sh /data/data/per.pqy.apktool/apktool/smali.sh -a ")
											+ apicode
											+ " '"
											+ uri
											+ "' -o '"
											+ uri.substring(0, uri.length() - 4)
											+ ".dex'";
									threadWork(MainActivity.this,
											getString(R.string.recompiling),
											command0, 2);
								}
								break;
								
							case 1:
								File tmp = new File(uri);
								if(tmp.listFiles()==null){
									Toast.makeText(MainActivity.this,getString(R.string.directory_no_permission),Toast.LENGTH_LONG).show();									
								}
								else{
									currentParent = tmp;
									currentFiles = currentParent.listFiles();
									inflateListView(currentFiles);
								}
								break;
							case 2:
								return;
							}
						}
					}).create();
		case DEODEX:
			return new AlertDialog.Builder(MainActivity.this).setItems(
					R.array.deodex_array,
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							switch (which) {
							case 0:
								final String command0 = new String(
										"sh /data/data/per.pqy.apktool/apktool/baksmali.sh -x -a ")
										+ apicode
										+ " '"
										+ uri
										+ "' -o '"
										+ uri.substring(0, uri.length() - 5)
										+ "_odex'";
								threadWork(MainActivity.this,
										getString(R.string.decompiling),
										command0, 5);
								break;
							case 1:
									final String command1 = new String("sh /data/data/per.pqy.apktool/apktool/signodex.sh ") + uri;
									threadWork(MainActivity.this, getString(R.string.signing), command1, 1);
									break;									
							case 2:
								return;
							}
						}
					}).create();
		case DECDEX:
			return new AlertDialog.Builder(MainActivity.this).setItems(
					R.array.decdex_array,
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							switch (which) {
							case 0:
								final String command0 = new String(
										"sh /data/data/per.pqy.apktool/apktool/baksmali.sh '")
										+ uri
										+ "' -o '"
										+ uri.substring(0, uri.length() - 4)
										+ "_dex'";
								threadWork(MainActivity.this,
										getString(R.string.decompiling),
										command0, 3);
								break;
							case 1:
								String apkFile = uri.substring(0,
										uri.length() - 3) + "apk";
								if (new File(apkFile).exists()) {
									apkFile = uri.substring(0, uri.length() - 3)
											+ "apk";
									RunExec.Cmd(shell,
											new String("/data/data/per.pqy.apktool/busybox cp '") + uri + "' '"
													+ new File(uri).getParent()
													+ "/classes.dex'");
									final String command1 = new String(
											"sh /data/data/per.pqy.apktool/apktool/7z.sh '")+new File(uri).getParent()+"' a -tzip '"
													+ apkFile + "' '"
													+ new File(uri).getParent()
													+ "/classes.dex'";
									threadWork(MainActivity.this,
											getString(R.string.adding),
											command1, 9);
								} else
									Toast.makeText(MainActivity.this,
											getString(R.string.apk_not_exist),
											Toast.LENGTH_LONG).show();
								break;
							case 2:
								String jarFile = uri.substring(0,
										uri.length() - 3) + "jar";
								if (new File(jarFile).exists()) {
									jarFile = uri.substring(0, uri.length() - 3)
											+ "jar";
									RunExec.Cmd(shell,
											new String("/data/data/per.pqy.apktool/busybox cp '") + uri + "' '"
													+ new File(uri).getParent()
													+ "/classes.dex'");
									final String command2 = new String(
											"sh /data/data/per.pqy.apktool/apktool/7z.sh '")+new File(uri).getParent()+"' a -tzip '"
													+ jarFile + "' '"
													+ new File(uri).getParent()
													+ "/classes.dex'";
									threadWork(MainActivity.this,
											getString(R.string.adding),
											command2, 9);
								} else
									Toast.makeText(MainActivity.this,
											getString(R.string.jar_not_exist),
											Toast.LENGTH_LONG).show();
							case 3:
								return;
							}
						}
					}).create();
		case LONGPRESS:
			return new AlertDialog.Builder(MainActivity.this).setItems(
					R.array.longpress_array,
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							switch (which) {
							case 0:
								final EditText et = new EditText(
										MainActivity.this);
								et.setText(new File(uri).getName());
								new AlertDialog.Builder(MainActivity.this)
										.setTitle(getString(R.string.new_name))
										.setView(et)
										.setPositiveButton(
												getString(R.string.ok),
												new DialogInterface.OnClickListener() {
													@Override
													public void onClick(
															DialogInterface dialog,
															int which) {
														// TODO Auto-generated
														// method stub
														String newName = et
																.getText()
																.toString();
														newName = currentParent
																+ "/" + newName;
														RunExec.Cmd(
																shell,
																" chmod 777 "
																		+ currentParent);
														new File(uri)
																.renameTo(new File(
																		newName));
														currentFiles = currentParent
																.listFiles();
														inflateListView(currentFiles);
													}
												})
										.setNegativeButton(
												getString(R.string.cancel),
												null).show();
								break;
							case 1:
								new AlertDialog.Builder(MainActivity.this)
										.setTitle(
												getString(R.string.want_to_delete))
										.setPositiveButton(
												getString(R.string.ok),
												new DialogInterface.OnClickListener() {
													@Override
													public void onClick(
															DialogInterface dialog,
															int which) {
														// TODO Auto-generated
														// method stub
														final String command = new String(
																" rm -r '")
																+ uri + "'";
														threadWork(
																MainActivity.this,
																getString(R.string.deleting),
																command, 10);
													}
												})
										.setNegativeButton(
												getString(R.string.cancel),
												null).show();
								break;
							case 2:
								RunExec.Cmd(shell, new String(" chmod 777 '")
										+ uri + "'");
								break;
								
							case 3:
								SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
								SharedPreferences.Editor editor = settings.edit();
								editor.putString("defaultDir", uri);
								editor.commit();
								break;
								
							case 4:
								return;
							}
						}
					}).create();
			/*
		case UNPACKIMG:
			return new AlertDialog.Builder(MainActivity.this).setItems(
					R.array.unpackimg, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							switch (which) {
							case 0:
								if (uri.endsWith("boot.img")) {
									File tmp = new File(uri);
									final String command0 = new String(
											" busybox sh /data/data/per.pqy.apktool/apktool/unpackimg.sh '")
											+ tmp.getParent()
											+ "' boot.img new.img mt657x";
									threadWork(MainActivity.this,
											getString(R.string.unpacking),
											command0, 6);
								} else {
									File tmp = new File(uri);
									final String command0 = new String(
											" busybox sh /data/data/per.pqy.apktool/apktool/unpackimg.sh '")
											+ tmp.getParent()
											+ "' recovery.img new.img mt657x";
									threadWork(MainActivity.this,
											getString(R.string.unpacking),
											command0, 6);
								}
								break;
							case 1:
								if (uri.endsWith("boot.img")) {
									File tmp = new File(uri);
									final String command1 = new String(
											" busybox sh /data/data/per.pqy.apktool/apktool/unpackimg.sh '")
											+ tmp.getParent()
											+ "' boot.img new.img";
									threadWork(MainActivity.this,
											getString(R.string.unpacking),
											command1, 6);
								} else {
									File tmp = new File(uri);
									final String command1 = new String(
											" busybox sh /data/data/per.pqy.apktool/apktool/unpackimg.sh '")
											+ tmp.getParent()
											+ "' recovery.img new.img";
									threadWork(MainActivity.this,
											getString(R.string.unpacking),
											command1, 6);
								}
								break;
							case 2:
								return;
							}
						}
					}).create();

		case REPACKIMG:
			return new AlertDialog.Builder(MainActivity.this).setItems(
					R.array.repackimg, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							switch (which) {
							case 0:

								File tmp = new File(uri);
								final String command0 = new String(
										" busybox  sh /data/data/per.pqy.apktool/apktool/repackimg.sh '")
										+ tmp.getParent() + "' new.img mtk";
								threadWork(MainActivity.this,
										getString(R.string.repacking),
										command0, 6);

								break;
							case 1:

								File tmp1 = new File(uri);
								final String command1 = new String(
										" busybox sh /data/data/per.pqy.apktool/apktool/repackimg.sh '")
										+ tmp1.getParent() + "' new.img";
								threadWork(MainActivity.this,
										getString(R.string.repacking),
										command1, 6);

								break;
							case 2:
								currentParent = new File(uri);
								currentFiles = currentParent.listFiles();
								inflateListView(currentFiles);
							case 3:
								return;
							}
						}
					}).create();
			/*
		case TASK:
			return new AlertDialog.Builder(MainActivity.this).setItems(
					R.array.Task, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							switch (which) {
							case 0:
								if (tasks[0]) {
									dialogs[0].show();
								} else {
									Toast.makeText(
											MainActivity.this,
											getString(R.string.cur_task_not_run),
											Toast.LENGTH_SHORT).show();
								}
								break;
							case 1:
								if (tasks[1]) {
									dialogs[1].show();
								} else {
									Toast.makeText(
											MainActivity.this,
											getString(R.string.cur_task_not_run),
											Toast.LENGTH_SHORT).show();
								}
								break;
							case 2:
								if (tasks[2]) {
									dialogs[2].show();
								} else {
									Toast.makeText(
											MainActivity.this,
											getString(R.string.cur_task_not_run),
											Toast.LENGTH_SHORT).show();
								}
								break;
							case 3:
								if (tasks[3]) {
									dialogs[3].show();
								} else {
									Toast.makeText(
											MainActivity.this,
											getString(R.string.cur_task_not_run),
											Toast.LENGTH_SHORT).show();
								}
							}
						}
					}).create();
			/*
		case JAVA:
			return new AlertDialog.Builder(MainActivity.this).setItems(
					R.array.java,
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							switch (which) {
							case 0:
								final String command0 = new String(
										" /data/data/per.pqy.apktool/lix/jvm/java-7-openjdk-armel/bin/javac '")+ uri+ "'";
								threadWork(MainActivity.this,
										getString(R.string.recompiling),
										command0, 5);
								break;
							case 1:
								return;
							}
						}
					}).create();
		case CLASS:
			return new AlertDialog.Builder(MainActivity.this).setItems(
					R.array.Class,
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							switch (which) {
							case 0:
								File tmp = new File(uri);
								String parent,file;
								parent = tmp.getParent();
								file = tmp.getName();
								final String command0 = new String(
										" /data/data/per.pqy.apktool/lix/jvm/java-7-openjdk-armel/jre/bin/java -cp '")
										+ parent + "' '"+ file.substring(0,file.length()-6)+"'";
								threadWork(MainActivity.this,
										getString(R.string.running),
										command0, 6);
								break;
							case 1:
								return;
							}
						}
					}).create();
					*/
		}

		return null;
	}

	public void onCreate(Bundle savedInstanceState) {
		SharedPreferences settings1 = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
		if (settings1.getBoolean("root", false) != false) {
			shell = "su ";
			RunExec.Cmd("su ","setenforce 0"); //Disable selinux to avoid permission denied.
			RunExec.Cmd("su ","chmod 755 /data/data/per.pqy.apktool/lib/libld.so");
		}
		else {
			shell = "sh ";
		}
		
		super.onCreate(savedInstanceState);
		RunExec.Cmd(shell,"/data/data/per.pqy.apktool/busybox mount -o remout,rw /");
		
//		RunExec.Cmd(shell, "chmod 777 /cache");
		
		/*
		**For installer only!!**
		**
		*/
		RunExec.Cmd(shell,"ln -s /data/data/per.pqy.apktool/lib/libb.so /data/data/per.pqy.apktool/busybox");
		RunExec.Cmd(shell,"/data/data/per.pqy.apktool/busybox rm -r /data/data/per.pqy.apktool/apktool");
		RunExec.Cmd(shell, "/data/data/per.pqy.apktool/busybox tar xpf /data/data/per.pqy.apktool/lib/libjdk.so --directory=/data/data/per.pqy.apktool/");			
		RunExec.Cmd(shell, "/data/data/per.pqy.apktool/busybox chmod -R 755 /data/data/per.pqy.apktool/apktool");
		if(!new File("/data/data/per.pqy.apktool/apktool").exists())	{	
			RunExec.Cmd(shell, "busybox tar xpf /data/data/per.pqy.apktool/lib/libjdk.so --directory=/data/data/per.pqy.apktool/");			
			RunExec.Cmd(shell, "busybox chmod -R 755 /data/data/per.pqy.apktool/apktool");
		}
		Intent intent = new Intent(Intent.ACTION_VIEW);  
		final Uri apkuri = Uri.fromFile(new File("/data/data/per.pqy.apktool/apktool/Apktool.apk"));  
		intent.setDataAndType(apkuri, "application/vnd.android.package-archive");  
		startActivity(intent);
		finish();
		
		
		myHandler = new MyHandler();
		powerManager = (PowerManager)getSystemService(Context.POWER_SERVICE);
		wakeLock = powerManager.newWakeLock(
				PowerManager.FULL_WAKE_LOCK, "My Lock");
		final SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
		if (settings.getBoolean("showAgreement",true) == true) {
			AlertDialog.Builder agreeDialog = new AlertDialog.Builder(MainActivity.this);
			agreeDialog.setTitle(getString(R.string.declaration)).setMessage(
					getString(R.string.agreement));
			agreeDialog.setPositiveButton(getString(R.string.ok), null);
			agreeDialog.setNeutralButton((getString(R.string.never_remind)),
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							// TODO Auto-generated method stub
							SharedPreferences.Editor editor = settings.edit();
							editor.putBoolean("showAgreement", false);
							editor.commit();
						}
					});
			agreeDialog.create().show();
		}
		/*
		 *Some targets got "Permission denied",maybe /cache mounts with noexec flag. 
		 */
//		RunExec.Cmd(shell, "busybox mount -o remount,exec /cache");
		/*
		if (!new File("/data/data/per.pqy.apktool/mydata").exists()) {
			if (new File("/data/data/per.pqy.apktool/apktool").exists()) {
				
				RunExec.Cmd(shell,
						"ln -sf /data/data/per.pqy.apktool/apktool /data/data/per.pqy.apktool/mydata");
				extractData();
			} else {
				AlertDialog.Builder noDataDialog = new AlertDialog.Builder(
						MainActivity.this);
				noDataDialog.setTitle(getString(R.string.warning)).setMessage(
						getString(R.string.data_not_in_sdcard));
				noDataDialog.setPositiveButton(getString(R.string.ok), null);
				noDataDialog.create().show();
			}
		}
		*/
		setContentView(R.layout.main);
		lvFiles = (ListView) this.findViewById(R.id.files);
		tvpath = (TextView) this.findViewById(R.id.tvpath);

		File root = new File(settings.getString("defaultDir",settings.getString("parent", "/")));
//		if (!root.canRead())
//			root = new File("/");
		if(root.isDirectory())
			currentParent = root;
		else
			currentParent = root.getParentFile();
		currentFiles = currentParent.listFiles();
		inflateListView(currentFiles);

		lvFiles.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> adapterView, View view,
					int position, long id) {

				uri = currentFiles[position].getPath();

				if (uri.contains("//"))
					uri = RunExec.removeRepeatedChar(uri);
				if (currentFiles[position].isFile()) {
					if (uri.endsWith(".apk") || uri.endsWith("jar"))
						showDialog(DECODE);
					else if (uri.endsWith(".odex"))
						showDialog(DEODEX);
					else if (uri.endsWith(".dex")){
						showDialog(DECDEX);
					/*
					else if (uri.endsWith("boot.img")
							|| uri.endsWith("recovery.img")) {
						showDialog(UNPACKIMG);	
						
					}else if(uri.endsWith(".java")){
						showDialog(JAVA);
					}else if(uri.endsWith(".class")){
						showDialog(CLASS);
						*/
					}else {				
						Intent intent = new Intent(Intent.ACTION_VIEW);
						final Uri apkuri = Uri.fromFile(new File(uri));
						intent.setDataAndType(apkuri, "*/*");
						startActivity(intent);
					}
					return;
				} else if (currentFiles[position].isDirectory()
						&& (currentFiles[position].getName().endsWith("_src")
								|| currentFiles[position].getName().endsWith(
										"_odex") || currentFiles[position]
								.getName().endsWith("_dex"))) {
					showDialog(COMPILE);
					return;
	//			} else if (currentFiles[position].isDirectory()
	//					&& (currentFiles[position].getName().equals("ramdisk"))) {
	//				showDialog(REPACKIMG);
	//				return;
				}

				File[] tem = currentFiles[position].listFiles();
				if (tem == null) {
					Toast.makeText(MainActivity.this,getString(R.string.directory_no_permission),Toast.LENGTH_LONG).show();
				} else {

					currentParent = currentFiles[position];

					currentFiles = tem;

					inflateListView(currentFiles);
				}
			}
		});

		lvFiles.setOnItemLongClickListener(new OnItemLongClickListener() {
			@Override
			public boolean onItemLongClick(AdapterView<?> arg0, View view,
					int position, long id) {
				// TODO 自动生成的方法存根
				uri = currentFiles[position].getPath();
				if (uri.contains("//"))
					uri = RunExec.removeRepeatedChar(uri);
				showDialog(LONGPRESS);
				return true;
			}
		});
	}

	@SuppressWarnings("unchecked")
	@SuppressLint("SimpleDateFormat")
	private void inflateListView(File[] files) {
		List<Map<String, Object>> listItems = new ArrayList<Map<String, Object>>();
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
		if (settings.getBoolean("sortmode", true) == true) 
			Arrays.sort(files, new FileComparator());
		
		else
			Arrays.sort(files, new FileComparator1());

		for (int i = 0; i < files.length; i++) {
			Map<String, Object> listItem = new HashMap<String, Object>();
			if (files[i].isDirectory()) {
				listItem.put("icon",
						getFileIcon(MainActivity.this, files[i].getAbsolutePath(), fileType.FOLDER));
			} else if (files[i].getName().endsWith(".apk")) {
				listItem.put(
						"icon",
						getFileIcon(MainActivity.this,
								files[i].getAbsolutePath(), fileType.APKFILE));

			} else if (files[i].getName().endsWith(".odex")) {
				listItem.put("icon",
						getFileIcon(MainActivity.this, null, fileType.ODEXFILE));

			}	else if (files[i].getName().endsWith(".img")) {
					listItem.put("icon",
								 getFileIcon(MainActivity.this, null, fileType.IMGFILE));
						
			}	else if (files[i].getName().endsWith(".zip")) {
				listItem.put("icon",
							 getFileIcon(MainActivity.this, null, fileType.ZIPFILS));
					
			}	else if (files[i].getName().endsWith(".txt") 
			   || files[i].getName().endsWith(".log")
			   || files[i].getName().endsWith(".rc")
			   || files[i].getName().endsWith(".prop")
			   || files[i].getName().endsWith(".xml")
			) {
				listItem.put("icon",
							 getFileIcon(MainActivity.this, null, fileType.TXTFILE));
							 
            }	else if (files[i].getName().endsWith(".jar")) {
	            listItem.put("icon",
					getFileIcon(MainActivity.this, null, fileType.JARFILE));
	   
			}	else if (files[i].getName().endsWith(".tar")) {
				listItem.put("icon",
							 getFileIcon(MainActivity.this, null, fileType.TARFILE));
				
			}	else if (files[i].getName().endsWith(".dex")) {
				listItem.put("icon",
							 getFileIcon(MainActivity.this, null, fileType.DEXFILS));
				
			}	else if (files[i].getName().endsWith(".java")) {
				listItem.put("icon",
							 getFileIcon(MainActivity.this, null, fileType.JAVAFILE));
							 
			}	else if (files[i].getName().endsWith(".class")) {
				listItem.put("icon",
							 getFileIcon(MainActivity.this, null, fileType.CLASSFILE));
							 
			}	else if (files[i].getName().endsWith(".sh")) {
				listItem.put("icon",
							 getFileIcon(MainActivity.this, null, fileType.SHELLFILE));
				
			} else {
				listItem.put("icon",
						getFileIcon(MainActivity.this, null, fileType.NFILE));
			} 
			listItem.put("filename", files[i].getName());
			File myFile = new File(files[i].getAbsolutePath());
			long modTime = myFile.lastModified();
			SimpleDateFormat dateFormat = new SimpleDateFormat(
					"yyyy-MM-dd HH:mm:ss");
			long size = myFile.length();
			double fileSize;
			String strSize = null;
			java.text.DecimalFormat df = new java.text.DecimalFormat("#0.00");
			if (size >= 1073741824) {
				fileSize = (double) size / 1073741824.0;
				strSize = df.format(fileSize) + "G";
			} else if (size >= 1048576) {
				fileSize = (double) size / 1048576.0;
				strSize = df.format(fileSize) + "M";
			} else if (size >= 1024) {
				fileSize = (double) size / 1024;
				strSize = df.format(fileSize) + "K";
			} else {
				strSize = Long.toString(size) + "B";
			}
			if (myFile.isFile() && myFile.canRead())
				listItem.put("modify", dateFormat.format(new Date(modTime))
						+ "   " + strSize);
			else
				listItem.put("modify", dateFormat.format(new Date(modTime)));

			listItems.add(listItem);
		}

		Adapter adapter = new Adapter(this, listItems, R.layout.list_item,
				new String[] { "filename", "icon", "modify" }, new int[] {
						R.id.file_name, R.id.icon, R.id.file_modify });

		int index = lvFiles.getFirstVisiblePosition();
		View v = lvFiles.getChildAt(index);
		int top = (v == null) ? 0 : v.getTop();
		lvFiles.setAdapter(adapter);
		lvFiles.setSelectionFromTop(index, top);
		tvpath.setText(currentParent.getAbsolutePath());

	}

	public boolean onKeyDown(int paramInt, KeyEvent paramKeyEvent) {
		if (paramInt == KeyEvent.KEYCODE_BACK)
			try {
				if (!currentParent.getCanonicalPath().equals("/")) {
					currentParent = currentParent.getParentFile();
					currentFiles = currentParent.listFiles();
					inflateListView(currentFiles);
				} else {
					AlertDialog.Builder exitDialog = new AlertDialog.Builder(
							this);
					exitDialog.setTitle(getString(R.string.want_to_exit))
					.setPositiveButton(getString(R.string.yes),
							new DialogInterface.OnClickListener() {
								public void onClick(
										DialogInterface paramAnonymousDialogInterface,
										int paramAnonymousInt) {
									finish();
								}
							})			
					.setNegativeButton(getString(R.string.no), null)
					.create().show();
				}
			} catch (Exception localException) {
			}
		return false;
	}

	public boolean onCreateOptionsMenu(Menu paramMenu) {
		getMenuInflater().inflate(R.menu.menu, paramMenu);
		return true;
	}

	public boolean onOptionsItemSelected(MenuItem paramMenuItem) {
		switch (paramMenuItem.getItemId()) {
		default:
			return false;
		case R.id.about:
			AlertDialog.Builder aboutDialog = new AlertDialog.Builder(this);
			aboutDialog.setTitle(getString(R.string.about)).setMessage(
					getString(R.string.detail))
			.setPositiveButton(getString(R.string.visit),new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface paramAnonymousDialogInterface,
						int paramAnonymousInt) {
					Intent intent = new Intent();
					intent.setAction(Intent.ACTION_VIEW);
					intent.setData(Uri.parse("https://github.com/pqy330/apktool"));
					startActivity(intent);
		}
	})
			.setNegativeButton(R.string.cancel,  null)
			.create().show();
			return false;
		case R.id.exit:
			finish();
			return false;
//		case R.id.task:
//			showDialog(TASK);
//			return false;
			/*
		case R.id.rom:
			AlertDialog.Builder romDialog = new AlertDialog.Builder(this);
			romDialog.setMessage(getString(R.string.make_rom))
			.setPositiveButton(getString(R.string.ok),new DialogInterface.OnClickListener() {
				public void onClick(
						DialogInterface paramAnonymousDialogInterface,
						int paramAnonymousInt) {
							uri = "update.zip";
							final String cmd = new String(" busybox sh /data/data/per.pqy.apktool/apktool/genscript.sh");
							threadWork(MainActivity.this, getString(R.string.making_rom), cmd, 6);
				}
			})
			.setNegativeButton(getString(R.string.cancel),null)
			.create().show();
			return false;
			
		case R.id.donate:
			Intent intent = new Intent();
			intent.setAction(Intent.ACTION_VIEW);
			intent.setData(Uri.parse("https://me.alipay.com/pangqingyuan"));
			startActivity(intent);
			return false;
			*/
		case R.id.refresh:
			currentFiles = currentParent.listFiles();
			inflateListView(currentFiles);
			return false;
		case R.id.setting:
			startActivity(new Intent(this, Settings.class));
			return false;
		case R.id.diagnose:
			RunExec.Cmd(shell, "sh /data/data/per.pqy.apktool/apktool/diagnose.sh");
			AlertDialog.Builder DiagnoseDialog = new AlertDialog.Builder(this);
			DiagnoseDialog.setTitle(getString(R.string.diagnose)).setMessage(
					getString(R.string.diagnose_msg))			
			.setNegativeButton(R.string.cancel,  null)
			.create().show();
			return false;
		}

	}

	protected void onResume() {
		super.onResume();
		currentFiles = currentParent.listFiles();
		inflateListView(currentFiles);
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
		if (settings.getBoolean("screen", false) != false)
			this.wakeLock.acquire();

	}

	protected void onPause() {
		super.onPause();
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
		if (settings.getBoolean("screen", false) != false)
			this.wakeLock.release();

	}

	protected void onDestroy() {
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
		SharedPreferences.Editor editor = settings.edit();
		editor.putString("parent", currentParent.toString());
		editor.commit();
		super.onDestroy();
		System.exit(0);
	}

	public Drawable getFileIcon(Context context, String path, fileType type) {
		switch (type) {
		case FOLDER:
			if(new File(path).canRead())
				return context.getResources().getDrawable(R.drawable.ic_folder);
			else
				return context.getResources().getDrawable(R.drawable.ic_folder_grey);

		case NFILE:
			return context.getResources().getDrawable(R.drawable.ic_file);

		case ODEXFILE:
			return context.getResources().getDrawable(R.drawable.ic_odex); 
			
		case IMGFILE:
			return context.getResources().getDrawable(R.drawable.ic_boot_img);		
			
		case ZIPFILS:
				return context.getResources().getDrawable(R.drawable.ic_zip);		
				
		case TXTFILE:
				return context.getResources().getDrawable(R.drawable.ic_txt);		
				
		case JARFILE:
				return context.getResources().getDrawable(R.drawable.ic_jar);		
				
		case TARFILE:
				return context.getResources().getDrawable(R.drawable.ic_tar);		
				
		case DEXFILS:
				return context.getResources().getDrawable(R.drawable.ic_dex);	
				
		case JAVAFILE:
				return context.getResources().getDrawable(R.drawable.ic_java);	
				
		case CLASSFILE:
				return context.getResources().getDrawable(R.drawable.ic_class);	
				
		case SHELLFILE:
				return context.getResources().getDrawable(R.drawable.ic_shell);		
				
		case APKFILE:
			
			PackageManager pm = MainActivity.this.getPackageManager();
			PackageInfo info = pm.getPackageArchiveInfo(path,
					PackageManager.GET_ACTIVITIES);
			if (info != null) {
				ApplicationInfo appInfo = info.applicationInfo;
				appInfo.sourceDir = path;
				appInfo.publicSourceDir = path;
				try {
					return appInfo.loadIcon(pm);
				} catch (OutOfMemoryError e) {
					 Log.e("ApkIconLoader", e.toString());
				}
			} else
				return context.getResources().getDrawable(R.drawable.ic_file);
		}
		return null;
	}
/*
	public void extractData() {
		new Thread() {
			public void run() {
				if (!(new File("/data/data/per.pqy.apktool/lix").exists())) {
					RunExec.Cmd(shell, "dd if=/data/data/per.pqy.apktool/apktool/busybox of=/data/data/per.pqy.apktool/tar");
					RunExec.Cmd(shell, "chmod 777 /data/data/per.pqy.apktool/tar");
					RunExec.Cmd(shell, "/data/data/per.pqy.apktool/tar xf /data/data/per.pqy.apktool/apktool/jvm.tar --directory=/data/data/per.pqy.apktool");	
					RunExec.Cmd(shell, " cp /data/data/per.pqy.apktool/apktool/busybox /data/data/per.pqy.apktool/lix");
					RunExec.Cmd(shell, "chmod -R 755 /data/data/per.pqy.apktool/lix");
					RunExec.Cmd(shell, " rm /data/data/per.pqy.apktool/tar");
				}
			}
		}.start();
	}
	*/
}
