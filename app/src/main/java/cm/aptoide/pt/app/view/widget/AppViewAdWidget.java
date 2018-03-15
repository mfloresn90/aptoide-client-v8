package cm.aptoide.pt.app.view.widget;

import android.view.View;
import android.widget.RatingBar;
import android.widget.TextView;
import cm.aptoide.pt.R;
import cm.aptoide.pt.utils.AptoideUtils;
import cm.aptoide.pt.view.recycler.displayable.GridAdDisplayable;
import cm.aptoide.pt.view.recycler.widget.GridAdWidget;

/**
 * Created by neuro on 01-08-2017.
 */

public class AppViewAdWidget extends GridAdWidget {

  private TextView downloadsTextView;
  private RatingBar ratingBar;

  public AppViewAdWidget(View itemView) {
    super(itemView);
  }

  @Override protected void assignViews(View itemView) {
    super.assignViews(itemView);

    downloadsTextView = (TextView) itemView.findViewById(R.id.downloads);
    ratingBar = (RatingBar) itemView.findViewById(R.id.ratingbar);
  }

  @Override public void bindView(GridAdDisplayable displayable) {
    super.bindView(displayable);

    int downloads = displayable.getPojo()
        .getDownloads();

    downloadsTextView.setText(
        AptoideUtils.StringU.withSuffix(downloads) + getContext().getString(R.string._downloads));
    ratingBar.setRating(displayable.getPojo()
        .getStars());
  }
}
