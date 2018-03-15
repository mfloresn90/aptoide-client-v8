package cm.aptoide.pt.spotandshare;

import android.net.Network;
import android.net.wifi.WifiConfiguration;

/**
 * Singleton without controller.
 * <p>Holds references to main stuff used in our webview app
 * </p>
 * Created by FilipeGonçalves on 10-08-2015.
 */
public class DataHolder {
  private static DataHolder holder;
  public Network network;
  private WifiConfiguration wcOnJoin;

  public static DataHolder getInstance() {
    if (holder == null) {
      holder = new DataHolder();
    }

    return holder;
  }

  public static void reset() {
    holder = null;
  }

  public WifiConfiguration getWcOnJoin() {
    return wcOnJoin;
  }

  public void setWcOnJoin(WifiConfiguration wcOnJoin) {
    this.wcOnJoin = wcOnJoin;
  }
}
