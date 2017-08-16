package cm.aptoide.pt.view.recycler.widget;

import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import cm.aptoide.pt.R;
import cm.aptoide.pt.V8Engine;
import cm.aptoide.pt.analytics.Analytics;
import cm.aptoide.pt.crashreports.CrashReport;
import cm.aptoide.pt.database.realm.MinimalAd;
import cm.aptoide.pt.networking.image.ImageLoader;
import cm.aptoide.pt.view.recycler.displayable.GridAdDisplayable;
import com.jakewharton.rxbinding.view.RxView;

/**
 * Created by neuro on 20-06-2016.
 */
public class GridAdWidget extends Widget<GridAdDisplayable> {

  private TextView name;
  private ImageView icon;

  public GridAdWidget(View itemView) {
    super(itemView);
  }

  @Override protected void assignViews(View itemView) {
    name = (TextView) itemView.findViewById(R.id.name);
    icon = (ImageView) itemView.findViewById(R.id.icon);
  }

  @Override public void bindView(GridAdDisplayable displayable) {
    MinimalAd pojo = displayable.getPojo();
    name.setText(pojo.getName());

    final FragmentActivity context = getContext();
    ImageLoader.with(context)
        .load(pojo.getIconPath(), icon);

    compositeSubscription.add(RxView.clicks(itemView)
        .subscribe(v -> {
          Analytics.AppViewViewedFrom.addStepToList(displayable.getTag());
          getFragmentNavigator().navigateTo(V8Engine.getFragmentProvider()
              .newAppViewFragment(pojo));
        }, throwable -> CrashReport.getInstance()
            .log(throwable)));
  }
}
