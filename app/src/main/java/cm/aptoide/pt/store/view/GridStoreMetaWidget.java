package cm.aptoide.pt.store.view;

import android.os.Build;
import android.support.annotation.ColorInt;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import cm.aptoide.accountmanager.AptoideAccountManager;
import cm.aptoide.pt.AptoideApplication;
import cm.aptoide.pt.R;
import cm.aptoide.pt.database.AccessorFactory;
import cm.aptoide.pt.database.realm.Store;
import cm.aptoide.pt.dataprovider.WebService;
import cm.aptoide.pt.dataprovider.ws.BodyInterceptor;
import cm.aptoide.pt.dataprovider.ws.v7.BaseBody;
import cm.aptoide.pt.networking.image.ImageLoader;
import cm.aptoide.pt.store.StoreCredentialsProviderImpl;
import cm.aptoide.pt.store.StoreTheme;
import cm.aptoide.pt.store.StoreUtilsProxy;
import cm.aptoide.pt.utils.AptoideUtils;
import cm.aptoide.pt.view.recycler.displayable.SpannableFactory;
import java.util.List;
import okhttp3.OkHttpClient;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;

/**
 * Created by neuro on 04-08-2016.
 */
public class GridStoreMetaWidget extends MetaStoresBaseWidget<GridStoreMetaDisplayable> {

  private AptoideAccountManager accountManager;
  private LinearLayout socialChannelsLayout;
  private ImageView mainIcon;
  private TextView mainName;
  private TextView description;
  private Button actionButton;
  private TextView followersCountTv;
  private TextView appsCountTv;
  private TextView followingCountTv;
  private ImageView secondaryIcon;
  private TextView secondaryName;
  private StoreUtilsProxy storeUtilsProxy;
  private ImageView badgeIcon;
  private View separator;

  public GridStoreMetaWidget(View itemView) {
    super(itemView);
  }

  @Override protected void assignViews(View itemView) {
    socialChannelsLayout = (LinearLayout) itemView.findViewById(R.id.social_channels);
    mainIcon = (ImageView) itemView.findViewById(R.id.main_icon);
    secondaryIcon = (ImageView) itemView.findViewById(R.id.secondary_icon);
    mainName = (TextView) itemView.findViewById(R.id.main_name);
    secondaryName = (TextView) itemView.findViewById(R.id.secondary_name);
    description = (TextView) itemView.findViewById(R.id.description);
    actionButton = (Button) itemView.findViewById(R.id.action_button);
    badgeIcon = (ImageView) itemView.findViewById(R.id.badge_icon);
    appsCountTv = (TextView) itemView.findViewById(R.id.apps_text_view);
    followingCountTv = (TextView) itemView.findViewById(R.id.following_text_view);
    followersCountTv = (TextView) itemView.findViewById(R.id.followers_text_view);
    separator = itemView.findViewById(R.id.separator);
  }

  @Override public void bindView(GridStoreMetaDisplayable displayable) {

    accountManager =
        ((AptoideApplication) getContext().getApplicationContext()).getAccountManager();
    final BodyInterceptor<BaseBody> bodyInterceptor =
        ((AptoideApplication) getContext().getApplicationContext()).getAccountSettingsBodyInterceptorPoolV7();
    final OkHttpClient httpClient =
        ((AptoideApplication) getContext().getApplicationContext()).getDefaultClient();
    storeUtilsProxy = new StoreUtilsProxy(accountManager, bodyInterceptor,
        new StoreCredentialsProviderImpl(AccessorFactory.getAccessorFor(
            ((AptoideApplication) getContext().getApplicationContext()
                .getApplicationContext()).getDatabase(), Store.class)),
        AccessorFactory.getAccessorFor(((AptoideApplication) getContext().getApplicationContext()
            .getApplicationContext()).getDatabase(), Store.class), httpClient,
        WebService.getDefaultConverter(),
        ((AptoideApplication) getContext().getApplicationContext()).getTokenInvalidator(),
        ((AptoideApplication) getContext().getApplicationContext()).getDefaultSharedPreferences());

    compositeSubscription.add(createHomeMeta(displayable).observeOn(AndroidSchedulers.mainThread())
        .doOnNext(homeMeta -> {
          showMainIcon(homeMeta.getMainIcon());
          showSecondaryIcon(homeMeta.getSecondaryIcon());
          showMainName(homeMeta.getMainName());
          showSecondaryName(homeMeta.getSecondaryName());
          setupActionButton(homeMeta.isOwner());
          showSocialChannels(homeMeta.getSocialChannels());
          showAppsCount(homeMeta.getAppsCount(), homeMeta.getThemeColor());
          showFollowersCount(homeMeta.getFollowersCount(), homeMeta.getThemeColor());
          showFollowingCount(homeMeta.getFollowingCount(), homeMeta.getThemeColor());
          showDescription(homeMeta.getDescription());
        })
        .subscribe());
  }

  private void showDescription(String descriptionText) {
    if (descriptionText != null && !descriptionText.isEmpty()) {
      description.setText(descriptionText);
      description.setVisibility(View.VISIBLE);
    } else {
      description.setVisibility(View.GONE);
    }
  }

