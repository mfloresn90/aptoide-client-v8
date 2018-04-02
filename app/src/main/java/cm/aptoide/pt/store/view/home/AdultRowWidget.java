package cm.aptoide.pt.store.view.home;

import android.support.v7.widget.SwitchCompat;
import android.view.View;
import cm.aptoide.accountmanager.AptoideAccountManager;
import cm.aptoide.pt.AptoideApplication;
import cm.aptoide.pt.R;
import cm.aptoide.pt.account.AdultContentAnalytics;
import cm.aptoide.pt.utils.design.ShowMessage;
import cm.aptoide.pt.view.ReloadInterface;
import cm.aptoide.pt.view.dialog.EditableTextDialog;
import cm.aptoide.pt.view.recycler.widget.Widget;
import cm.aptoide.pt.view.rx.RxAlertDialog;
import cm.aptoide.pt.view.rx.RxSwitch;
import cm.aptoide.pt.view.settings.PinDialog;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;

public class AdultRowWidget extends Widget<AdultRowDisplayable> {

  private SwitchCompat adultSwitch;
  private SwitchCompat adultPinSwitch;
  private boolean trackAnalytics = true;
  private AptoideAccountManager accountManager;
  private RxAlertDialog adultContentConfirmationDialog;
  private EditableTextDialog enableAdultContentPinDialog;
  private boolean ignoreCheck;
  private boolean ignorePinCheck;
  private AdultContentAnalytics adultContentAnalytics;

  public AdultRowWidget(View itemView) {
    super(itemView);
  }

  @Override protected void assignViews(View itemView) {
    adultSwitch = (SwitchCompat) itemView.findViewById(R.id.adult_content);
    adultPinSwitch = (SwitchCompat) itemView.findViewById(R.id.pin_adult_content);
    adultContentAnalytics =
        ((AptoideApplication) getContext().getApplicationContext()).getAdultContentAnalytics();
    adultContentConfirmationDialog =
        new RxAlertDialog.Builder(getContext()).setMessage(R.string.are_you_adult)
            .setPositiveButton(R.string.yes)
            .setNegativeButton(R.string.no)
            .build();

    enableAdultContentPinDialog =
        new PinDialog.Builder(getContext()).setMessage(R.string.request_adult_pin)
            .setPositiveButton(R.string.all_button_ok)
            .setNegativeButton(R.string.cancel)
            .setView(R.layout.dialog_requestpin)
            .setEditText(R.id.pininput)
            .build();

    trackAnalytics = true;
    accountManager =
        ((AptoideApplication) getContext().getApplicationContext()).getAccountManager();
  }

  @Override public void bindView(final AdultRowDisplayable displayable) {
    compositeSubscription.add(accountManager.pinRequired()
        .observeOn(AndroidSchedulers.mainThread())
        .doOnNext(pinRequired -> {
          if (pinRequired) {
            adultPinSwitch.setVisibility(View.VISIBLE);
            adultSwitch.setVisibility(View.GONE);
          } else {
            adultSwitch.setVisibility(View.VISIBLE);
            adultPinSwitch.setVisibility(View.GONE);
          }
        })
        .subscribe());

    compositeSubscription.add(RxSwitch.checks(adultSwitch)
        .filter(check -> shouldCheck())
        .flatMap(checked -> {
          ignoreCheck = false;
          rollbackCheck(adultSwitch);
          if (checked) {
            adultContentConfirmationDialog.show();
            return Observable.empty();
          } else {
            adultSwitch.setEnabled(false);
            return accountManager.disable()
                .doOnCompleted(this::trackLock)
                .observeOn(AndroidSchedulers.mainThread())
                .doOnTerminate(() -> adultSwitch.setEnabled(true))
                .doOnTerminate(() -> reload(displayable))
                .toObservable();
          }
        })
        .retry()
        .subscribe());

    compositeSubscription.add(RxSwitch.checks(adultPinSwitch)
        .filter(check -> shouldPinCheck())
        .flatMap(checked -> {
          rollbackCheck(adultPinSwitch);
          if (checked) {
            enableAdultContentPinDialog.show();
            return Observable.empty();
          } else {
            adultPinSwitch.setEnabled(false);
            return accountManager.disable()
                .doOnCompleted(this::trackLock)
                .observeOn(AndroidSchedulers.mainThread())
                .doOnTerminate(() -> adultPinSwitch.setEnabled(true))
                .doOnTerminate(() -> reload(displayable))
                .toObservable();
          }
        })
        .retry()
        .subscribe());

    compositeSubscription.add(adultContentConfirmationDialog.positiveClicks()
        .doOnNext(click -> adultSwitch.setEnabled(false))
        .flatMap(click -> accountManager.enable()
            .doOnCompleted(this::trackUnlock)
            .observeOn(AndroidSchedulers.mainThread())
            .doOnTerminate(() -> adultSwitch.setEnabled(true))
            .doOnTerminate(() -> reload(displayable))
            .toObservable())
        .retry()
        .subscribe());

    compositeSubscription.add(enableAdultContentPinDialog.positiveClicks()
        .doOnNext(clock -> adultPinSwitch.setEnabled(false))
        .flatMap(pin -> accountManager.enable(Integer.valueOf(pin.toString()))
            .doOnCompleted(this::trackUnlock)
            .observeOn(AndroidSchedulers.mainThread())
            .doOnError(throwable -> {
              if (throwable instanceof SecurityException) {
                ShowMessage.asSnack(getContext(), R.string.adult_pin_wrong);
              }
            })
            .doOnTerminate(() -> adultPinSwitch.setEnabled(true))
            .doOnTerminate(() -> reload(displayable))
            .toObservable())
        .retry()
        .subscribe());

    compositeSubscription.add(accountManager.enabled()
        .observeOn(AndroidSchedulers.mainThread())
        .doOnNext(enabled -> {

          if (enabled != adultSwitch.isChecked()) {
            ignoreCheck = true;
            adultSwitch.setChecked(enabled);
          }

          if (enabled != adultPinSwitch.isChecked()) {
            ignorePinCheck = true;
            adultPinSwitch.setChecked(enabled);
          }
        })
        .subscribe());
  }

  private boolean shouldCheck() {
    if (!ignoreCheck) {
      return true;
    }
    ignoreCheck = false;
    return false;
  }

  private boolean shouldPinCheck() {
    if (!ignorePinCheck) {
      return true;
    }
    ignorePinCheck = false;
    return false;
  }

  private void trackLock() {
    if (trackAnalytics) {
      trackAnalytics = false;
      adultContentAnalytics.lock();
    }
  }

  private void trackUnlock() {
    if (trackAnalytics) {
      trackAnalytics = false;
      adultContentAnalytics.unlock();
    }
  }

  private void reload(ReloadInterface reloader) {
    reloader.load(true, true, null);
  }

  private void rollbackCheck(SwitchCompat adultSwitch) {
    adultSwitch.setChecked(!adultSwitch.isChecked());
  }
}
