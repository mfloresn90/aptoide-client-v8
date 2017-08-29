package cm.aptoide.pt.install;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import cm.aptoide.pt.AptoideApplication;
import cm.aptoide.pt.crashreports.CrashReport;
import rx.schedulers.Schedulers;

/**
 * Created by trinkes on 07/06/2017.
 */

public class CheckRootOnBoot extends BroadcastReceiver {
  private final static String HTC_BOOT_COMPLETED = "android.intent.action.QUICKBOOT_POWERON";
  private CrashReport crashReport;

  @Override public void onReceive(Context context, Intent intent) {
    crashReport = CrashReport.getInstance();

    if (intent != null && (intent.getAction()
        .equals(Intent.ACTION_BOOT_COMPLETED) || intent.getAction()
        .equals(Intent.ACTION_REBOOT) || intent.getAction()
        .equals(HTC_BOOT_COMPLETED))) {
      ((AptoideApplication) context.getApplicationContext()).getRootAvailabilityManager()
          .updateRootAvailability()
          .subscribeOn(Schedulers.computation())
          .subscribe(() -> {
          }, throwable -> crashReport.log(throwable));
    }
  }
}