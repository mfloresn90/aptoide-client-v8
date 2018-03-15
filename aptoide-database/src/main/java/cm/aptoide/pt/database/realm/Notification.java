package cm.aptoide.pt.database.realm;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

/**
 * Created by trinkes on 10/05/2017.
 */

public class Notification extends RealmObject {
  public final static String KEY = "key";
  public static final int NOT_DISMISSED = -1;

  private Long expire;
  @PrimaryKey private String key;
  private String abTestingGroup;
  private String body;
  private int campaignId;
  private String img;
  private String lang;
  private String title;
  private String url;
  private String urlTrack;
  private String notificationCenterUrlTrack;
  private int type;
  private long timeStamp;
  private long dismissed;
  private String appName;
  private String graphic;
  private String ownerId;

  public Notification(Long expire, String abTestingGroup, String body, int campaignId, String img,
      String lang, String title, String url, String urlTrack, String notificationCenterUrlTrack,
      long timeStamp, int type, long dismissed, String appName, String graphic, String ownerId) {
    this.expire = expire;
    this.body = body;
    this.img = img;
    this.title = title;
    this.url = url;
    this.notificationCenterUrlTrack = notificationCenterUrlTrack;
    this.type = type;
    this.abTestingGroup = abTestingGroup;
    this.campaignId = campaignId;
    this.lang = lang;
    this.urlTrack = urlTrack;
    this.timeStamp = timeStamp;
    this.dismissed = dismissed;
    this.appName = appName;
    this.graphic = graphic;
    this.ownerId = ownerId;
    key = String.valueOf(timeStamp + type);
  }

  public Notification() {
    expire = 0L;
  }

  public Long getExpire() {
    return expire;
  }

  public String getAppName() {
    return appName;
  }

  public String getGraphic() {
    return graphic;
  }

  public long getDismissed() {
    return dismissed;
  }

  public int getType() {
    return type;
  }

  public String getAbTestingGroup() {
    return abTestingGroup;
  }

  public String getBody() {
    return body;
  }

  public int getCampaignId() {
    return campaignId;
  }

  public String getImg() {
    return img;
  }

  public String getLang() {
    return lang;
  }

  public String getTitle() {
    return title;
  }

  public String getUrl() {
    return url;
  }

  public String getUrlTrack() {
    return urlTrack;
  }

  public long getTimeStamp() {
    return timeStamp;
  }

  public String getOwnerId() {
    return ownerId;
  }

  public String getKey() {
    return key;
  }

  public boolean isDismissed() {
    return dismissed != NOT_DISMISSED;
  }

  public void setDismissed(long dismissed) {
    this.dismissed = dismissed;
  }

  public String getNotificationCenterUrlTrack() {
    return notificationCenterUrlTrack;
  }
}
