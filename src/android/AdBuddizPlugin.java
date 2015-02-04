package com.flyingsoftgames.adbuddizplugin;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;

import com.purplebrain.adbuddiz.sdk.AdBuddiz;
import com.purplebrain.adbuddiz.sdk.AdBuddizDelegate;
import com.purplebrain.adbuddiz.sdk.AdBuddizError;

import org.apache.cordova.PluginResult;
import org.apache.cordova.PluginResult.Status;

import android.util.Log;

public class AdBuddizPlugin extends CordovaPlugin {
 // Common tag used for logging statements.
 private static final String LOGTAG = "AdBuddizPlugin";
 
 // Cordova Actions.
 private static final String ACTION_SET_OPTIONS              = "setOptions";
 private static final String ACTION_CREATE_INTERSTITIAL_VIEW = "createInterstitialView";
 private static final String ACTION_REQUEST_INTERSTITIAL_AD  = "requestInterstitialAd";
 private static final String ACTION_SHOW_INTERSTITIAL_AD     = "showInterstitialAd";
 
 // Options.
 private static final String OPT_AD_ID      = "adId";
 private static final String OPT_IS_TESTING = "isTesting";
 
 private String adId       = "";
 private boolean isTesting = false;
 
 @Override public boolean execute (String action, JSONArray inputs, CallbackContext callbackContext) throws JSONException {
  PluginResult result = null;
  if (ACTION_SET_OPTIONS.equals(action)) {
   JSONObject options = inputs.optJSONObject(0);
   result = executeSetOptions(options, callbackContext);
  } else if (ACTION_CREATE_INTERSTITIAL_VIEW.equals(action)) {
   JSONObject options = inputs.optJSONObject(0);
   result = executeCreateInterstitialView(options, callbackContext);
  } else if (ACTION_REQUEST_INTERSTITIAL_AD.equals(action)) {
   JSONObject options = inputs.optJSONObject(0);
   result = executeRequestInterstitialAd(options, callbackContext);
  } else if (ACTION_SHOW_INTERSTITIAL_AD.equals(action)) {
   boolean show = inputs.optBoolean(0);
   result = executeShowInterstitialAd(show, callbackContext);
  } else {
   Log.d (LOGTAG, String.format("Invalid action passed: %s", action));
   result = new PluginResult(Status.INVALID_ACTION);
  }
  if (result != null) callbackContext.sendPluginResult (result);
  return true;
 }
 
 private PluginResult executeSetOptions (JSONObject options, CallbackContext callbackContext) {
  this.setOptions (options);
  callbackContext.success ();
  return null;
 }
 
 private void setOptions (JSONObject options) {
  if (options == null) return;
  if (options.has(OPT_AD_ID))      this.adId      = options.optString  (OPT_AD_ID);
  if (options.has(OPT_IS_TESTING)) this.isTesting = options.optBoolean (OPT_IS_TESTING);
  if (isTesting) AdBuddiz.setTestModeActive ();
 }
 
 private PluginResult executeCreateInterstitialView (JSONObject options, CallbackContext callbackContext) {
  this.setOptions (options);
  final CallbackContext delayCallback = callbackContext;
  cordova.getActivity().runOnUiThread (new Runnable(){
   @Override public void run () {
    AdBuddiz.setDelegate (new AdListener ());
    AdBuddiz.setPublisherKey (adId);
    AdBuddiz.cacheAds (cordova.getActivity());
    testIsReadyToShowAd ();
    delayCallback.success ();
   }
  });
  return null;
 }
 
 // Keep checking if an ad is ready to show. If it is, run the onReceiveInterstitialAd event.
 private void testIsReadyToShowAd () {
  if (AdBuddiz.isReadyToShowAd(cordova.getActivity())) {webView.loadUrl ("javascript:cordova.fireDocumentEvent('onReceiveInterstitialAd', {'ad_network': 'adbuddiz'});"); return;}
  cordova.getActivity().getWindow().getDecorView().postDelayed(new Runnable () {
   @Override public void run () {testIsReadyToShowAd ();}
  }, 250);
 }
 
 private PluginResult executeRequestInterstitialAd (JSONObject options, CallbackContext callbackContext) {
  this.setOptions (options);
  final CallbackContext delayCallback = callbackContext;
  cordova.getActivity().runOnUiThread (new Runnable() {
   @Override public void run() {
    delayCallback.success ();
   }
  });
  return null;
 }
 
 private PluginResult executeShowInterstitialAd (final boolean show, final CallbackContext callbackContext) {
  cordova.getActivity().runOnUiThread (new Runnable () {
   @Override public void run () {
    if (AdBuddiz.isReadyToShowAd(cordova.getActivity())) AdBuddiz.showAd (cordova.getActivity());
    if (callbackContext != null) callbackContext.success ();
   }
  });
  return null;
 }
 
 // This class implements the AdBuddizPlugin ad listener events.  It forwards the events to the JavaScript layer.
 class AdListener implements AdBuddizDelegate {
  @Override public void didFailToShowAd (AdBuddizError errorCode) {webView.loadUrl (String.format("javascript:cordova.fireDocumentEvent('onFailedToReceiveAd', {'ad_network': 'adbuddiz', 'error': '%s'});", getErrorReason(errorCode)));}
  @Override public void didCacheAd      ()                        {}
  @Override public void didShowAd       ()                        {webView.loadUrl ("javascript:cordova.fireDocumentEvent('onPresentInterstitialAd', {'ad_network': 'adbuddiz'});");}
  @Override public void didHideAd       ()                        {webView.loadUrl ("javascript:cordova.fireDocumentEvent('onDismissInterstitialAd', {'ad_network': 'adbuddiz'});");}
  @Override public void didClick        ()                        {webView.loadUrl ("javascript:cordova.fireDocumentEvent('onLeaveToInterstitialAd', {'ad_network': 'adbuddiz'});");}
 }
 
 // Gets a string error reason from an error code.
 public String getErrorReason (AdBuddizError errorCode) {
  String errorReason = "";
  switch (errorCode) {
   case NO_MORE_AVAILABLE_ADS : errorReason = "No more ads!"          ; break;
   case CONFIG_NOT_READY      : errorReason = "Config not ready."     ; break;
   case MISSING_PUBLISHER_KEY : errorReason = "Missing publisher key."; break;
   case INVALID_PUBLISHER_KEY : errorReason = "Invalid publisher key."; break;
  }
  return errorReason;
 }
}
