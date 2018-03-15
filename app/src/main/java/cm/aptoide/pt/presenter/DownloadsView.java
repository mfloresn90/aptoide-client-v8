package cm.aptoide.pt.presenter;

import cm.aptoide.pt.install.Install;
import java.util.List;

public interface DownloadsView extends View {

  void showActiveDownloads(List<Install> downloads);

  void showStandByDownloads(List<Install> downloads);

  void showCompletedDownloads(List<Install> downloads);

  void showEmptyDownloadList();

  class DownloadViewModel {

    private final int progress;
    private final String appMd5;
    private final String appName;
    private final Status status;
    private final String icon;
    private final int speed;

    public DownloadViewModel(int progress, String appMd5, String appName, Status status,
        String icon, int speed) {
      this.progress = progress;
      this.appMd5 = appMd5;
      this.appName = appName;
      this.status = status;
      this.icon = icon;
      this.speed = speed;
    }

    public String getIcon() {
      return icon;
    }

    public int getSpeed() {
      return speed;
    }

    public String getAppMd5() {
      return appMd5;
    }

    public int getProgress() {
      return progress;
    }

    public String getAppName() {
      return appName;
    }

    public Status getStatus() {
      return status;
    }

    public enum Status {
      DOWNLOADING, STAND_BY, COMPLETED, ERROR
    }
  }
}
