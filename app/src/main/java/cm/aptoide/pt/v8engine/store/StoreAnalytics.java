package cm.aptoide.pt.v8engine.store;

import android.os.Bundle;
import cm.aptoide.pt.analytics.Analytics;
import cm.aptoide.pt.analytics.events.FacebookEvent;
import cm.aptoide.pt.analytics.events.FlurryEvent;
import cm.aptoide.pt.utils.AptoideUtils;
import com.facebook.appevents.AppEventsLogger;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by pedroribeiro on 26/06/17.
 */

public class StoreAnalytics {

  private static final String STORES_TAB_OPEN = "Stores_Tab_Open";
  private static final String STORES_TAB_INTERACT = "Stores_Tab_Interact";
  private static final String STORES_OPEN = "Store_Open";
  private static final String STORES_INTERACT = "Store_Interact";
  private static final String ACTION = "action";
  private static final String SOURCE = "source";
  private static final String STORE_NAME = "store_name";
  private static final String FOLLOW_STORE_APPS = "follow_store_apps";
  private static final String FOLLOW_STORE_FOLLOWERS = "follow_store_followers";
  private static final String TAB = "tab_name";
  private final AppEventsLogger facebook;
  private final Analytics analytics;

  public StoreAnalytics(AppEventsLogger facebook, Analytics analytics) {
    this.facebook = facebook;
    this.analytics = analytics;
  }

  public void sendStoreTabOpenedEvent() {
    analytics.sendEvent(new FacebookEvent(facebook, STORES_TAB_OPEN));
    analytics.sendEvent(new FlurryEvent(STORES_TAB_OPEN));
  }

  /// "add store" event implemented (according to sunil the information about how many apps/subscribers only needs to be sent when comming from a "follow a recommended store" event
  public void sendStoreTabInteractEvent(String action) {
    analytics.sendEvent(
        new FacebookEvent(facebook, STORES_TAB_INTERACT, createStoreInteractBundle(action)));
    analytics.sendEvent(
        new FlurryEvent(STORES_TAB_INTERACT, createStoreTabInteractFlurryDataMap(action)));
  }

  //Only for "follow a recommended store event"
  public void sendStoreTabInteractEvent(String action, int storeAppsNumber, int storeFollowers) {
    analytics.sendEvent(new FacebookEvent(facebook, STORES_TAB_INTERACT,
        createStoreTabInteractFacebookBundle(action, storeAppsNumber, storeFollowers)));
    analytics.sendEvent(new FlurryEvent(STORES_TAB_INTERACT,
        createStoreTabInteractFlurryDataMap(action, storeAppsNumber, storeFollowers)));
  }

  public void sendStoreOpenEvent(String source, String storeName) {
    analytics.sendEvent(
        new FacebookEvent(facebook, STORES_OPEN, createStoreOpenFacebookBundle(source, storeName)));
    analytics.sendEvent(
        new FlurryEvent(STORES_OPEN, createStoreOpenFlurryDataMap(source, storeName)));
  }

  public void sendStoreInteractEvent(String action, String tab, String storeName) {
    analytics.sendEvent(new FacebookEvent(facebook, STORES_INTERACT,
        createStoreInteractFacebookBundle(action, tab, storeName)));
    analytics.sendEvent(new FlurryEvent(STORES_INTERACT,
        createFlurryStoreInteractFlurryDataMap(action, tab, storeName)));
  }

  private Map<String, String> createFlurryStoreInteractFlurryDataMap(String action, String tab,
      String storeName) {
    Map<String, String> map = new HashMap<>();
    map.put(ACTION, action);
    map.put(TAB, tab);
    map.put(STORE_NAME, storeName);
    return map;
  }

  private Bundle createStoreInteractFacebookBundle(String action, String tab, String storeName) {
    Bundle bundle = new Bundle();
    bundle.putString(ACTION, action);
    bundle.putString(TAB, tab);
    bundle.putString(STORE_NAME, storeName);
    return bundle;
  }

  private Map<String, String> createStoreTabInteractFlurryDataMap(String action) {
    Map<String, String> map = new HashMap<>();
    map.put(ACTION, action);
    return map;
  }

  private Bundle createStoreInteractBundle(String action) {
    Bundle bundle = new Bundle();
    bundle.putString(ACTION, action);
    return bundle;
  }

  private Map<String, String> createStoreOpenFlurryDataMap(String source, String storeName) {
    Map<String, String> map = new HashMap<>();
    map.put(SOURCE, source);
    map.put(STORE_NAME, storeName);
    return map;
  }

  private Bundle createStoreOpenFacebookBundle(String source, String storeName) {
    Bundle bundle = new Bundle();
    bundle.putString(SOURCE, source);
    bundle.putString(STORE_NAME, storeName);
    return bundle;
  }

  private Map<String, String> createStoreTabInteractFlurryDataMap(String action,
      int storeAppsNumber, int storeFollowers) {
    Map<String, String> map = new HashMap<>();
    map.put(ACTION, action);
    map.put(FOLLOW_STORE_APPS, AptoideUtils.StringU.toString(storeAppsNumber));
    map.put(FOLLOW_STORE_FOLLOWERS, AptoideUtils.StringU.toString(storeFollowers));
    return map;
  }

  private Bundle createStoreTabInteractFacebookBundle(String action, int storeAppsNumber,
      int storeFollowers) {
    Bundle bundle = new Bundle();
    bundle.putString(ACTION, action);
    bundle.putInt(FOLLOW_STORE_APPS, storeAppsNumber);
    bundle.putInt(FOLLOW_STORE_FOLLOWERS, storeFollowers);
    return bundle;
  }
}
