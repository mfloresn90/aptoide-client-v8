package cm.aptoide.pt.timeline.post;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.LayoutRes;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import cm.aptoide.pt.R;
import cm.aptoide.pt.account.view.LoginBottomSheet;
import cm.aptoide.pt.navigator.TabNavigation;
import cm.aptoide.pt.navigator.TabNavigator;
import cm.aptoide.pt.view.BackButtonActivity;
import rx.Observable;

public class PostActivity extends BackButtonActivity
    implements PostUrlProvider, TabNavigator, LoginBottomSheet {

  @Override public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(getLayoutId());
    if (savedInstanceState == null) {
      final Fragment fragment = PostFragment.newInstanceFromExternalSource();
      getFragmentNavigator().navigateToWithoutBackSave(fragment, true);
    }
  }

  @Override public void onBackPressed() {
    super.onBackPressed();
  }

  @LayoutRes private int getLayoutId() {
    return R.layout.empty_frame;
  }

  @Override protected void onNewIntent(Intent intent) {
    super.onNewIntent(intent);
    setIntent(intent);
  }

  @Override public String getUrlToShare() {
    Intent intent = getIntent();
    String toShare = null;
    if (intent != null && intent.getType() != null && intent.getType()
        .equals("text/plain")) {
      toShare = intent.getStringExtra(Intent.EXTRA_TEXT);
    }
    return toShare;
  }

  @Override public void navigate(TabNavigation tabNavigation) {

  }

  @Override public Observable<TabNavigation> navigation() {
    return null;
  }

  @Override public void clearNavigation() {

  }

  @Override public void expand() {

  }

  @Override public void collapse() {

  }

  @Override public Observable<State> state() {
    return null;
  }
}
