/*
 * Copyright (c) 2016.
 * Modified on 14/06/2016.
 */

package cm.aptoide.pt.download.view.scheduled;

import cm.aptoide.pt.R;
import cm.aptoide.pt.dataprovider.model.v7.GetApp;
import cm.aptoide.pt.view.recycler.displayable.DisplayablePojo;

/**
 * Created on 14/06/16.
 */
public class ScheduleDownloadDisplayable extends DisplayablePojo<GetApp> {

  @Override protected Configs getConfig() {
    return new Configs(1, false);
  }

  @Override public int getViewLayout() {
    return R.layout.displayable_grid_scheduled_download;
  }
}
