package org.immopoly.android.fragments;

import java.io.IOException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import oauth.signpost.OAuth;
import oauth.signpost.exception.OAuthCommunicationException;
import oauth.signpost.exception.OAuthExpectationFailedException;
import oauth.signpost.exception.OAuthMessageSignerException;
import oauth.signpost.exception.OAuthNotAuthorizedException;

import org.immopoly.android.R;
import org.immopoly.android.api.ApiResultReciever.Receiver;
import org.immopoly.android.api.IS24ApiService;
import org.immopoly.android.api.ReceiverState;
import org.immopoly.android.app.DashboardActivity;
import org.immopoly.android.app.ImmopolyActivity;
import org.immopoly.android.app.UserSignupActivity;
import org.immopoly.android.app.WebViewActivity;
import org.immopoly.android.constants.Const;
import org.immopoly.android.helper.HudPopupHelper;
import org.immopoly.android.helper.LocationHelper;
import org.immopoly.android.helper.MapLocationCallback;
import org.immopoly.android.helper.MapMarkerCallback;
import org.immopoly.android.helper.Settings;
import org.immopoly.android.helper.TrackingManager;
import org.immopoly.android.helper.WebHelper;
import org.immopoly.android.model.Flat;
import org.immopoly.android.model.Flats;
import org.immopoly.android.model.ImmopolyUser;
import org.immopoly.android.model.OAuthData;
import org.immopoly.android.provider.FlatsProvider;
import org.immopoly.android.tasks.GetUserInfoTask;
import org.immopoly.android.widget.ImmoscoutPlacesOverlay;
import org.immopoly.android.widget.MyPositionOverlay;
import org.immopoly.android.widget.PlaceOverlayItem;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.location.Address;
import android.location.Geocoder;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;
import android.widget.ZoomControls;

import com.google.android.apps.analytics.GoogleAnalyticsTracker;
import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;

public class MapFragment extends Fragment implements Receiver,
		MapMarkerCallback, MapLocationCallback, OnMapItemClickedListener {

	public static final String TAG = "Immopoly";

	private MapController mMapController;
	private List<Overlay> mMapOverlays;
	private PlaceOverlayItem myLocationOverlayItem;
	private MapView mMapView;
	private ImmoscoutPlacesOverlay overlays;
	private Flats mFlats;

	public static final int RELOAD_DELAY_MILLIS = 800;
	ReceiverState mState;

	private MyPositionOverlay myLocationOverlays;
	private int mNumberGeoCodeTry;
	private HudPopupHelper mHudPopup;

	private GoogleAnalyticsTracker tracker;

	private Button hudText;
	private ImageButton mapButton;

	private OnMapItemClickedListener mOnMapItemClickedListener;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,Bundle savedInstanceState) {
		mMapView = ((ImmopolyActivity) getActivity()).acquireMapView( this );
		mMapView.setBuiltInZoomControls(true);
		mMapController = mMapView.getController();
		tracker = GoogleAnalyticsTracker.getInstance();
		// Start the tracker in manual dispatch mode...
		tracker.startNewSession(TrackingManager.UA_ACCOUNT, Const.ANALYTICS_INTERVAL, getActivity()
							.getApplicationContext());

		// mState = (ReceiverState) getActivity()
		// .getLastNonConfigurationInstance();
		if (mState != null) {
			// Start listening for Service updates again
			mState.mReceiver.setReceiver(this);
		} else {
			mState = new ReceiverState();
			mState.mReceiver.setReceiver(this);
		}
		return mMapView;
	}
	
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		try {
			mOnMapItemClickedListener = (OnMapItemClickedListener) activity;
		} catch (ClassCastException e) {
			throw new ClassCastException(activity.toString() + " must implement OnMapItemClickedListener");
		}
	}

	public void onDestroyView() {
		((ImmopolyActivity) getActivity()).releaseMapView( this );
		Log.i( "IMPO", "MapFragment.onDestroyView" );
		mMapView = null;
		super.onDestroyView();
	}
	
	public OnMapItemClickedListener getOnMapItemClickedListener() {
		return mOnMapItemClickedListener;
	}

