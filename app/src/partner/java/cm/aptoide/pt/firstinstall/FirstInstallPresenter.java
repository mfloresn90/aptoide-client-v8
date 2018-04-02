package cm.aptoide.pt.firstinstall;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.view.WindowManager;
import cm.aptoide.pt.AptoideApplication;
import cm.aptoide.pt.actions.PermissionManager;
import cm.aptoide.pt.actions.PermissionService;
import cm.aptoide.pt.ads.AdsRepository;
import cm.aptoide.pt.ads.MinimalAdMapper;
import cm.aptoide.pt.app.AppRepository;
import cm.aptoide.pt.app.FirstInstallAnalytics;
import cm.aptoide.pt.crashreports.CrashReport;
import cm.aptoide.pt.database.accessors.StoredMinimalAdAccessor;
import cm.aptoide.pt.database.realm.Download;
import cm.aptoide.pt.database.realm.MinimalAd;
import cm.aptoide.pt.dataprovider.ads.AdNetworkUtils;
import cm.aptoide.pt.dataprovider.model.v7.GetStoreWidgets;
import cm.aptoide.pt.dataprovider.model.v7.Layout;
import cm.aptoide.pt.dataprovider.model.v7.ListApps;
import cm.aptoide.pt.dataprovider.model.v7.Type;
import cm.aptoide.pt.dataprovider.model.v7.listapp.App;
import cm.aptoide.pt.dataprovider.ws.v7.store.StoreContext;
import cm.aptoide.pt.download.DownloadFactory;
import cm.aptoide.pt.firstinstall.displayable.FirstInstallAdDisplayable;
import cm.aptoide.pt.firstinstall.displayable.FirstInstallAppDisplayable;
import cm.aptoide.pt.install.InstallManager;
import cm.aptoide.pt.presenter.Presenter;
import cm.aptoide.pt.presenter.View;
import cm.aptoide.pt.repository.request.RequestFactory;
import cm.aptoide.pt.search.model.SearchAdResult;
import cm.aptoide.pt.util.ReferrerUtils;
import cm.aptoide.pt.utils.AptoideUtils;
import cm.aptoide.pt.utils.q.QManager;
import cm.aptoide.pt.view.recycler.displayable.Displayable;
import cm.aptoide.pt.view.recycler.displayable.DisplayableGroup;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import okhttp3.OkHttpClient;
import retrofit2.Converter;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * Created by diogoloureiro on 02/10/2017.
 *
 * First install Presenter implementation
 */

public class FirstInstallPresenter implements Presenter {

  private FirstInstallView view;
  private CrashReport crashReport;
  private RequestFactory requestFactoryCdnPool;
  private Context context;
  private String storeName;
  private String url;
  private AdsRepository adsRepository;
  private Resources resources;
  private WindowManager windowManager;
  private AppRepository appRepository;
  private FirstInstallAnalytics firstInstallAnalytics;
  private PermissionManager permissionManager;
  private PermissionService permissionService;
  private InstallManager installManager;
  private MinimalAdMapper adMapper;
  private OkHttpClient httpClient;
  private Converter.Factory converterFactory;
  private QManager qManager;
  private StoredMinimalAdAccessor storedMinimalAdAccessor;

  private List<FirstInstallAppDisplayable> appDisplayables;
  private List<FirstInstallAdDisplayable> adDisplayables;

  FirstInstallPresenter(FirstInstallView view, CrashReport crashReport,
      RequestFactory requestFactoryCdnPool, Context context, String storeName, String url,
      AdsRepository adsRepository, Resources resources, WindowManager windowManager,
      AppRepository appRepository, FirstInstallAnalytics firstInstallAnalytics,
      PermissionManager permissionManager, PermissionService permissionService,
      InstallManager installManager, MinimalAdMapper adMapper, OkHttpClient httpClient,
      Converter.Factory converterFactory, QManager qManager,
      StoredMinimalAdAccessor storedMinimalAdAccessor) {
    this.view = view;
    this.crashReport = crashReport;
    this.requestFactoryCdnPool = requestFactoryCdnPool;
    this.context = context;
    this.storeName = storeName;
    this.url = url;
    this.adsRepository = adsRepository;
    this.resources = resources;
    this.windowManager = windowManager;
    this.appRepository = appRepository;
    this.firstInstallAnalytics = firstInstallAnalytics;
    this.permissionManager = permissionManager;
    this.permissionService = permissionService;
    this.installManager = installManager;
    this.adMapper = adMapper;
    this.httpClient = httpClient;
    this.converterFactory = converterFactory;
    this.qManager = qManager;
    this.storedMinimalAdAccessor = storedMinimalAdAccessor;
  }

  @Override public void present() {
    appDisplayables = new ArrayList<>();
    adDisplayables = new ArrayList<>();

    handleInstallAllClick();
    handleCloseClick();
    getFirstInstallWidget();
  }

