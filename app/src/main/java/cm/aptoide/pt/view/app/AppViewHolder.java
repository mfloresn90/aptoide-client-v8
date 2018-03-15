package cm.aptoide.pt.view.app;

import android.support.v7.widget.RecyclerView;
import android.view.View;

/**
 * Created by trinkes on 19/10/2017.
 */

abstract class AppViewHolder extends RecyclerView.ViewHolder {
  public AppViewHolder(View itemView) {
    super(itemView);
  }

  abstract void setApp(Application app);
}
