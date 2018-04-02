package cm.aptoide.pt.search.view;

import android.support.annotation.NonNull;
import android.util.Pair;
import cm.aptoide.pt.R;
import cm.aptoide.pt.crashreports.CrashReport;
import cm.aptoide.pt.presenter.Presenter;
import cm.aptoide.pt.presenter.View;
import cm.aptoide.pt.search.SearchManager;
import cm.aptoide.pt.search.SearchNavigator;
import cm.aptoide.pt.search.analytics.SearchAnalytics;
import cm.aptoide.pt.search.model.SearchAdResult;
import cm.aptoide.pt.search.model.SearchAppResult;
import com.jakewharton.rxrelay.PublishRelay;
import java.util.List;
import rx.Observable;
import rx.Scheduler;
import rx.Single;

public class SearchResultPresenter implements Presenter {

  private final SearchResultView view;
  private final SearchAnalytics analytics;
  private final SearchNavigator navigator;
  private final CrashReport crashReport;
  private final Scheduler viewScheduler;
  private final SearchManager searchManager;
  private final PublishRelay<SearchAdResult> onAdClickRelay;
  private final PublishRelay<SearchAppResult> onItemViewClickRelay;
  private final PublishRelay<Pair<SearchAppResult, android.view.View>> onOpenPopupMenuClickRelay;
  private boolean isMultiStoreSearch;
  private String defaultStoreName;
  private String defaultThemeName;

  public SearchResultPresenter(SearchResultView view, SearchAnalytics analytics,
      SearchNavigator navigator, CrashReport crashReport, Scheduler viewScheduler,
      SearchManager searchManager, PublishRelay<SearchAdResult> onAdClickRelay,
      PublishRelay<SearchAppResult> onItemViewClickRelay,
      PublishRelay<Pair<SearchAppResult, android.view.View>> onOpenPopupMenuClickRelay,
      boolean isMultiStoreSearch, String defaultStoreName, String defaultThemeName) {
    this.view = view;
    this.analytics = analytics;
    this.navigator = navigator;
    this.crashReport = crashReport;
    this.viewScheduler = viewScheduler;
    this.searchManager = searchManager;
    this.onAdClickRelay = onAdClickRelay;
    this.onItemViewClickRelay = onItemViewClickRelay;
    this.onOpenPopupMenuClickRelay = onOpenPopupMenuClickRelay;
    this.isMultiStoreSearch = isMultiStoreSearch;
    this.defaultStoreName = defaultStoreName;
    this.defaultThemeName = defaultThemeName;
  }

  @Override public void present() {
    stopLoadingMoreOnDestroy();
    firstSearchDataLoad();
    firstAdsDataLoad();
    handleClickFollowedStoresSearchButton();
    handleClickEverywhereSearchButton();
    handleClickToOpenAppViewFromItem();
    handleClickToOpenAppViewFromAdd();
    handleClickToOpenPopupMenu();
    handleClickOnNoResultsImage();
    handleAllStoresListReachedBottom();
    handleFollowedStoresListReachedBottom();
    restoreSelectedTab();
  }

  private void restoreSelectedTab() {
    view.getLifecycle()
        .filter(event -> event.equals(View.LifecycleEvent.CREATE))
        .first()
        .toSingle()
        .map(__ -> view.getViewModel())
        .subscribe(viewModel -> {
          if (viewModel.getAllStoresOffset() > 0 && viewModel.getFollowedStoresOffset() > 0) {
            if (viewModel.isAllStoresSelected()) {
              view.showAllStoresResult();
            } else {
              view.showFollowedStoresResult();
            }
          }
        }, crashReport::log);
  }

  private void stopLoadingMoreOnDestroy() {
    view.getLifecycle()
        .filter(event -> event.equals(View.LifecycleEvent.DESTROY))
        .first()
        .toSingle()
        .observeOn(viewScheduler)
        .subscribe(__ -> view.hideLoadingMore(), crashReport::log);
  }

