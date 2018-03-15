package cm.aptoide.pt.view;

import android.content.Context;
import cm.aptoide.pt.view.fragment.NavigationTrackFragment;

/**
 * Created by marcelobenites on 11/04/17.
 */

public abstract class BackButtonFragment extends NavigationTrackFragment implements BackButton {

  private BackButton backButton;

  @Override public void onAttach(Context context) {
    super.onAttach(context);
    if (context instanceof BackButton) {
      backButton = (BackButton) context;
    } else {
      throw new IllegalStateException("Context must implement " + BackButton.class.getSimpleName());
    }
  }

  @Override public void registerClickHandler(ClickHandler clickHandler) {
    backButton.registerClickHandler(clickHandler);
  }

  @Override public void unregisterClickHandler(ClickHandler clickHandler) {
    backButton.unregisterClickHandler(clickHandler);
  }
}