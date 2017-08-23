package cm.aptoide.pt.dataprovider.model.v7.timeline;

import cm.aptoide.pt.dataprovider.model.v7.listapp.App;
import cm.aptoide.pt.dataprovider.model.v7.store.Store;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Date;
import java.util.List;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@EqualsAndHashCode(exclude = { "store", "apps", "latestUpdate" }) public class StoreLatestApps
    implements TimelineCard {

  @Getter private final String cardId;
  @Getter private final Store store;
  @Getter private final List<App> apps;
  @Getter private final Ab ab;
  private final Urls urls;

  private Date latestUpdate;

  @JsonCreator
  public StoreLatestApps(@JsonProperty("uid") String cardId, @JsonProperty("store") Store store,
      @JsonProperty("apps") List<App> apps, @JsonProperty("ab") Ab ab,
      @JsonProperty("urls") Urls urls) {
    this.cardId = cardId;
    this.store = store;
    this.apps = apps;
    this.ab = ab;
    this.urls = urls;
  }

  @Override public Urls getUrls() {
    return urls;
  }

  public Date getLatestUpdate() {
    if (latestUpdate == null) {
      for (App app : apps) {
        if (latestUpdate == null || (app.getUpdated() != null
            && app.getUpdated()
            .getTime() > latestUpdate.getTime())) {
          latestUpdate = app.getUpdated();
        }
      }
    }
    return latestUpdate;
  }
}