  /**
   * handle the install all button click
   */
  private void handleInstallAllClick() {
    view.getLifecycle()
        .filter(event -> event.equals(View.LifecycleEvent.CREATE))
        .flatMap(resumed -> view.installAllClick())
        .compose(view.bindUntilEvent(View.LifecycleEvent.DESTROY))
        .subscribe(installAllClick -> installAll(appDisplayables, adDisplayables),
            crashReport::log);
  }

  /**
   * handle the close button click
   */
  private void handleCloseClick() {
    view.getLifecycle()
        .filter(event -> event.equals(View.LifecycleEvent.CREATE))
        .flatMap(resumed -> view.closeClick())
        .compose(view.bindUntilEvent(View.LifecycleEvent.DESTROY))
        .subscribe(closeClick -> {
          firstInstallAnalytics.sendCloseWindowsEvent();
          view.removeFragmentAnimation();
        }, crashReport::log);
  }

  /**
   * get the apps from the firstInstall Widget to display
   */
  private void getFirstInstallWidget() {
    requestFactoryCdnPool.newStoreWidgets(url, storeName, StoreContext.first_install)
        .observe(true)
        .subscribeOn(Schedulers.newThread())
        .observeOn(AndroidSchedulers.mainThread())
        .flatMap(this::parseGetStoreWidgetsToDisplayables)
        .subscribe(done -> {
        }, crashReport::log, () -> getAds(getNumberOfAdsToShow(appDisplayables.size())));
  }

  /**
   * parse displayables from the getStoreWidgets list received
   *
   * @param getStoreWidgets received list
   *
   * @return observable of displayables from the parsed list
   */
  private Observable<List<Displayable>> parseGetStoreWidgetsToDisplayables(
      GetStoreWidgets getStoreWidgets) {
    return Observable.from(getStoreWidgets.getDataList()
        .getList())
        .concatMapEager(wsWidget -> wsWidgetParser(wsWidget).toList()
            .first());
  }

  /**
   * parse the wsWidget to displayables
   *
   * @param wsWidget first install wsWidget
   *
   * @return Observable
   */
  private Observable<Displayable> wsWidgetParser(GetStoreWidgets.WSWidget wsWidget) {
    ListApps listApps = (ListApps) wsWidget.getViewObject();
    if (listApps == null) {
      return Observable.empty();
    }

    List<Displayable> displayables = new ArrayList<>(listApps.getDataList()
        .getList()
        .size());
    if (Layout.LIST.equals(wsWidget.getData()
        .getLayout())) {
      for (App app : listApps.getDataList()
          .getList()) {
        if (!isPackageInstalled(app.getPackageName(), context.getPackageManager())) {
          FirstInstallAppDisplayable firstInstallAppDisplayable =
              new FirstInstallAppDisplayable(app, true);
          displayables.add(firstInstallAppDisplayable);
          appDisplayables.add(firstInstallAppDisplayable);
        }
      }
    }
    return Observable.just(new DisplayableGroup(displayables, windowManager, resources));
  }

  /**
   * request ads for the first install window
   *
   * @param limitOfAds max limit of ads to receive on the response
   */
  private void getAds(int limitOfAds) {
    adsRepository.getAdsFromFirstInstall(limitOfAds)
        .subscribeOn(Schedulers.newThread())
        .observeOn(AndroidSchedulers.mainThread())
        .flatMap(this::parseMinimalAdsToDisplayables)
        .subscribe(done -> {
        }, crashReport::log, () -> view.addFirstInstallDisplayables(new ArrayList<Displayable>() {
          {
            addAll(appDisplayables);
            addAll(adDisplayables);
          }
        }, true));
  }

  /**
   * parse displayables from the minimalads list received
   *
   * @param minimalAds received list
   *
   * @return observable of displayables from the parsed list
   */
  private Observable<List<Displayable>> parseMinimalAdsToDisplayables(List<MinimalAd> minimalAds) {
    List<Displayable> displayables = new LinkedList<>();
    for (MinimalAd minimalAd : minimalAds) {
      handleAdsLogic(minimalAd);
      FirstInstallAdDisplayable firstInstallAdDisplayable = new FirstInstallAdDisplayable(minimalAd,
          minimalAd.getAdId()
              .toString(), true);
      adDisplayables.add(firstInstallAdDisplayable);
      displayables.add(firstInstallAdDisplayable);
    }
    return Observable.just(displayables);
  }

  /**
   * handle ads logic by storing it and extracting the referrer
   *
   * @param minimalAd minimal ad to store and extract referrer
   */
  private void handleAdsLogic(MinimalAd minimalAd) {
    SearchAdResult searchAdResult = new SearchAdResult(minimalAd);
    storedMinimalAdAccessor.insert(adMapper.map(searchAdResult, null));
    AdNetworkUtils.knockCpc(adMapper.map(searchAdResult));
    AptoideUtils.ThreadU.runOnUiThread(
        () -> ReferrerUtils.extractReferrer(searchAdResult, ReferrerUtils.RETRIES, false,
            adsRepository, httpClient, converterFactory, qManager, context.getApplicationContext(),
            ((AptoideApplication) context.getApplicationContext()).getDefaultSharedPreferences(),
            new MinimalAdMapper()));
  }