//	@Override
//	public void onHudAction(View view) {
//		switch (view.getId()) {
//		case R.id.hud_map:
//			// hud map
//			// already there
//			LocationHelper.getLastLocation(getActivity());
//			break;
//		case R.id.hud_portfolio:
//			startActivity(new Intent(getActivity(), DashboardActivity.class));
//			break;
//		case R.id.hud_profile:
//			startActivity(new Intent(getActivity(), DashboardActivity.class));
//			break;
//		case R.id.hud_text:
//			// Toast.makeText(this, ImmopolyUser.getInstance().flats.toString(),
//			// Toast.LENGTH_LONG);
//			if (mHudPopup != null) {
//				mHudPopup.show(getActivity().findViewById(R.id.hud_text), -200, -60);
//			}
//			break;
//		default:
//			break;
//		}
//	}

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		setRetainInstance(true);
		// signIn();
	}

	@Override
	public void onActivityCreated(Bundle arg0) {
		super.onActivityCreated(arg0);

		GeoPoint point = new GeoPoint((int) (LocationHelper.sLat * 1E6), (int) (LocationHelper.sLng * 1E6));
		mMapOverlays = mMapView.getOverlays();
		LocationHelper.callback = this;
		LocationHelper.getLastLocation(getActivity());
		// this is the bounding box container

		myLocationOverlayItem = new PlaceOverlayItem(point, "my city", "This is wher you are");
		overlays = new ImmoscoutPlacesOverlay(this, mMapView, getActivity().getLayoutInflater());
		myLocationOverlays = new MyPositionOverlay(getResources().getDrawable(R.drawable.house_icon), getActivity(),
				mMapView, getLayoutInflater());
		myLocationOverlayItem.setMarker(this.getResources().getDrawable(R.drawable.house_icon));

		myLocationOverlays.addOverlay(myLocationOverlayItem);
		mMapOverlays.add(myLocationOverlays);
		// setMapViewWithZoom(R.id.mapview, R.id.map_zoom_controls);
		mMapView.setBuiltInZoomControls(true);
		mMapView.invalidate();

		// maybe do this in your init or something
		final GestureDetector gDetector = new GestureDetector(new MyDetector());
		mMapView.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {

				return gDetector.onTouchEvent(event);

			}

		});
	}

	@Override
	public void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
		if (mHudPopup != null) {
			mHudPopup.dismiss();
		}
	}

	@Override
	public void onStart() {
		super.onStart();
		LocationHelper.callback = this;
		tracker.trackPageView(TrackingManager.VIEW_MAP);
		updateHud(null, 0);
	}

	@Override
	public void onStop() {
		super.onStop();
	}

	public MapView getMapView() {
		return mMapView;
	}

	public ViewGroup getContentView() {
		return mMapView;
	}

	public void setMapViewWithZoom(int mapLayoutId, int zoomControlsLayoutId) {

		ZoomControls zoomControls = (ZoomControls) mMapView.findViewById(zoomControlsLayoutId);
		zoomControls.setOnZoomInClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				getMapView().getController().zoomInFixing(getMapView().getWidth() / 2, getMapView().getHeight() / 2);
			}
		});
		zoomControls.setOnZoomOutClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				getMapView().getController().zoomOut();
			}
		});
	}

	@Override
	public void onReceiveResult(int resultCode, Bundle resultData) {
		switch (resultCode) {
		case IS24ApiService.STATUS_RUNNING:
			// show progress
			// Toast.makeText(this, "Running", Toast.LENGTH_SHORT).show();
			break;
		case IS24ApiService.STATUS_FINISHED:
			// Toast.makeText(this, "Finished", Toast.LENGTH_SHORT).show();
			ArrayList<Flat> a = resultData.getParcelableArrayList("flats");
			mFlats = (Flats) a;
			updateMap(true);
			// do something interesting
			// hide progress
			break;
		case IS24ApiService.STATUS_ERROR:
			// Toast.makeText(this, "Error", Toast.LENGTH_SHORT).show();
			// handle the error;
			break;
		}
	}

	public void updateMap(boolean centerMap) {
		if ( mMapView != null && mFlats != null) {
			int count = 0;
			double minX = 999, minY = 999, maxX = -999, maxY = -999;
			myLocationOverlays.clear();
			mMapOverlays.clear();

			GeoPoint point = new GeoPoint((int) (LocationHelper.sLat * 1E6), (int) (LocationHelper.sLng * 1E6));

			myLocationOverlayItem = new PlaceOverlayItem(point, "my city", "THis is wher you are");
			myLocationOverlayItem.setMarker(this.getResources().getDrawable(R.drawable.house_icon));

			myLocationOverlays.addOverlay(myLocationOverlayItem);

			mMapOverlays.add(myLocationOverlays);

			Cursor cur;
			Log.d(this.getClass().getName(), "Flats :" + mFlats.size());
			for (Flat f : mFlats) {
				if (f.lat != 0.0 || f.lng != 0.0) {
					cur = getActivity().getContentResolver().query(FlatsProvider.CONTENT_URI, null,
							FlatsProvider.Flat.FLAT_ID + "=" + f.uid, null, null);
					f.owned = cur.getCount() == 1;
					cur.close();

					if (f.lng < minX)
						minX = f.lng;
					if (f.lng > maxX)
						maxX = f.lng;

					if (f.lat < minY)
						minY = f.lat;
					if (f.lat > maxY)
						maxY = f.lat;

					count++;
				}
			}
			overlays.setFlats(mFlats);

			if (LocationHelper.sLng < minX)
				minX = LocationHelper.sLng;
			if (LocationHelper.sLng > maxX)
				maxX = LocationHelper.sLng;

			if (LocationHelper.sLat < minY)
				minY = LocationHelper.sLat;
			if (LocationHelper.sLat > maxY)
				maxY = LocationHelper.sLat;

			int mLon = (int) Math.round((minX + (maxX - minX) / 2.0) * 1000000);
			int mLat = (int) Math.round((minY + (maxY - minY) / 2.0) * 1000000);
			if (centerMap) {
				mMapController.setCenter(new GeoPoint(mLat, mLon));
			}
			if (count > 0) {
				mMapOverlays.add(overlays);
				// TODO etwas weiter rauszoomen
				int spanLon = (int) Math.round(((maxX - minX) / 2.0) * 1000000);
				int spanLat = (int) Math.round(((maxY - minY) / 2.0) * 1000000);
				mMapController.zoomToSpan(spanLat, spanLon);

			}

			mMapView.invalidate();
		}

	}

	public void signIn() {
		String authUrl = "";
		SharedPreferences shared = getActivity().getSharedPreferences("oauth", 0);
		String accessToken = shared.getString("oauth_token", "");
		if (accessToken.length() != accessToken.length()) {
			OAuthData.getInstance(getActivity().getBaseContext()).signedIn = true;
			OAuthData.getInstance(getActivity().getBaseContext()).accessToken = accessToken;

		} else {
			OAuthData.getInstance(getActivity().getBaseContext()).signedIn = false;
			try {
				authUrl = OAuthData.getInstance(getActivity().getBaseContext()).provider.retrieveRequestToken(
						OAuthData.getInstance(getActivity().getBaseContext()).consumer, OAuth.OUT_OF_BAND);
				Log.d("OAUTH", authUrl);
			} catch (OAuthMessageSignerException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (OAuthNotAuthorizedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (OAuthExpectationFailedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (OAuthCommunicationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			// Login in web view
			Intent i = new Intent(getActivity(), WebViewActivity.class);
			i.putExtra(Const.AUTH_URL, authUrl);
			i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			startActivity(i);
		}

	}

	public void headerClick(View v) {
		switch (v.getId()) {
		case R.id.location_refresh:
			LocationHelper.getLastLocation(getContext());
			// mRefreshButton.startAnimation(AnimationUtils.loadAnimation(this,
			// R.anim.locating_animation));
			break;
		case R.id.header_logo:
			startActivity(new Intent(getActivity(), DashboardActivity.class));
			break;
		}

	}

	private class GeoCodeLocationTask extends AsyncTask<Double, Void, String> {

		@Override
		protected String doInBackground(Double... params) {
			String result = null;
			LocationHelper.mAddress = null;
			if ((params[0] != null && params[1] != null) && (params[0] != 0.0 && params[1] != 0.0)) {
				try {
					Geocoder geocoder = new Geocoder(getActivity(), Locale.getDefault());
					List<Address> addresses = geocoder.getFromLocation(params[0], params[1], 1);
					if (addresses != null && addresses.size() > 0) {
						Address address = addresses.get(0);
						// sending back first mAddress line and locality
						result = address.getAddressLine(0) + ", " + address.getLocality();
						LocationHelper.mAddress = result;
						// ((TextView)findViewById(R.id.header_location)).setText(result);
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			} else {

				result = null;
			}
			return result;
		}

		@Override
		protected void onPostExecute(String result) {
			setAddress(result);
		}
	}

	private void setAddress(String address) {
		if (address != null && address.length() > 0) {
			mNumberGeoCodeTry = 0;
			// ((TextView) findViewById(R.id.header_location)).setText(address);
		} else if (LocationHelper.sAccuracy >= 0) {
			mNumberGeoCodeTry++;
			if (mNumberGeoCodeTry < 3) {
				new GeoCodeLocationTask().execute(LocationHelper.sLat, LocationHelper.sLng);
			} else {
				NumberFormat nFormat = NumberFormat.getInstance(Locale.GERMANY);
				nFormat.setMinimumIntegerDigits(2);
				nFormat.setMaximumFractionDigits(2);
				/*
				 * ((TextView)
				 * findViewById(R.id.header_location)).setText("lat:" +
				 * nFormat.format(LocationHelper.sLat) + " - lng:" +
				 * nFormat.format(LocationHelper.sLng) + " ~" +
				 * nFormat.format(LocationHelper.sAccuracy));
				 */
			}
		} else {
			// ((TextView) findViewById(R.id.header_location))
			// .setText(R.string.no_location_value);
		}
	}

	class MyDetector extends SimpleOnGestureListener {
		@Override
		public boolean onDoubleTap(MotionEvent event) {
			mMapView.getController().zoomInFixing((int) event.getX(), (int) event.getY());
			return super.onDoubleTap(event);
		}

		@Override
		public boolean onSingleTapConfirmed(MotionEvent e) {
			return super.onSingleTapConfirmed(e);
		}
	}

	private class GetUserInfoUpdateTask extends GetUserInfoTask {

		public GetUserInfoUpdateTask(Context context) {
			super(context);
		}

		@Override
		protected void onPostExecute(ImmopolyUser result) {
			if (result != null && ImmopolyUser.getInstance().flats != null) {
				updateMap(false);
			} else if (Settings.isOnline(getActivity())) {
				Intent intent = new Intent(getActivity(), UserSignupActivity.class);
				startActivity(intent);
			} else {
				Toast.makeText(getActivity(), R.string.no_internet_connection, Toast.LENGTH_LONG).show();
			}
			updateHud(null, 0);
		}
	}

	@Override
	public void updateMapData(boolean newLoc) {
		// mRefreshButton.clearAnimation();
		if (newLoc || LocationHelper.mAddress == null) {
			new GeoCodeLocationTask().execute(LocationHelper.sLat, LocationHelper.sLng);
		} else {
			setAddress(LocationHelper.mAddress);
		}
		Intent i = new Intent(getActivity(), IS24ApiService.class);
		i.putExtra(IS24ApiService.COMMAND, IS24ApiService.CMD_SEARCH);
		i.putExtra(IS24ApiService.LAT, LocationHelper.sLat);
		i.putExtra(IS24ApiService.LNG, LocationHelper.sLng);
		i.putExtra(IS24ApiService.API_RECEIVER, mState.mReceiver);
		getActivity().startService(i);
		updateHud(null, 0);
	}

	public void showInfo(View v) {

		LayoutInflater inflater = LayoutInflater.from(getActivity());

		View alertDialogView = inflater.inflate(R.layout.info_webview, null);

		WebView myWebView = (WebView) alertDialogView.findViewById(R.id.DialogWebView);
		myWebView.setWebViewClient(new WebViewClient());
		myWebView.getSettings().setSupportZoom(true);
		myWebView.getSettings().setUseWideViewPort(true);

		myWebView.loadUrl(WebHelper.SERVER_URL_PREFIX);
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setView(alertDialogView);
		builder.setPositiveButton(R.string.button_ok, new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.cancel();
					}
				}).show();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		tracker.stopSession();
	}

	public void updateHud(Intent data, int element) {
		if (mapButton != null) {
			mapButton.setSelected(true);
		}
		if (hudText != null) {
			NumberFormat nFormat = NumberFormat.getCurrencyInstance(Locale.GERMANY);
			nFormat.setMinimumIntegerDigits(1);
			nFormat.setMaximumFractionDigits(2);
			hudText.setText(nFormat.format(ImmopolyUser.getInstance().getBalance()));
		}
	}

	@Override
	public LayoutInflater getLayoutInflater() {
		return getActivity().getLayoutInflater();
	}

	@Override
	public void startActivity(Intent arg0) {
		getActivity().sendBroadcast(arg0);
	}

	@Override
	public void onMapItemClicked(int exposeID, boolean isInPortfolio) {
		mOnMapItemClickedListener.onMapItemClicked(exposeID, isInPortfolio);
	}
	
	@Override
	public void onMapOverlayClicked(int exposeID, boolean isInPortfolio) {
		mOnMapItemClickedListener.onMapOverlayClicked(exposeID, isInPortfolio);
	}
	
	@Override
	public Context getContext() {
		return getActivity();
	}
}
