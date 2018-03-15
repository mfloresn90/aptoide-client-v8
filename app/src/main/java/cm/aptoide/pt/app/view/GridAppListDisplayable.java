/*
 * Copyright (c) 2016.
 * Modified by Neurophobic Animal on 30/06/2016.
 */

package cm.aptoide.pt.app.view;

import cm.aptoide.pt.R;
import cm.aptoide.pt.dataprovider.model.v7.listapp.App;
import cm.aptoide.pt.view.recycler.displayable.DisplayablePojo;

/**
 * Created by neuro on 29-06-2016.
 */
public class GridAppListDisplayable extends DisplayablePojo<App> {

  private String tag;

  public GridAppListDisplayable() {
  }

  public GridAppListDisplayable(App pojo, String tag) {
    super(pojo);
    this.tag = tag;
  }

  @Override protected Configs getConfig() {
    return new Configs(1, false);
  }

  @Override public int getViewLayout() {
    return R.layout.displayable_list_app;
  }

  public String getTag() {
    return tag;
  }
}
