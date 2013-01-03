package org.moap.logger.server;

import com.mendhak.gpslogger.GpsMainActivity;
import com.mendhak.gpslogger.loggers.ServerLogger;

import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.view.Menu;
import android.widget.CheckBox;
import android.widget.Toast;
import arida.ufc.br.moapgpstracker.R;
import arida.ufc.br.moapgpstracker.R.layout;
import arida.ufc.br.moapgpstracker.R.menu;

public class ServerAuthorizationActivity extends PreferenceActivity {
	private ServerHelper serverHelper;
	private SharedPreferences prefs;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
//		setContentView(R.layout.activity_server_authotization);
		addPreferencesFromResource(R.xml.serversettings);
		
		this.serverHelper = new ServerHelper(getApplicationContext(),getSharedPreferences(GpsMainActivity.MOAP, Context.MODE_PRIVATE));
		
		this.prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
		
		
		
		final CheckBoxPreference log_server = (CheckBoxPreference)findPreference("log_server");
		log_server.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			
			public boolean onPreferenceClick(Preference preference) {
				// TODO Auto-generated method stub
				final String token = getSharedPreferences(GpsMainActivity.MOAP, Context.MODE_PRIVATE).getString("user.gpstrackerserver.token", null);
				CheckBoxPreference log_server = (CheckBoxPreference)preference;
				if(log_server.isChecked()){
					if(token==null){
						Toast.makeText(getApplicationContext(),  "You should log in first", Toast.LENGTH_SHORT).show();
						log_server.setChecked(false);
					}
				}
				
				return true;
			}
		});
		
		
		
		PreferenceScreen preferenceScreen = getPreferenceScreen();
		
		Preference login_button = (Preference)findPreference("server_login_button");
		login_button.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			
			public boolean onPreferenceClick(Preference preference) {
				// TODO Auto-generated method stub
				
				final String login_name = prefs.getString("server_login_key", "");
				final String login_pass = prefs.getString("server_pass_key", "");

				if(!(login_name.equalsIgnoreCase("") || login_pass.equalsIgnoreCase("") )){
					serverHelper.signinRequest(login_name, login_pass);
				}
				else{
					Toast.makeText(getApplicationContext(),  "Set up your informations", Toast.LENGTH_SHORT).show();
				}
				
				return true;
			}
		});
		
		Preference logout_button = (Preference)findPreference("server_logout_button");
		logout_button.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			
			public boolean onPreferenceClick(Preference preference) {
				// TODO Auto-generated method stub
				
				final String login_name = prefs.getString("server_login_key", "");
				
				
				serverHelper.signoutRequest(login_name);
				log_server.setChecked(false);
				return true;
			}
		});

//		if(token==null){
//
//			preferenceScreen.addPreference(login_button);
//			preferenceScreen.removePreference(logout_button);
//			
//		}
//		else{
//			preferenceScreen.addPreference(logout_button);
//			preferenceScreen.removePreference(login_button);
//		}
		
		
		
		Preference signup_button = (Preference)findPreference("server_signup_button");
		signup_button.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			
			public boolean onPreferenceClick(Preference preference) {
				// TODO Auto-generated method stub
				
				final String signup_name =prefs.getString("server_signup_name", "");
				final String signup_username =prefs.getString("server_signup_username", "");
				final String signup_pass =prefs.getString("server_signup_pass", "");
				
				if(!(signup_name.equalsIgnoreCase("") || signup_username.equalsIgnoreCase("") || signup_pass.equalsIgnoreCase(""))){
					serverHelper.signupRequest(signup_name, signup_username, signup_pass);
				}
				else{
					Toast.makeText(getApplicationContext(),  "Set up your informations", Toast.LENGTH_SHORT).show();
				}
				
				return true;
			}
		});
		
		
		
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
//		getMenuInflater().inflate(R.menu.activity_server_authorization, menu);
		return true;
	}
	
	private void update(){
		
		onContentChanged();
	}
	
	

}
