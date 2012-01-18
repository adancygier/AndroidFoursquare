package net.londatiga.fsq;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

import net.londatiga.fsq.FoursquareDialog.FsqDialogListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import android.app.ProgressDialog;
import android.content.Context;
import android.location.Location;
import android.location.LocationManager;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

/**
 * 
 * @author Lorensius W. L. T <lorenz@londatiga.net>
 *
 */
public class FoursquareApp {
	private FoursquareSession mSession;
	private FoursquareDialog mDialog;
	private FsqAuthListener mListener;
	private ProgressDialog mProgress;
	private String mTokenUrl;
	private String mAccessToken;
	
	/**
	 * Callback url, as set in 'Manage OAuth Costumers' page (https://developer.foursquare.com/)
	 */
	public static final String CALLBACK_URL = "myapp://connect";
	private static final String AUTH_URL = "https://foursquare.com/oauth2/authenticate?response_type=code";
	private static final String TOKEN_URL = "https://foursquare.com/oauth2/access_token?grant_type=authorization_code";	
	private static final String API_URL = "https://api.foursquare.com/v2";
	
	private static final String TAG = "FoursquareApi";
	
	public FoursquareApp(Context context, String clientId, String clientSecret) {
		mSession		= new FoursquareSession(context);
		
		mAccessToken	= mSession.getAccessToken();
		
		mTokenUrl		= TOKEN_URL + "&client_id=" + clientId + "&client_secret=" + clientSecret
						+ "&redirect_uri=" + CALLBACK_URL;
		
		String url		= AUTH_URL + "&client_id=" + clientId + "&redirect_uri=" + CALLBACK_URL;
		
		FsqDialogListener listener = new FsqDialogListener() {
			@Override
			public void onComplete(String code) {
				getAccessToken(code);
			}
			
			@Override
			public void onError(String error) {
				mListener.onFail("Authorization failed");
			}
		};
		
		mDialog			= new FoursquareDialog(context, url, listener);
		mProgress		= new ProgressDialog(context);
		
		mProgress.setCancelable(false);
	}
    
	
	private void getAccessToken(final String code) {
		mProgress.setMessage("Getting access token ...");
		mProgress.show();
		
		new Thread() {
			@Override
			public void run() {
				Log.i(TAG, "Getting access token");
				
				int what = 0;
				
				try {
					URL url = new URL(mTokenUrl + "&code=" + code);
					
					Log.i(TAG, "Opening URL " + url.toString());
					
					HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
					
					urlConnection.setRequestMethod("GET");
					urlConnection.setDoInput(true);
					urlConnection.setDoOutput(true);
					
					urlConnection.connect();
					
					JSONObject jsonObj  = (JSONObject) new JSONTokener(streamToString(urlConnection.getInputStream())).nextValue();
		        	mAccessToken 		= jsonObj.getString("access_token");
		        	
		        	Log.i(TAG, "Got access token: " + mAccessToken);
				} catch (Exception ex) {
					what = 1;
					
					ex.printStackTrace();
				}
				
				mHandler.sendMessage(mHandler.obtainMessage(what, 1, 0));
			}
		}.start();
	}
	
	private void fetchUserName() {
		mProgress.setMessage("Finalizing ...");
		
		new Thread() {
			@Override
			public void run() {
				Log.i(TAG, "Fetching user name");
				int what = 0;
		
				try {
					URL url = new URL(API_URL + "/users/self?oauth_token=" + mAccessToken);
					
					Log.d(TAG, "Opening URL " + url.toString());
					
					HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
					
					urlConnection.setRequestMethod("GET");
					urlConnection.setDoInput(true);
					urlConnection.setDoOutput(true);
					
					urlConnection.connect();
					
					String response		= streamToString(urlConnection.getInputStream());
					JSONObject jsonObj 	= (JSONObject) new JSONTokener(response).nextValue();
		       
					JSONObject resp		= (JSONObject) jsonObj.get("response");
					JSONObject user		= (JSONObject) resp.get("user");
					
					String firstName 	= user.getString("firstName");
		        	String lastName		= user.getString("lastName");
		        
		        	Log.i(TAG, "Got user name: " + firstName + " " + lastName);
		        	
		        	mSession.storeAccessToken(mAccessToken, firstName + " " + lastName);
				} catch (Exception ex) {
					what = 1;
					
					ex.printStackTrace();
				}
				
				mHandler.sendMessage(mHandler.obtainMessage(what, 2, 0));
			}
		}.start();
	}
	