  private void handleAllStoresListReachedBottom() {
    view.getLifecycle()
        .filter(event -> event.equals(View.LifecycleEvent.CREATE))
        .observeOn(viewScheduler)
        .flatMap(__ -> view.allStoresResultReachedBottom())
        .map(__ -> view.getViewModel())
        .filter(viewModel -> !viewModel.hasReachedBottomOfAllStores())
        .observeOn(viewScheduler)
        .doOnNext(__ -> view.showLoadingMore())
        .flatMapSingle(viewModel -> loadDataForAllNonFollowedStores(viewModel.getCurrentQuery(),
            viewModel.isOnlyTrustedApps(), viewModel.getAllStoresOffset()).onErrorResumeNext(
            err -> {
              crashReport.log(err);
              return Single.just(null);
            }))
        .observeOn(viewScheduler)
        .doOnNext(__ -> view.hideLoadingMore())
        .filter(data -> data != null)
        .doOnNext(data -> {
          final SearchResultView.Model viewModel = view.getViewModel();
          viewModel.incrementOffsetAndCheckIfReachedBottomOfFollowedStores(getItemCount(data));
        })
        .compose(view.bindUntilEvent(View.LifecycleEvent.DESTROY))
        .subscribe(__ -> {
        }, crashReport::log);
  }

  private int getItemCount(List<SearchAppResult> data) {
    return data != null ? data.size() : 0;
  }

  private void handleFollowedStoresListReachedBottom() {
    view.getLifecycle()
        .filter(event -> event.equals(View.LifecycleEvent.CREATE))
        .observeOn(viewScheduler)
        .flatMap(__ -> view.followedStoresResultReachedBottom())
        .map(__ -> view.getViewModel())
        .filter(viewModel -> !viewModel.hasReachedBottomOfFollowedStores())
        .observeOn(viewScheduler)
        .doOnNext(__ -> view.showLoadingMore())
        .flatMapSingle(viewModel -> loadDataForAllFollowedStores(viewModel.getCurrentQuery(),
            viewModel.isOnlyTrustedApps(), viewModel.getFollowedStoresOffset()).onErrorResumeNext(
            err -> {
              crashReport.log(err);
              return Single.just(null);
            }))
        .observeOn(viewScheduler)
        .doOnNext(__ -> view.hideLoadingMore())
        .filter(data -> data != null)
        .doOnNext(data -> {
          final SearchResultView.Model viewModel = view.getViewModel();
          viewModel.incrementOffsetAndCheckIfReachedBottomOfFollowedStores(getItemCount(data));
        })
        .compose(view.bindUntilEvent(View.LifecycleEvent.DESTROY))
        .subscribe(__ -> {
        }, crashReport::log);
  }

  private void firstAdsDataLoad() {
    view.getLifecycle()
        .filter(event -> event.equals(View.LifecycleEvent.CREATE))
        .map(__ -> view.getViewModel())
        .filter(viewModel -> !viewModel.hasLoadedAds())
        .flatMap(viewModel -> searchManager.getAdsForQuery(viewModel.getCurrentQuery())
            .onErrorReturn(err -> {
              crashReport.log(err);
              return null;
            })
            .observeOn(viewScheduler)
            .doOnNext(__ -> viewModel.setHasLoadedAds())
            .doOnNext(ad -> {
              if (ad == null) {
                view.setFollowedStoresAdsEmpty();
                view.setAllStoresAdsEmpty();
              } else {
                view.setAllStoresAdsResult(ad);
                view.setFollowedStoresAdsResult(ad);
              }
            }))
        .compose(view.bindUntilEvent(View.LifecycleEvent.DESTROY))
        .subscribe(__ -> {
        }, crashReport::log);
  }

  private void handleClickToOpenAppViewFromItem() {
    view.getLifecycle()
        .filter(event -> event.equals(View.LifecycleEvent.CREATE))
        .observeOn(viewScheduler)
        .flatMap(__ -> onItemViewClickRelay)
        .doOnNext(data -> openAppView(data))
        .compose(view.bindUntilEvent(View.LifecycleEvent.DESTROY))
        .subscribe(__ -> {
        }, crashReport::log);
  }

