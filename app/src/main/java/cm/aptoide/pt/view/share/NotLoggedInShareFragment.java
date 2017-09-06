package cm.aptoide.pt.view.share;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import cm.aptoide.accountmanager.AptoideAccountManager;
import cm.aptoide.pt.AptoideApplication;
import cm.aptoide.pt.R;
import cm.aptoide.pt.crashreports.CrashReport;
import cm.aptoide.pt.dataprovider.model.v7.GetAppMeta;
import cm.aptoide.pt.logger.Logger;
import cm.aptoide.pt.networking.image.ImageLoader;
import cm.aptoide.pt.presenter.Presenter;
import cm.aptoide.pt.view.account.GoogleLoginFragment;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.jakewharton.rxbinding.view.RxView;
import java.util.Arrays;
import rx.Observable;

/**
 * Created by pedroribeiro on 29/08/17.
 */

public class NotLoggedInShareFragment extends GoogleLoginFragment implements NotLoggedInShareView {

  private static final String TAG = NotLoggedInShareFragment.class.getSimpleName();
  private static final String APP_NAME = "app_name";
  private static final String APP_ICON = "app_title";
  private static final String APP_RATING = "app_rating";
  private AptoideAccountManager accountManager;
  private SharedPreferences defaultSharedPreferences;
  private Presenter presenter;
  private LoginManager facebookLoginManager;
  private CallbackManager callbackManager;
  private Button facebookButton;
  private Button googleButton;
  private RatingBar appRating;
  private TextView appTitle;
  private ImageView appIcon;
  private TextView closeText;
  private TextView dontShowAgain;

  public Fragment newInstance(GetAppMeta.App app) {
    Fragment fragment = new NotLoggedInShareFragment();
    Bundle bundle = new Bundle();
    bundle.putString(APP_NAME, app.getName());
    bundle.putString(APP_ICON, app.getIcon());
    bundle.putFloat(APP_RATING, app.getStats()
        .getRating()
        .getAvg());
    fragment.setArguments(bundle);
    return fragment;
  }

  @Override public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    presenter = new NotLoggedInSharePresenter(this,
        ((AptoideApplication) getContext().getApplicationContext()).getAccountManager(),
        ((AptoideApplication) getContext().getApplicationContext()).getDefaultSharedPreferences(),
        CrashReport.getInstance());
  }

  @Nullable @Override
  public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    return inflater.inflate(R.layout.not_logged_in_share, null, false);
  }

  private void bindViews(View view) {
    facebookButton = (Button) view.findViewById(R.id.not_logged_in_share_facebook_button);
    googleButton = (Button) view.findViewById(R.id.not_logged_in_share_google_button);
    appIcon = (ImageView) view.findViewById(R.id.not_logged_in_app_icon);
    appTitle = (TextView) view.findViewById(R.id.not_logged_int_app_title);
    closeText = (TextView) view.findViewById(R.id.not_logged_in_close);
    dontShowAgain = (TextView) view.findViewById(R.id.not_logged_in_dont_show_again);
    appRating = (RatingBar) view.findViewById(R.id.not_logged_in_app_rating);

    appTitle.setText(getArguments().getString(APP_NAME));
    appRating.setRating(getArguments().getFloat(APP_RATING));
    ImageLoader.with(getContext())
        .load(getArguments().getString(APP_ICON), appIcon);
  }

  @Override protected Button getGoogleButton() {
    return googleButton;
  }

  @Override public void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    callbackManager.onActivityResult(requestCode, resultCode, data);
  }

  @Override public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    bindViews(view);
    presenter.present();
  }

  @Override protected void showGoogleLoginError() {
    Snackbar.make(getActivity().findViewById(android.R.id.content), R.string.google_login_cancelled,
        Snackbar.LENGTH_LONG)
        .show();
  }

  @Override public Observable<Void> facebookButtonClick() {
    return RxView.clicks(facebookButton);
  }

  @Override public void facebookLogin() {
    LoginManager.getInstance()
        .logInWithReadPermissions(getActivity(), Arrays.asList("public_profile", "user_friends"));
  }

  @Override public void facebookInit() {
    FacebookSdk.sdkInitialize(getContext().getApplicationContext());
    callbackManager = CallbackManager.Factory.create();
    LoginManager.getInstance()
        .registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
          @Override public void onSuccess(LoginResult loginResult) {
            Logger.d(TAG, "Facebook login Successful");
          }

          @Override public void onCancel() {
            Logger.d(TAG, "Facebook login Canceled");
          }

          @Override public void onError(FacebookException error) {
            Logger.d(TAG, "Facebook login Failed");
          }
        });
  }

  @Override public Observable<Void> closeClick() {
    return RxView.clicks(closeText);
  }

  @Override public void closeFragment() {
    getFragmentNavigator().popBackStack();
  }

  @Override public Observable<Void> dontShowAgainClick() {
    return RxView.clicks(dontShowAgain);
  }
}