	private Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			if (msg.arg1 == 1) {
				if (msg.what == 0) {
					fetchUserName();
				} else {
					mProgress.dismiss();
					
					mListener.onFail("Failed to get access token");
				}
			} else {
				mProgress.dismiss();
				
				mListener.onSuccess();
			}
		}
	};
	
	public boolean hasAccessToken() {
		return (mAccessToken == null) ? false : true;
	}
    
    public void logOut() {
    	this.mSession.resetAccessToken();
    }
	
	public void setListener(FsqAuthListener listener) {
		mListener = listener;
	}
	
	public String getUserName() {
		return mSession.getUsername();
	}
	
	public void authorize() {
		mDialog.show();
	}
    
	public ArrayList<FoursquareVenue> getVenues(String lat, String lon, String query) throws MalformedURLException, IOException, JSONException {
		ArrayList<FoursquareVenue> venueList = new ArrayList<FoursquareVenue>();
        
		String ll 	= lat + "," + lon;
        String url_str = API_URL + "/venues/search?ll=" + ll + "&oauth_token=" + mAccessToken;
            
        if (query != null && query.length() > 0) {
        	String query_encoded = java.net.URLEncoder.encode(query);
    		query_encoded = query_encoded.replace("+", "%20");
        	url_str = url_str + "&query=" + query_encoded;
        }
            
		URL url;
		try {
			url = new URL(url_str);
		    Log.d(TAG, "Opening URL " + url.toString());
			HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
			urlConnection.setRequestMethod("GET");
			urlConnection.setDoInput(true);
			urlConnection.setDoOutput(true);
			urlConnection.connect();
			String response		= streamToString(urlConnection.getInputStream());
                
    			
            JSONObject jsonObj = (JSONObject) new JSONTokener(response).nextValue();
			JSONArray groups = (JSONArray) jsonObj.getJSONObject("response").getJSONArray("groups");
    		
			int length = groups.length();
                
    			
			if (length > 0) {
                for (int i = 0; i < groups.length(); i ++) {
    				JSONObject group = (JSONObject) groups.get(i);
    				JSONArray items = (JSONArray) group.getJSONArray("items");
        					
    				for (int j = 0; j < items.length(); j++) {
    					JSONObject item = (JSONObject) items.get(j);
        						
    					FoursquareVenue venue = new FoursquareVenue();
    					venue.id 		= item.getString("id");
    					venue.name		= item.getString("name");
        						
    					JSONObject location = (JSONObject) item.getJSONObject("location");
    					JSONArray categories= (JSONArray) item.getJSONArray("categories");
                            
                        //Log.v(TAG, "location: " +  location.toString());
                        //Log.v(TAG, "categories: " +  categories.toString());
                            
    					if (categories.length() > 0) {
                            JSONObject cat = (JSONObject) categories.get(0);
                                
                            if (cat.has("icon")) {
                                venue.icon = cat.getString("icon");
                            }
                            else {
                                venue.icon = "";	
                            }
    				    }
    					else {
    						venue.icon = "";
    					}
        						
                        if (location.has("address")) {
    					    venue.address = location.getString("address");
                        }
                        else {
                        	venue.address = "";
                        }
                            
                        if (location.has("distance")) {
    					    venue.distance = location.getInt("distance");
                        }
                        else {
                        	venue.distance = 0;
                        }
                        
                        if (location.has("lat")) {
                        	venue.lat = location.getDouble("lat");
                        }
                        else {
                        	venue.lat = 0;
                        }
                        
                        if (location.has("lng")) {
                        	venue.lng = location.getDouble("lng");
                        }
                        else {
                        	venue.lng = 0;
                        }
                            
                            
                        if (group.has("type")) {
    						venue.type = group.getString("type");
                        }
                        else {
                        	venue.type = "";
                        }
                            
                        if (location.has("city")) {
                            venue.city = location.getString("city");
                        }
                        else {
                            venue.city = "";
                        }
                            
                        if (location.has("state")) {
                            venue.state = location.getString("state");
                        }
                        else {
                            venue.state = "";
                        }
                            
                        if (location.has("postalCode")) {
                            venue.postalCode = location.getString("postalCode");
                        }
                        else {
                            venue.postalCode = "";
                        }
                            
                        if (item.has("hereNow") && item.getJSONObject("hereNow").has("count")) {
    					    venue.herenow = item.getJSONObject("hereNow").getInt("count");
                        }
                        else {
                            venue.herenow = 0;	
                        }
                                
    					venueList.add(venue);
    				}
                }
		    }
		} catch (MalformedURLException mfe) {
            Log.e(TAG, "Caught MalformedURLException in venues api call: " + mfe.toString());
			throw mfe;
		} catch (IOException ioe) {
            Log.e(TAG, "Caught IOException in venues api call: " + ioe.toString());
            throw ioe;
		} catch (JSONException jsone) {
            Log.e(TAG, "Caught JSONException in venues api call: " + jsone.toString());
            throw jsone;
		}
			
		return venueList;
	}
	
	public ArrayList<FsqVenue> getNearby(double latitude, double longitude) throws Exception {
		ArrayList<FsqVenue> venueList = new ArrayList<FsqVenue>();
		
		try {
			String ll 	= String.valueOf(latitude) + "," + String.valueOf(longitude);
			URL url 	= new URL(API_URL + "/venues/search?ll=" + ll + "&oauth_token=" + mAccessToken);
			
			Log.d(TAG, "Opening URL " + url.toString());
			
			HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
			
			urlConnection.setRequestMethod("GET");
			urlConnection.setDoInput(true);
			urlConnection.setDoOutput(true);
			
			urlConnection.connect();
			
			String response		= streamToString(urlConnection.getInputStream());
			JSONObject jsonObj 	= (JSONObject) new JSONTokener(response).nextValue();
			
			JSONArray groups	= (JSONArray) jsonObj.getJSONObject("response").getJSONArray("groups");
			
			int length			= groups.length();
			
			if (length > 0) {
				for (int i = 0; i < length; i++) {
					JSONObject group 	= (JSONObject) groups.get(i);
					JSONArray items 	= (JSONArray) group.getJSONArray("items");
					
					int ilength 		= items.length();
					
					for (int j = 0; j < ilength; j++) {
						JSONObject item = (JSONObject) items.get(j);
						
						FsqVenue venue 	= new FsqVenue();
						
						venue.id 		= item.getString("id");
						venue.name		= item.getString("name");
						
						JSONObject location = (JSONObject) item.getJSONObject("location");
						
						Location loc 	= new Location(LocationManager.GPS_PROVIDER);
						
						loc.setLatitude(Double.valueOf(location.getString("lat")));
						loc.setLongitude(Double.valueOf(location.getString("lng")));
						
						venue.location	= loc;
						venue.address	= location.getString("address");
						venue.distance	= location.getInt("distance");
						venue.herenow	= item.getJSONObject("hereNow").getInt("count");
						venue.type		= group.getString("type");
						
						venueList.add(venue);
					}
				}
			}
		} catch (Exception ex) {
			throw ex;
		}
		
		return venueList;
	}
	
	private String streamToString(InputStream is) throws IOException {
		String str  = "";
		
		if (is != null) {
			StringBuilder sb = new StringBuilder();
			String line;
			
			try {
				BufferedReader reader 	= new BufferedReader(new InputStreamReader(is));
				
				while ((line = reader.readLine()) != null) {
					sb.append(line);
				}
				
				reader.close();
			} finally {
				is.close();
			}
			
			str = sb.toString();
		}
		
		return str;
	}
	
	public interface FsqAuthListener {
		public abstract void onSuccess();
		public abstract void onFail(String error);
	}
}