  /**
   * get the number of ads needed to fill the first install window
   *
   * @param numberOfApps number of apps displayed
   *
   * @return number of ads to request
   */
  private int getNumberOfAdsToShow(int numberOfApps) {
    int numberOfAds = Type.ADS.getPerLineCount(resources, windowManager) * 2 - numberOfApps;
    return numberOfAds > 0 ? numberOfAds : 0;
  }

  /**
   * request for external storage permission
   * do a get app request for every displayable received
   * download all the apps received
   * call the installer for all downloaded apps
   *
   * @param appDisplayablesList list of firstInstallAppDisplayables to download and install
   */
  private void installAll(List<FirstInstallAppDisplayable> appDisplayablesList,
      List<FirstInstallAdDisplayable> adDisplayablesList) {
    permissionManager.requestDownloadAccess(permissionService)
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .flatMap(success -> permissionManager.requestExternalStoragePermission(permissionService))
        .map(success -> Observable.just(appDisplayablesList)
            .subscribeOn(Schedulers.newThread())
            .observeOn(AndroidSchedulers.mainThread())
            .flatMapIterable(displayables -> displayables)
            .filter(FirstInstallAppDisplayable::isSelected)
            .map(firstInstallAppDisplayable -> appRepository.getApp(
                firstInstallAppDisplayable.getPojo()
                    .getId(), true, false, firstInstallAppDisplayable.getPojo()
                    .getStore()
                    .getName(), firstInstallAppDisplayable.getPojo()
                    .getPackageName(), false)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .map(app -> new DownloadFactory(app.getNodes()
                    .getMeta()
                    .getData()
                    .getStore()
                    .getName()).create(app.getNodes()
                    .getMeta()
                    .getData(), Download.ACTION_INSTALL))
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .flatMapCompletable(download -> installManager.install(download))
                .subscribe(done -> {
                }, crashReport::log))
            .subscribe(done -> {
            }, crashReport::log))
        .map(success -> Observable.just(adDisplayablesList)
            .subscribeOn(Schedulers.newThread())
            .observeOn(AndroidSchedulers.mainThread())
            .flatMapIterable(displayables -> displayables)
            .filter(FirstInstallAdDisplayable::isSelected)
            .map(firstInstallAdDisplayable -> appRepository.getApp(
                firstInstallAdDisplayable.getPojo()
                    .getAppId(), true, true, null, firstInstallAdDisplayable.getPojo()
                    .getPackageName(), false)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .map(app -> new DownloadFactory(app.getNodes()
                    .getMeta()
                    .getData()
                    .getStore()
                    .getName()).create(app.getNodes()
                    .getMeta()
                    .getData(), Download.ACTION_INSTALL))
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .flatMapCompletable(download -> installManager.install(download))
                .subscribe(done -> {
                }, crashReport::log))
            .subscribe(done -> {
            }, crashReport::log))
        .subscribe(done -> {
        }, crashReport::log, () -> {
          analyticsForDownload(adDisplayablesList, appDisplayablesList);
          view.removeFragmentAnimation();
        });
  }

  /**
   * check if an application is installed
   *
   * @param packagename app's packagename
   * @param packageManager packagemanager
   *
   * @return true if installed, false if not
   */
  private boolean isPackageInstalled(String packagename, PackageManager packageManager) {
    try {
      packageManager.getPackageInfo(packagename, 0);
      return true;
    } catch (PackageManager.NameNotFoundException e) {
      return false;
    }
  }

  /**
   * setup analytics for downloads. Only selected apps and ads will be submited to analytics
   *
   * @param adDisplayablesList list of ads
   * @param appDisplayablesList list of apps
   */
  private void analyticsForDownload(List<FirstInstallAdDisplayable> adDisplayablesList,
      List<FirstInstallAppDisplayable> appDisplayablesList) {
    int numberOfSelectedAds = 0;
    int numberOfSelectedApps = 0;
    for (FirstInstallAdDisplayable firstInstallAdDisplayable : adDisplayablesList) {
      if (firstInstallAdDisplayable.isSelected()) {
        numberOfSelectedAds++;
      }
    }
    for (FirstInstallAppDisplayable firstInstallAppDisplayable : appDisplayablesList) {
      if (firstInstallAppDisplayable.isSelected()) {
        numberOfSelectedApps++;
      }
    }
    firstInstallAnalytics.sendStartDownloadEvent(String.valueOf(numberOfSelectedAds),
        String.valueOf(numberOfSelectedApps));
  }
}
