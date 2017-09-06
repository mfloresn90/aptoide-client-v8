package cm.aptoide.pt.view;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.widget.ImageView;
import cm.aptoide.pt.AptoideApplication;
import cm.aptoide.pt.PartnerApplication;
import cm.aptoide.pt.R;
import cm.aptoide.pt.dataprovider.BuildConfig;
import cm.aptoide.pt.logger.Logger;
import cm.aptoide.pt.networking.image.ImageLoader;
import cm.aptoide.pt.remotebootconfig.BootConfigServices;
import cm.aptoide.pt.remotebootconfig.datamodel.BootConfig;
import cm.aptoide.pt.remotebootconfig.datamodel.RemoteBootConfig;
import cm.aptoide.pt.store.StoreTheme;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Created by diogoloureiro on 11/08/2017.
 *
 * Partners Main launch activity.
 */

public class PartnersLaunchView extends ActivityView {

  private boolean usesSplashScreen;
  private BootConfig bootConfig;
  private String partnerId;

  @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    partnerId = ((PartnerApplication) getApplicationContext()).getPartnerId();
    bootConfig = ((PartnerApplication) getApplicationContext()).getBootConfig();
    if (getSupportActionBar() != null) {
      getSupportActionBar().hide();
    }
    loadSplashScreen();

    if (savedInstanceState == null) {
      getBootConfigRequest(this);
    }
  }

  /**
   * don't do anything on back pressed - avoid that users closes the app on splash screen
   */
  @Override public void onBackPressed() {
    //nothing
  }

  /**
   * check if splash screen is enabled. If it is, loads it, according to the screen orientation
   */
  private void loadSplashScreen() {
    usesSplashScreen = ((PartnerApplication) getApplicationContext()).getBootConfig()
        .getPartner()
        .getAppearance()
        .getSplash()
        .isEnable();
    if (usesSplashScreen) {
      setContentView(R.layout.partners_launch);
      setTheme(StoreTheme.get(bootConfig.getPartner()
          .getAppearance()
          .getTheme())
          .getThemeResource());
      String url;
      if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
        url = ((PartnerApplication) getApplicationContext()).getBootConfig()
            .getPartner()
            .getAppearance()
            .getSplash()
            .getLandscape();
      } else {
        url = ((PartnerApplication) getApplicationContext()).getBootConfig()
            .getPartner()
            .getAppearance()
            .getSplash()
            .getPortrait();
      }
      ImageLoader.with(this)
          .load(url, (ImageView) findViewById(R.id.splashscreen));
    }
  }

  /**
   * gets the Remote boot config, applies it, and saves it to shared preferences
   */
  private void getBootConfigRequest(Context context) {
    Retrofit retrofit = new Retrofit.Builder().baseUrl(BuildConfig.APTOIDE_WEB_SERVICES_SCHEME
        + "://"
        + BuildConfig.APTOIDE_WEB_SERVICES_V7_HOST
        + "/api/7/client/boot/")
        .addConverterFactory(GsonConverterFactory.create())
        .client(((AptoideApplication) context.getApplicationContext()).getDefaultClient())
        .build();
    Call<RemoteBootConfig> call = retrofit.create(BootConfigServices.class)
        .getRemoteBootConfig(BuildConfig.APPLICATION_ID, bootConfig.getPartner()
            .getType(), partnerId, String.valueOf(BuildConfig.VERSION_CODE));
    call.enqueue(new Callback<RemoteBootConfig>() {
      @Override
      public void onResponse(Call<RemoteBootConfig> call, Response<RemoteBootConfig> response) {
        if (response.body() != null) {
          ((PartnerApplication) getApplicationContext()).setRemoteBootConfig(response.body());
        }
        handleSplashScreenTimer();
      }

      @Override public void onFailure(Call<RemoteBootConfig> call, Throwable t) {
        Logger.e("PartnersConfiguration", "Failed to get remote boot config: " + t.getMessage());
        handleSplashScreenTimer();
      }
    });
  }

  /**
   * show the splash screen according to the time established by the partner on the configs.
   * case there's not splash screen, automatically starts the main activity
   */
  private void handleSplashScreenTimer() {
    if (usesSplashScreen) {
      new java.util.Timer().schedule(new java.util.TimerTask() {
        @Override public void run() {
          startActivity();
        }
      }, ((PartnerApplication) getApplicationContext()).getBootConfig()
          .getPartner()
          .getAppearance()
          .getSplash()
          .getTimeout() * 1000);
    } else {
      startActivity();
    }
  }

  /**
   * start the main activity
   */
  private void startActivity() {
    Intent i = new Intent(this, MainActivity.class);
    if (getIntent().getExtras() != null) {
      i.putExtras(getIntent().getExtras());
    }
    startActivity(i);
    finish();
  }
}