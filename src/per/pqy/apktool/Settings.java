package per.pqy.apktool;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;

//继承PreferenceActivity，并实现OnPreferenceChangeListener和OnPreferenceClickListener监听接口  
public class Settings extends PreferenceActivity implements OnPreferenceChangeListener,   
OnPreferenceClickListener{  
  @Override  
  public void onCreate(Bundle savedInstanceState) {  
      super.onCreate(savedInstanceState);   
      addPreferencesFromResource(R.xml.preference);  
  }
@Override
public boolean onPreferenceClick(Preference arg0) {
	// TODO Auto-generated method stub
	return false;
}
@Override
public boolean onPreferenceChange(Preference preference, Object newValue) {
	// TODO Auto-generated method stub
	return false;
}
}
 
 