  private void handleClickToOpenAppViewFromAdd() {
    view.getLifecycle()
        .filter(event -> event.equals(View.LifecycleEvent.CREATE))
        .observeOn(viewScheduler)
        .flatMap(__ -> onAdClickRelay)
        .doOnNext(data -> {
          analytics.searchAppClick(view.getViewModel()
              .getCurrentQuery(), data.getPackageName());
          navigator.goToAppView(data);
        })
        .compose(view.bindUntilEvent(View.LifecycleEvent.DESTROY))
        .subscribe(__ -> {
        }, crashReport::log);
  }

  private void handleClickToOpenPopupMenu() {
    view.getLifecycle()
        .filter(event -> event.equals(View.LifecycleEvent.CREATE))
        .observeOn(viewScheduler)
        .flatMap(__ -> onOpenPopupMenuClickRelay)
        .flatMap(pair -> {
          final SearchAppResult data = pair.first;
          final boolean hasVersions = data.hasOtherVersions();
          final String appName = data.getAppName();
          final String appIcon = data.getIcon();
          final String packageName = data.getPackageName();
          final String storeName = data.getStoreName();

          return view.showPopup(hasVersions, pair.second)
              .doOnNext(optionId -> {
                if (optionId == R.id.versions) {
                  if (isMultiStoreSearch) {
                    navigator.goToOtherVersions(appName, appIcon, packageName);
                  } else {
                    navigator.goToOtherVersions(appName, appIcon, packageName, defaultStoreName);
                  }
                } else if (optionId == R.id.go_to_store) {
                  navigator.goToStoreFragment(storeName, defaultThemeName);
                }
              });
        })
        .compose(view.bindUntilEvent(View.LifecycleEvent.DESTROY))
        .subscribe(__ -> {
        }, crashReport::log);
  }

  private void handleClickOnNoResultsImage() {
    view.getLifecycle()
        .filter(event -> event.equals(View.LifecycleEvent.CREATE))
        .observeOn(viewScheduler)
        .flatMap(__ -> view.clickNoResultsSearchButton())
        .filter(query -> query.length() > 1)
        .doOnNext(query -> navigator.goToSearchFragment(query, view.getViewModel()
            .getStoreName()))
        .compose(view.bindUntilEvent(View.LifecycleEvent.DESTROY))
        .subscribe(__ -> {
        }, crashReport::log);
  }

  private void openAppView(SearchAppResult searchApp) {
    final String packageName = searchApp.getPackageName();
    final long appId = searchApp.getAppId();
    final String storeName = searchApp.getStoreName();
    final String themeName = defaultThemeName;
    analytics.searchAppClick(view.getViewModel()
        .getCurrentQuery(), packageName);
    navigator.goToAppView(appId, packageName, themeName, storeName);
  }

  private void handleClickFollowedStoresSearchButton() {
    view.getLifecycle()
        .filter(event -> event.equals(View.LifecycleEvent.CREATE))
        .observeOn(viewScheduler)
        .flatMap(__ -> view.clickFollowedStoresSearchButton())
        .doOnNext(__ -> view.showFollowedStoresResult())
        .compose(view.bindUntilEvent(View.LifecycleEvent.DESTROY))
        .subscribe(__ -> {
        }, crashReport::log);
  }

  private void handleClickEverywhereSearchButton() {
    view.getLifecycle()
        .filter(event -> event.equals(View.LifecycleEvent.CREATE))
        .observeOn(viewScheduler)
        .flatMap(__ -> view.clickEverywhereSearchButton())
        .doOnNext(__ -> view.showAllStoresResult())
        .compose(view.bindUntilEvent(View.LifecycleEvent.DESTROY))
        .subscribe(__ -> {
        }, crashReport::log);
  }

