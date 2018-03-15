package cm.aptoide.pt.presenter;

import rx.Observable;

/**
 * Created by marcelobenites on 23/02/17.
 */
public interface SpotSharePreviewView extends View {

  Observable<Void> startSelection();

  void navigateToSpotShareView();

  void showToolbar(String title);

  void finish();
}
