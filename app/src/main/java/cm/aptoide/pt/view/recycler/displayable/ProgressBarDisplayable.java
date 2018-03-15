/*
 * Copyright (c) 2016.
 * Modified by Neurophobic Animal on 08/06/2016.
 */

package cm.aptoide.pt.view.recycler.displayable;

import cm.aptoide.pt.R;

/**
 * Created by neuro on 07-06-2016.
 */
public class ProgressBarDisplayable extends Displayable {

  public ProgressBarDisplayable() {
  }

  @Override protected Configs getConfig() {
    return new Configs(1, false);
  }

  @Override public int getViewLayout() {
    return R.layout.row_progress_bar;
  }
}
