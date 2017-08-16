/*
 * Copyright (c) 2016.
 * Modified on 28/07/2016.
 */

package cm.aptoide.pt.view.updates.rollback;

import android.content.Context;
import cm.aptoide.pt.R;
import cm.aptoide.pt.V8Engine;
import cm.aptoide.pt.database.realm.Download;
import cm.aptoide.pt.database.realm.Rollback;
import cm.aptoide.pt.download.DownloadFactory;
import cm.aptoide.pt.install.Installer;
import cm.aptoide.pt.view.navigator.FragmentNavigator;
import cm.aptoide.pt.view.recycler.displayable.DisplayablePojo;
import rx.Completable;

/**
 * Created on 14/06/16.
 */
public class RollbackDisplayable extends DisplayablePojo<Rollback> {

  private Installer installManager;

  public RollbackDisplayable() {
  }

  public RollbackDisplayable(Installer installManager, Rollback pojo) {
    this(installManager, pojo, false);
  }

  private RollbackDisplayable(Installer installManager, Rollback pojo, boolean fixedPerLineCount) {
    super(pojo);
    this.installManager = installManager;
  }

  public Download getDownloadFromPojo() {
    return new DownloadFactory().create(getPojo());
  }

  @Override protected Configs getConfig() {
    return new Configs(1, false);
  }

  @Override public int getViewLayout() {
    return R.layout.rollback_row;
  }

  public void install(FragmentNavigator navigator) {
    openAppview(navigator);
  }

  public void openAppview(FragmentNavigator navigator) {
    navigator.navigateToAllowingStateLoss(V8Engine.getFragmentProvider()
        .newAppViewFragment(getPojo().getMd5()));
  }

  public Completable uninstall(Context context, Download appDownload) {
    return installManager.uninstall(context, appDownload.getFilesToDownload()
        .get(0)
        .getPackageName(), appDownload.getVersionName());
  }

  public void downgrade(FragmentNavigator navigator) {
    openAppview(navigator);
  }

  public void update(FragmentNavigator navigator) {
    openAppview(navigator);
  }
}
