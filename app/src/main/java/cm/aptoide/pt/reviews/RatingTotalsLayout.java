package cm.aptoide.pt.reviews;

import android.support.v7.widget.AppCompatRatingBar;
import android.view.View;
import android.widget.TextView;
import cm.aptoide.pt.R;
import cm.aptoide.pt.dataprovider.model.v7.GetAppMeta;
import cm.aptoide.pt.utils.AptoideUtils;
import java.util.Locale;

public class RatingTotalsLayout {

  private TextView usersVoted;
  private TextView ratingValue;
  private AppCompatRatingBar ratingBar;

  public RatingTotalsLayout(View view) {
    usersVoted = (TextView) view.findViewById(R.id.users_voted);
    ratingValue = (TextView) view.findViewById(R.id.rating_value);
    ratingBar = (AppCompatRatingBar) view.findViewById(R.id.rating_bar);
  }

  public void setup(GetAppMeta.Stats.Rating rating) {
    usersVoted.setText(AptoideUtils.StringU.withSuffix(rating.getTotal()));
    ratingValue.setText(String.format(Locale.getDefault(), "%.1f", rating.getAvg()));
    ratingBar.setRating(rating.getAvg());
  }
}