  private void showFollowingCount(long followingCount, @ColorInt int color) {
    String count = AptoideUtils.StringU.withSuffix(followingCount);
    String followingText = String.format(getContext().getString(R.string.following), count);
    followingCountTv.setText(new SpannableFactory().createColorSpan(followingText, color, count));
  }

  private void showFollowersCount(long followersCount, @ColorInt int color) {
    String count = AptoideUtils.StringU.withSuffix(followersCount);
    String followingText = String.format(getContext().getString(R.string.subscribers), count);
    followersCountTv.setText(new SpannableFactory().createColorSpan(followingText, color, count));
  }

  private void showAppsCount(long appsCount, @ColorInt int color) {
    String count = AptoideUtils.StringU.withSuffix(appsCount);
    String followingText = String.format(getContext().getString(R.string.apps), count);
    appsCountTv.setText(new SpannableFactory().createColorSpan(followingText, color, count));
  }

  private void showSocialChannels(
      List<cm.aptoide.pt.dataprovider.model.v7.store.Store.SocialChannel> socialChannels) {
    if (socialChannels != null && !socialChannels.isEmpty()) {
      setupSocialLinks(socialChannels, socialChannelsLayout);
      socialChannelsLayout.setVisibility(View.VISIBLE);
      separator.setVisibility(View.GONE);
    } else {
      socialChannelsLayout.setVisibility(View.GONE);
      separator.setVisibility(View.VISIBLE);
    }
  }

  private void setupActionButton(boolean owner) {
    // TODO: 03/10/2017 trinkes 
  }

  private void showSecondaryName(String secondaryNameString) {
    if (secondaryName != null) {
      secondaryName.setText(secondaryNameString);
    }
  }

  private void showMainName(String mainNameString) {
    if (mainNameString != null) {
      mainName.setText(mainNameString);
    }
  }

  private Observable<HomeMeta> createHomeMeta(GridStoreMetaDisplayable displayable) {
    return Observable.just(new HomeMeta(displayable.getMainIcon(), displayable.getSecondaryIcon(),
        displayable.getMainName(), displayable.getSecondaryName(),
        displayable.isLoggedIn(accountManager), displayable.getSocialLinks(),
        displayable.getAppsCount(), displayable.getFollowersCount(),
        displayable.getFollowingsCount(), displayable.getDescription(),
        getColorOrDefault(displayable.getStoreTheme())));
  }

  private @ColorInt int getColorOrDefault(StoreTheme theme) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      return getContext().getResources()
          .getColor(theme.getPrimaryColor(), getContext().getTheme());
    } else {
      return getContext().getResources()
          .getColor(theme.getPrimaryColor());
    }
  }

  private void showMainIcon(String mainIconUrl) {
    ImageLoader.with(getContext())
        .loadWithShadowCircleTransform(mainIconUrl, mainIcon);
  }

  private void showSecondaryIcon(String secondaryIconUrl) {
    if (secondaryIconUrl != null) {
      ImageLoader.with(getContext())
          .loadWithShadowCircleTransform(secondaryIconUrl, secondaryIcon);
      secondaryIcon.setVisibility(View.VISIBLE);
    } else {
      secondaryIcon.setVisibility(View.GONE);
    }
  }

  public static class HomeMeta {
    private final String mainIcon;
    private final String secondaryIcon;
    private final String mainName;
    private final String secondaryName;
    private final boolean owner;
    private final List<cm.aptoide.pt.dataprovider.model.v7.store.Store.SocialChannel>
        socialChannels;
    private final long appsCount;
    private final long followersCount;
    private final long followingCount;
    private final String description;
    private final int themeColor;

    public HomeMeta(String mainIcon, String secondaryIcon, String mainName, String secondaryName,
        boolean owner,
        List<cm.aptoide.pt.dataprovider.model.v7.store.Store.SocialChannel> socialChannels,
        long appsCount, long followersCount, long followingCount, String description,
        int themeColor) {
      this.mainIcon = mainIcon;
      this.secondaryIcon = secondaryIcon;
      this.mainName = mainName;
      this.secondaryName = secondaryName;
      this.owner = owner;
      this.socialChannels = socialChannels;
      this.appsCount = appsCount;
      this.followersCount = followersCount;
      this.followingCount = followingCount;
      this.description = description;
      this.themeColor = themeColor;
    }

    public String getMainIcon() {
      return mainIcon;
    }

    public String getDescription() {
      return description;
    }

    public String getSecondaryIcon() {
      return secondaryIcon;
    }

    public String getMainName() {
      return mainName;
    }

    public String getSecondaryName() {
      return secondaryName;
    }

    public boolean isOwner() {
      return owner;
    }

    public List<cm.aptoide.pt.dataprovider.model.v7.store.Store.SocialChannel> getSocialChannels() {
      return socialChannels;
    }

    public long getAppsCount() {
      return appsCount;
    }

    public long getFollowersCount() {
      return followersCount;
    }

    public long getFollowingCount() {
      return followingCount;
    }

    public int getThemeColor() {
      return themeColor;
    }
  }
}
