package com.google.googletest;

import java.io.IOException;
import java.io.InputStream;

import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.plus.Plus;
import com.google.android.gms.plus.model.people.Person;

//import android.support.v7.app.ActionBarActivity;
import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender.SendIntentException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity implements OnClickListener,
		ConnectionCallbacks, OnConnectionFailedListener, AsyncResponse{
	
	private static final int RC_SIGN_IN = 0;
	private static final String TAG = "MainActivity";
	private GoogleApiClient mGoogleApiClient;
	private static final int PROFILE_PIC_SIZE = 180;
	
	private static final String TAGTKN = "RetrieveAccessToken";
    private static final int REQ_SIGN_IN_REQUIRED = 55664;
	
	private boolean mIntentInProgress;
	private boolean mSignInClicked;
	
	private ConnectionResult mConnectionResult;
	
	private SignInButton btnSignIn;
	private Button btnSignOut, btnRevokeAccess;
	private ImageView imgProfilePic;
	private TextView txtName, txtEmail;
	private LinearLayout llProfileLayout, llData;
	
	// Initialize class for obtain token
	getGmailToken getTkn = new getGmailToken();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		imgProfilePic = (ImageView) findViewById(R.id.imgProfilePic);
		txtName = (TextView) findViewById(R.id.txtName);
		txtEmail = (TextView) findViewById(R.id.txtEmail);
		llProfileLayout = (LinearLayout) findViewById(R.id.llProfile);
		llData = (LinearLayout) findViewById(R.id.llData);
		
		btnSignIn = (SignInButton) findViewById(R.id.btn_sign_in);
		btnSignIn.setOnClickListener(this);
		
		btnSignOut = (Button) findViewById(R.id.btn_sign_out);
		btnSignOut.setOnClickListener(this);
		
		btnRevokeAccess = (Button) findViewById(R.id.btn_revoke_access);
		btnRevokeAccess.setOnClickListener(this);
		
		getTkn.delegate = this;
		
		//Initializing google plus api client
		mGoogleApiClient = new GoogleApiClient.Builder(this)
		.addConnectionCallbacks(this)
		.addOnConnectionFailedListener(this).addApi(Plus.API)
		.addScope(Plus.SCOPE_PLUS_LOGIN).build();
			
	}
	
	protected void onStart(){
		super.onStart();
		mGoogleApiClient.connect();
	}
	
	protected void onStop(){
		super.onStop();
		if(mGoogleApiClient.isConnected()){
			mGoogleApiClient.disconnect();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onClick(View v) {
		switch(v.getId()){
		case R.id.btn_sign_in:
			signInWithGplus();
			break;
		case R.id.btn_sign_out:
			signOutFromGplus();
			break;
		case R.id.btn_revoke_access:
			revokeGplusAccess();
            break;
		case R.id.btn_accept:
			sendData();
		}	
	}

	@Override
	public void onConnectionFailed(ConnectionResult result) {
		if(!result.hasResolution()){
			GooglePlayServicesUtil.getErrorDialog(result.getErrorCode(), this, 
					0).show();
			return;
		}
		
		if(!mIntentInProgress){
			//Store the connection for later usage
			mConnectionResult = result;
			
			if(mSignInClicked){
				// The user has already clicked 'sign-in' so we attempt to
	            // resolve all
	            // errors until the user is signed in, or they cancel.
				resolveSignInError();
			}
		}
		
	}

	@Override
	public void onConnected(Bundle arg0) {
		mSignInClicked = false;
		Toast.makeText(this, "User is connected", Toast.LENGTH_LONG).show();
		
		// Get User's information
		getProfileInformation();
		
		// Update the UI after sign-in
	    updateUI(true);		
	}

	@Override
	public void onConnectionSuspended(int arg0) {
		mGoogleApiClient.connect();
		updateUI(false);
	}
	
	private void updateUI(boolean isSignedIn){
		if(isSignedIn){
			btnSignIn.setVisibility(View.GONE);
			//btnSignOut.setVisibility(View.VISIBLE);
	        //btnRevokeAccess.setVisibility(View.VISIBLE);
	        llProfileLayout.setVisibility(View.VISIBLE);
	        llData.setVisibility(View.VISIBLE);
	    }
		else {
	        btnSignIn.setVisibility(View.VISIBLE);
	        //btnSignOut.setVisibility(View.GONE);
	        //btnRevokeAccess.setVisibility(View.GONE);
	        llProfileLayout.setVisibility(View.GONE);
	        llData.setVisibility(View.GONE);
	    }
	}
	
	/*
	 * Sign-in into google
	 */
	private void signInWithGplus(){
		if(!mGoogleApiClient.isConnecting()){
			mSignInClicked = true;
			resolveSignInError();
		}
	}
	
	/*
	 * Method to resolve any sign-in errors
	 */
	private void resolveSignInError(){
		if(mConnectionResult.hasResolution()){
			try{
				mIntentInProgress = true;
				mConnectionResult.startResolutionForResult(this, RC_SIGN_IN);
			}
			catch(SendIntentException e){
				mIntentInProgress = false;
				mGoogleApiClient.connect();
			}
		}
	}
	
	/*
	 * User's information name, email, profile pic 
	 */
	private void getProfileInformation(){
		try{
			if(Plus.PeopleApi.getCurrentPerson(mGoogleApiClient) != null){
				Person currentPerson = Plus.PeopleApi.getCurrentPerson(mGoogleApiClient);
				
				String personName = currentPerson.getDisplayName();
				String perosnPhotoUrl = currentPerson.getImage().getUrl();
				String personGooglePlusProfile = currentPerson.getUrl();
				String email = Plus.AccountApi.getAccountName(mGoogleApiClient);
				
				Log.e(TAG, "Name: " + personName + ", plusProfile: "
						+ personGooglePlusProfile + ", email: " + email
						+ ", Image: " + perosnPhotoUrl);
				
				txtName.setText(personName);
				txtEmail.setText(email);
				perosnPhotoUrl = perosnPhotoUrl.substring(0,
						perosnPhotoUrl.length() - 2)
						+ PROFILE_PIC_SIZE;
				
				new LoadProfileImage(imgProfilePic).execute(perosnPhotoUrl);
			}
			else{
				Toast.makeText(getApplicationContext(), 
						"Person informations is null", Toast.LENGTH_LONG).show();
			}
		}
		catch(Exception e){
			e.printStackTrace();
		}
	}
	
	/*
	 * Background Async task to load user profile picture from url
	 */
	private class LoadProfileImage extends AsyncTask<String, Void, Bitmap>{
		ImageView bmImage;
		
		public LoadProfileImage(ImageView bmImage){
			this.bmImage = bmImage;
		}
		
		@Override
		protected Bitmap doInBackground(String... urls) {
			String urldisplay = urls[0];
			Bitmap mIcon11 = null;
			
			try{
				InputStream in = new java.net.URL(urldisplay).openStream();
				mIcon11 = BitmapFactory.decodeStream(in);
			}
			catch(Exception e){
				Log.e("Error", e.getMessage());
				e.printStackTrace();
			}
			return mIcon11;
		}
		
		protected void onPostExecute(Bitmap result){
			bmImage.setImageBitmap(result);
		}
	}
	
	/*
	 * Sign-out from google 
	 */
	private void signOutFromGplus(){
		if(mGoogleApiClient.isConnected()){
			Plus.AccountApi.clearDefaultAccount(mGoogleApiClient);
			mGoogleApiClient.disconnect();
			mGoogleApiClient.connect();
			updateUI(false);
		}
	}
	
	/*
	 * Revoking access from google 
	 */
	private void revokeGplusAccess(){
		if(mGoogleApiClient.isConnected()){
			Plus.AccountApi.clearDefaultAccount(mGoogleApiClient);
			Plus.AccountApi.revokeAccessAndDisconnect(mGoogleApiClient)
				.setResultCallback(new ResultCallback<Status>() {
					
					@Override
					public void onResult(Status arg0) {
						Log.e(TAG, "User acces revodes!");
						mGoogleApiClient.connect();
						updateUI(false);
					}
				});
		}
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
	    if (requestCode == RC_SIGN_IN) {
	    	mIntentInProgress = false;
	        if (resultCode == RESULT_OK) {
	            // Make sure the app is not already connected or attempting to connect
	            if (!mGoogleApiClient.isConnecting() &&
	                    !mGoogleApiClient.isConnected()) {
	                mGoogleApiClient.connect();
	            }
	        }
	    }
	}
	
	private class getGmailToken extends AsyncTask<String, Void, String>{
		
		// Declare interface AsyncResponse
		public AsyncResponse delegate=null;
		
		@Override
		protected String doInBackground(String... params) {
			String userEmail = params[0];
			String scope = "oauth2:https://www.googleapis.com/auth/gmail.readonly";
			String token = null;			
			try{
				// Obtain token for access gmail account
				token = GoogleAuthUtil.getToken(getApplicationContext(), userEmail, scope);
			}
			catch(IOException e){
				Log.e(TAGTKN, e.getMessage());
			}
			catch(UserRecoverableAuthException e){
				startActivityForResult(e.getIntent(), REQ_SIGN_IN_REQUIRED);
			} 
			catch (GoogleAuthException e) {
				Log.e(TAGTKN, e.getMessage());
			}
			return token;
		}
		
		protected void onPostExecute(String result){
			// Send data resulting in the previous method
			delegate.processFinish(result);
		}
	}

	// Method to obtain the results of the class AsyncTask
	@Override
	public void processFinish(String output) {
		// TODO Auto-generated method stub
		
	}
	
	private void sendData(){
		
	}
}