  private Observable<List<SearchAppResult>> loadData(String query, String storeName,
      String defaultStoreName, boolean onlyTrustedApps, int offset) {

    if (storeName != null && !storeName.trim()
        .equals("")) {
      return Single.defer(() -> {
        view.setViewWithStoreNameAsSingleTab(storeName);
        return loadDataForSpecificStore(query, storeName, offset);
      })
          .toObservable();
    }

    if (!isMultiStoreSearch && defaultStoreName != null && !defaultStoreName.trim()
        .equals("")) {
      return Single.defer(() -> {
        view.setViewWithStoreNameAsSingleTab(defaultStoreName);
        return loadDataForSpecificStore(query, defaultStoreName, offset);
      })
          .toObservable();
    }

    // search every store. followed and not followed
    return Single.merge(loadDataForAllFollowedStores(query, onlyTrustedApps, offset),
        loadDataForAllNonFollowedStores(query, onlyTrustedApps, offset));
  }

  @NonNull private Single<List<SearchAppResult>> loadDataForAllNonFollowedStores(String query,
      boolean onlyTrustedApps, int offset) {
    return searchManager.searchInNonFollowedStores(query, onlyTrustedApps, offset)
        .observeOn(viewScheduler)
        .doOnSuccess(data -> {
          view.addAllStoresResult(data);
          final SearchResultView.Model viewModel = view.getViewModel();
          viewModel.incrementOffsetAndCheckIfReachedBottomOfAllStores(getItemCount(data));
        });
  }

  @NonNull private Single<List<SearchAppResult>> loadDataForAllFollowedStores(String query,
      boolean onlyTrustedApps, int offset) {
    return searchManager.searchInFollowedStores(query, onlyTrustedApps, offset)
        .observeOn(viewScheduler)
        .doOnSuccess(data -> {
          view.addFollowedStoresResult(data);
          final SearchResultView.Model viewModel = view.getViewModel();
          viewModel.incrementOffsetAndCheckIfReachedBottomOfFollowedStores(getItemCount(data));
        });
  }

  @NonNull
  private Single<List<SearchAppResult>> loadDataForSpecificStore(String query, String storeName,
      int offset) {
    return searchManager.searchInStore(query, storeName, offset)
        .observeOn(viewScheduler)
        .doOnSuccess(data -> {
          view.addFollowedStoresResult(data);
          final SearchResultView.Model viewModel = view.getViewModel();
          viewModel.setAllStoresSelected(false);
          viewModel.incrementOffsetAndCheckIfReachedBottomOfFollowedStores(getItemCount(data));
        });
  }

  private void firstSearchDataLoad() {
    view.getLifecycle()
        .filter(event -> event.equals(View.LifecycleEvent.CREATE))
        .map(__ -> view.getViewModel())
        .filter(viewModel -> viewModel.getAllStoresOffset() == 0
            && viewModel.getFollowedStoresOffset() == 0)
        .observeOn(viewScheduler)
        .doOnNext(__ -> view.showLoading())
        .doOnNext(viewModel -> analytics.search(viewModel.getCurrentQuery()))
        .flatMap(viewModel -> loadData(viewModel.getCurrentQuery(), viewModel.getStoreName(),
            viewModel.getDefaultStoreName(), viewModel.isOnlyTrustedApps(), 0).onErrorResumeNext(
            err -> {
              crashReport.log(err);
              return Observable.just(null);
            })
            .observeOn(viewScheduler)
            .doOnNext(data -> {
              view.hideLoading();
              if (data == null || getItemCount(data) == 0) {
                view.showNoResultsView();
                analytics.searchNoResults(viewModel.getCurrentQuery());
              } else {
                view.showResultsView();
                if (viewModel.isAllStoresSelected()) {
                  view.showAllStoresResult();
                } else {
                  view.showFollowedStoresResult();
                }
              }
            }))
        .compose(view.bindUntilEvent(View.LifecycleEvent.DESTROY))
        .subscribe(__ -> {
        }, crashReport::log);
  }
}
