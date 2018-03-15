package cm.aptoide.pt.search.view.item;

import android.content.Context;
import android.content.res.Resources;
import android.view.View;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import cm.aptoide.pt.R;
import cm.aptoide.pt.networking.image.ImageLoader;
import cm.aptoide.pt.search.model.SearchAdResult;
import cm.aptoide.pt.utils.AptoideUtils;
import com.jakewharton.rxbinding.view.RxView;
import com.jakewharton.rxrelay.PublishRelay;
import java.util.Date;

public class SearchResultAdViewHolder extends SearchResultItemView<SearchAdResult> {

  public static final int LAYOUT = R.layout.search_ad;
  private final PublishRelay<SearchAdResult> onItemViewClickRelay;

  private TextView name;
  private ImageView icon;
  private TextView downloadsTextView;
  private RatingBar ratingBar;
  private TextView timeTextView;
  private SearchAdResult adResult;

  public SearchResultAdViewHolder(View itemView,
      PublishRelay<SearchAdResult> onItemViewClickRelay) {
    super(itemView);
    this.onItemViewClickRelay = onItemViewClickRelay;
    bind(itemView);
  }

  private void bind(View itemView) {
    name = (TextView) itemView.findViewById(R.id.name);
    icon = (ImageView) itemView.findViewById(R.id.icon);
    downloadsTextView = (TextView) itemView.findViewById(R.id.downloads);
    ratingBar = (RatingBar) itemView.findViewById(R.id.ratingbar);
    timeTextView = (TextView) itemView.findViewById(R.id.search_time);
    RxView.clicks(itemView)
        .map(__ -> adResult)
        .subscribe(data -> onItemViewClickRelay.call(data));
  }

  @Override public void setup(SearchAdResult searchAd) {
    final Context context = itemView.getContext();
    final Resources resources = itemView.getResources();
    this.adResult = searchAd;
    setName(searchAd);
    setIcon(searchAd, context);
    setDownloadsCount(searchAd, resources);
    setRatingStars(searchAd);
    setModifiedDate(searchAd, resources);
  }

  private void setIcon(SearchAdResult searchAd, Context context) {
    ImageLoader.with(context)
        .load(searchAd.getIcon(), icon);
  }

  private void setName(SearchAdResult searchAd) {
    name.setText(searchAd.getAppName());
  }

  private void setDownloadsCount(SearchAdResult searchAd, Resources resources) {
    String downloadNumber =
        AptoideUtils.StringU.withSuffix(searchAd.getTotalDownloads()) + " " + resources.getString(
            R.string.downloads);
    downloadsTextView.setText(downloadNumber);
  }

  private void setRatingStars(SearchAdResult searchAd) {
    ratingBar.setRating(searchAd.getStarRating());
  }

  private void setModifiedDate(SearchAdResult searchAd, Resources resources) {
    if (searchAd.getModifiedDate() > 0) {
      Date modified = new Date(searchAd.getModifiedDate());
      String timeSinceUpdate = AptoideUtils.DateTimeU.getInstance(itemView.getContext())
          .getTimeDiffAll(itemView.getContext(), modified.getTime(), resources);
      if (timeSinceUpdate != null && !timeSinceUpdate.equals("")) {
        timeTextView.setText(timeSinceUpdate);
      }
    }
  }
}
