package cm.aptoide.pt.dataprovider.ads;

import android.content.Context;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import cm.aptoide.pt.dataprovider.model.MinimalAdInterface;
import cm.aptoide.pt.dataprovider.model.v2.GetAdsResponse;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import java.io.IOException;
import java.util.Date;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class AdNetworkUtils {

  /**
   * Execute a simple request (knock at the door) to the given URL.
   */
  public static void knock(String url) {
    if (url == null) {
      return;
    }

    OkHttpClient client = new OkHttpClient();

    Request click = new Request.Builder().url(url)
        .build();

    client.newCall(click)
        .enqueue(new Callback() {
          @Override public void onFailure(Call call, IOException e) {

          }

          @Override public void onResponse(Call call, Response response) throws IOException {
            response.body()
                .close();
          }
        });
  }

  public static String parseMacros(@NonNull String clickUrl, String androidId,
      String uniqueIdentifier, String advertisingId) {

    if (!TextUtils.isEmpty(androidId)) {
      clickUrl = clickUrl.replace("[USER_ANDROID_ID]", androidId);
    }
    clickUrl = clickUrl.replace("[USER_UDID]", uniqueIdentifier);
    clickUrl = clickUrl.replace("[USER_AAID]", advertisingId);
    clickUrl = clickUrl.replace("[TIME_STAMP]", String.valueOf(new Date().getTime()));

    return clickUrl;
  }

  public static boolean isGooglePlayServicesAvailable(Context context) {
    return GoogleApiAvailability.getInstance()
        .isGooglePlayServicesAvailable(context) == ConnectionResult.SUCCESS;
  }

  public static void knockCpc(MinimalAdInterface minimalAd) {
    // TODO: 28-07-2016 Baikova clicked on ad.
    knock(minimalAd.getCpcUrl());
  }

  public static void knockCpd(MinimalAdInterface minimalAd) {
    // TODO: 28-07-2016 Baikova clicked on download button.
    knock(minimalAd.getCpdUrl());
  }

  public static void knockCpi(MinimalAdInterface minimalAd) {
    // TODO: 28-07-2016 Baikova ad installed.
    knock(minimalAd.getCpiUrl());
  }

  // FIXME: 29-07-2016 neuro so wrong...
  public static void knockImpression(GetAdsResponse.Ad ad) {
    if (isImpressionUrlPresent(ad)) {
      knock(ad.getPartner()
          .getData()
          .getImpressionUrl());
    }
  }

  private static boolean isImpressionUrlPresent(GetAdsResponse.Ad ad) {
    return ad != null
        && ad.getPartner() != null
        && ad.getPartner()
        .getData() != null
        && ad.getPartner()
        .getData()
        .getImpressionUrl() != null;
  }
}
