package cm.aptoide.pt.spotandshare.transference;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.StatFs;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v7.app.NotificationCompat;
import cm.aptoide.pt.R;
import cm.aptoide.pt.logger.Logger;
import cm.aptoide.pt.spotandshare.DataHolder;
import cm.aptoide.pt.spotandshare.Utils;
import cm.aptoide.pt.spotandshare.app.App;
import cm.aptoide.pt.spotandshare.connection.HostsCallbackManager;
import cm.aptoide.pt.spotandshare.socket.entities.AndroidAppInfo;
import cm.aptoide.pt.spotandshare.socket.entities.FileInfo;
import cm.aptoide.pt.spotandshare.socket.interfaces.FileClientLifecycle;
import cm.aptoide.pt.spotandshare.socket.interfaces.FileLifecycleProvider;
import cm.aptoide.pt.spotandshare.socket.interfaces.FileServerLifecycle;
import cm.aptoide.pt.spotandshare.socket.interfaces.OnError;
import cm.aptoide.pt.spotandshare.socket.interfaces.SocketBinder;
import cm.aptoide.pt.spotandshare.socket.message.client.AptoideMessageClientSocket;
import cm.aptoide.pt.spotandshare.socket.message.interfaces.StorageCapacity;
import cm.aptoide.pt.spotandshare.socket.message.messages.v1.RequestPermissionToSend;
import cm.aptoide.pt.spotandshare.socket.message.server.AptoideMessageServerSocket;
import cm.aptoide.pt.spotandshare.view.TransferRecordActivity;
import cm.aptoide.pt.utils.AptoideUtils;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class ServerService extends Service {

  public static final int INSTALL_APP_NOTIFICATION_REQUEST_CODE = 147;
  public static final String TAG = ServerService.class.getSimpleName();
  private final int PROGRESS_SPLIT_SIZE = 10;
  private final SocketBinder socketBinder = Utils.Socket.newDefaultSocketBinder();
  private NotificationManagerCompat mNotifyManager;
  private FileLifecycleProvider<AndroidAppInfo> fileLifecycleProvider;
  private List<App> listOfApps;
  private AptoideMessageServerSocket aptoideMessageServerSocket;
  private AptoideMessageClientSocket aptoideMessageClientSocket;
  private OnError<IOException> onError = e -> {
    System.err.println("OnError lacks implementation!");

    if (mNotifyManager != null) {
      mNotifyManager.cancelAll();
    }

    setInitialApConfig();//to not interfere with recovering wifi state

    Intent i = new Intent();
    i.setAction("SERVER_DISCONNECT");
    sendBroadcast(i);
  };

  @Override public void onCreate() {
    super.onCreate();
    if (mNotifyManager == null) {
      mNotifyManager = NotificationManagerCompat.from(getApplicationContext());
    }

    fileLifecycleProvider = new FileLifecycleProvider<AndroidAppInfo>() {
      @Override public FileServerLifecycle<AndroidAppInfo> newFileServerLifecycle() {
        return new FileServerLifecycle<AndroidAppInfo>() {

          private ProgressFilter progressFilter;
          private AndroidAppInfo androidAppInfo;

          @Override public void onStartSending(AndroidAppInfo androidAppInfo) {

            this.androidAppInfo = androidAppInfo;
            progressFilter = new ProgressFilter(PROGRESS_SPLIT_SIZE);

            createSendNotification();

            Intent i = new Intent();
            i.putExtra("isSent", false);
            i.putExtra("needReSend", false);
            i.putExtra("appName", androidAppInfo.getAppName());
            i.putExtra("packageName", androidAppInfo.getPackageName());
            i.putExtra("positionToReSend", 100000);
            i.setAction("SENDAPP");
            sendBroadcast(i);
          }

          @Override public void onFinishSending(AndroidAppInfo androidAppInfo) {
            Logger.d(TAG, "Server : finished sending " + androidAppInfo);

            finishSendNotification(androidAppInfo);

            Intent i = new Intent();
            i.putExtra("isSent", true);
            i.putExtra("needReSend", false);
            i.putExtra("appName", androidAppInfo.getAppName());
            i.putExtra("packageName", androidAppInfo.getPackageName());
            i.putExtra("positionToReSend", 100000);
            i.setAction("SENDAPP");
            sendBroadcast(i);
          }

          @Override public void onError(IOException e) {
            Logger.d(TAG, "Fell on error Server !! ");
            e.printStackTrace();

            if (mNotifyManager != null && androidAppInfo != null) {
              mNotifyManager.cancel(androidAppInfo.getPackageName()
                  .hashCode());
            }
            Intent i = new Intent();
            i.setAction("ERRORSENDING");
            sendBroadcast(i);
          }

          @Override public void onProgressChanged(AndroidAppInfo androidAppInfo, float progress) {
            if (progressFilter.shouldUpdate(progress)) {
              int actualProgress = Math.round(progress * 100);
              showSendProgress(androidAppInfo.getAppName(), actualProgress, androidAppInfo);
            }
          }
        };
      }

      @Override public FileClientLifecycle<AndroidAppInfo> newFileClientLifecycle() {
        return new FileClientLifecycle<AndroidAppInfo>() {

          private ProgressFilter progressFilter;
          private AndroidAppInfo androidAppInfo;

          @Override public void onStartReceiving(AndroidAppInfo androidAppInfo) {
            this.androidAppInfo = androidAppInfo;

            progressFilter = new ProgressFilter(PROGRESS_SPLIT_SIZE);

            createReceiveNotification(androidAppInfo.getAppName());

            Intent i = new Intent();
            i.putExtra("FinishedReceiving", false);
            i.putExtra("appName", androidAppInfo.getAppName());
            i.setAction("RECEIVEAPP");
            sendBroadcast(i);
          }

          @Override public void onFinishReceiving(AndroidAppInfo androidAppInfo) {

            finishReceiveNotification(androidAppInfo.getApk()
                .getFilePath(), androidAppInfo.getPackageName(), androidAppInfo);

            Intent i = new Intent();
            i.putExtra("FinishedReceiving", true);
            i.putExtra("needReSend", false);
            i.putExtra("appName", androidAppInfo.getAppName());
            i.putExtra("packageName", androidAppInfo.getPackageName());
            i.putExtra("filePath", androidAppInfo.getApk()
                .getFilePath());
            i.setAction("RECEIVEAPP");
            sendBroadcast(i);
          }

          @Override public void onError(IOException e) {

            if (mNotifyManager != null && androidAppInfo != null) {
              mNotifyManager.cancel(androidAppInfo.getPackageName()
                  .hashCode());
            }

            // Não ta facil perceber pk é k isto cai aqui quando só há um cliente, martelo ftw :/
            if (aptoideMessageServerSocket.getAptoideMessageControllers()
                .size() <= 1) {
              return;
            }

            e.printStackTrace();

            Intent i = new Intent();
            i.setAction("ERRORRECEIVING");
            sendBroadcast(i);
          }

          @Override public void onProgressChanged(AndroidAppInfo androidAppInfo, float progress) {

            if (progressFilter.shouldUpdate(progress)) {
              int actualProgress = Math.round(progress * 100);
              showReceiveProgress(androidAppInfo.getAppName(), actualProgress, androidAppInfo);
            }
          }
        };
      }
    };
  }

  @Override public int onStartCommand(Intent intent, int flags, int startId) {
    if (intent != null) {

      if (intent.getAction() != null && intent.getAction()
          .equals("RECEIVE")) {
        final String externalStoragepath = intent.getStringExtra("ExternalStoragePath");

        Logger.d(TAG, "Going to start serving");
        HostsCallbackManager hostsCallbackManager;
        if (intent.getExtras()
            .containsKey("autoShareFilePath")) {
          String autoShareFilePath = intent.getStringExtra("autoShareFilePath");
          hostsCallbackManager =
              new HostsCallbackManager(this.getApplicationContext(), autoShareFilePath);
        } else {
          hostsCallbackManager = new HostsCallbackManager(this.getApplicationContext());
        }

        aptoideMessageServerSocket =
            new AptoideMessageServerSocket(55555, Integer.MAX_VALUE, Integer.MAX_VALUE);
        aptoideMessageServerSocket.setHostsChangedCallbackCallback(hostsCallbackManager);
        aptoideMessageServerSocket.startAsync();

        StorageCapacity storageCapacity = new StorageCapacity() {
          @Override public boolean hasCapacity(long bytes) {
            long availableSpace = -1L;
            StatFs stat = new StatFs(externalStoragepath);
            availableSpace = (long) stat.getAvailableBlocks() * (long) stat.getBlockSize();
            return availableSpace > bytes;
          }
        };

        // TODO: 07-04-2017 asdsadefeqf neuro Filipe onError sff lol
        aptoideMessageClientSocket =
            new AptoideMessageClientSocket("192.168.43.1", 55555, externalStoragepath,
                storageCapacity, fileLifecycleProvider, socketBinder, onError, Integer.MAX_VALUE);
        aptoideMessageClientSocket.startAsync();
      } else if (intent.getAction() != null && intent.getAction()
          .equals("SEND")) {
        Bundle b = intent.getBundleExtra("bundle");

        listOfApps = b.getParcelableArrayList("listOfAppsToInstall");

        for (int i = 0; i < listOfApps.size(); i++) {
          String filePath = listOfApps.get(i)
              .getFilePath();
          String appName = listOfApps.get(i)
              .getAppName();
          String packageName = listOfApps.get(i)
              .getPackageName();
          String obbsFilePath = listOfApps.get(i)
              .getObbsFilePath();

          File apk = new File(filePath);

          File mainObb = null;
          File patchObb = null;

          List<FileInfo> fileInfoList = getFileInfo(filePath, obbsFilePath);

          final AndroidAppInfo appInfo = new AndroidAppInfo(appName, packageName, fileInfoList);

          AptoideUtils.ThreadU.runOnIoThread(new Runnable() {
            @Override public void run() {
              aptoideMessageClientSocket.send(
                  new RequestPermissionToSend(aptoideMessageClientSocket.getLocalhost(), appInfo));
            }
          });
        }
      } else if (intent.getAction() != null && intent.getAction()
          .equals("SHUTDOWN_SERVER")) {
        if (aptoideMessageServerSocket != null) {
          aptoideMessageClientSocket.disable();
          aptoideMessageServerSocket.shutdown(new Runnable() {
            @Override public void run() {
              if (mNotifyManager != null) {
                mNotifyManager.cancelAll();
              }

              setInitialApConfig();//to not interfere with recovering wifi state

              Intent i = new Intent();
              i.setAction("SERVER_DISCONNECT");
              sendBroadcast(i);
            }
          });
        }
      }
    }
    return START_STICKY;
  }

  @Nullable @Override public IBinder onBind(Intent intent) {
    return null;
  }

  private void createReceiveNotification(String receivingAppName) {

    NotificationCompat.Builder mBuilderReceive = new NotificationCompat.Builder(this);
    mBuilderReceive.setContentTitle(this.getResources()
        .getString(R.string.spot_share) + " - " + this.getResources()
        .getString(R.string.receive))
        .setContentText(this.getResources()
            .getString(R.string.receiving) + " " + receivingAppName);
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      mBuilderReceive.setSmallIcon(R.drawable.ic_stat_aptoide_notification);
    } else {
      mBuilderReceive.setSmallIcon(R.mipmap.ic_launcher);
    }
  }

  private void finishReceiveNotification(String receivedApkFilePath, String packageName,
      AndroidAppInfo androidAppInfo) {
    NotificationCompat.Builder mBuilderReceive = new NotificationCompat.Builder(this);
    mBuilderReceive.setContentTitle(this.getResources()
        .getString(R.string.spot_share) + " - " + this.getResources()
        .getString(R.string.receive))
        .setContentText(this.getResources()
            .getString(R.string.transfCompleted))
        .setSmallIcon(android.R.drawable.stat_sys_download_done)
        .setProgress(0, 0, false)
        .setAutoCancel(true);

    Intent intent = new Intent();
    intent.setAction("INSTALL_APP_NOTIFICATION");
    intent.putExtra("filePath", receivedApkFilePath);
    intent.putExtra("packageName", packageName);
    PendingIntent contentIntent =
        PendingIntent.getBroadcast(this, INSTALL_APP_NOTIFICATION_REQUEST_CODE, intent,
            PendingIntent.FLAG_CANCEL_CURRENT);

    mBuilderReceive.setContentIntent(contentIntent);
    if (mNotifyManager == null) {
      mNotifyManager = NotificationManagerCompat.from(getApplicationContext());
    }
    mNotifyManager.notify(androidAppInfo.getPackageName()
        .hashCode(), mBuilderReceive.build());
  }

  private void showReceiveProgress(String receivingAppName, int actual,
      AndroidAppInfo androidAppInfo) {

    NotificationCompat.Builder mBuilderReceive = new NotificationCompat.Builder(this);
    mBuilderReceive.setContentTitle(this.getResources()
        .getString(R.string.spot_share) + " - " + this.getResources()
        .getString(R.string.receive))
        .setContentText(this.getResources()
            .getString(R.string.receiving) + " " + receivingAppName);
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      mBuilderReceive.setSmallIcon(R.drawable.ic_stat_aptoide_notification);
    } else {
      mBuilderReceive.setSmallIcon(R.mipmap.ic_launcher);
    }

    mBuilderReceive.setProgress(100, actual, false);
    if (mNotifyManager == null) {
      mNotifyManager = NotificationManagerCompat.from(getApplicationContext());
    }
    mNotifyManager.notify(androidAppInfo.getPackageName()
        .hashCode(), mBuilderReceive.build());
  }

  private void createSendNotification() {

    NotificationCompat.Builder mBuilderSend = new NotificationCompat.Builder(this);
    mBuilderSend.setContentTitle(this.getResources()
        .getString(R.string.spot_share) + " - " + this.getResources()
        .getString(R.string.send))
        .setContentText(this.getResources()
            .getString(R.string.preparingSend));
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      mBuilderSend.setSmallIcon(R.drawable.ic_stat_aptoide_notification);
    } else {
      mBuilderSend.setSmallIcon(R.mipmap.ic_launcher);
    }
  }

  private void finishSendNotification(AndroidAppInfo androidAppInfo) {

    NotificationCompat.Builder mBuilderSend = new NotificationCompat.Builder(this);
    mBuilderSend.setContentTitle(this.getResources()
        .getString(R.string.spot_share) + " - " + this.getResources()
        .getString(R.string.send))
        .setContentText(this.getResources()
            .getString(R.string.transfCompleted))
        .setSmallIcon(android.R.drawable.stat_sys_download_done)
        .setProgress(0, 0, false)
        .setAutoCancel(true);
    if (mNotifyManager == null) {
      mNotifyManager = NotificationManagerCompat.from(getApplicationContext());
    }
    mNotifyManager.notify(androidAppInfo.getPackageName()
        .hashCode(), mBuilderSend.build());
  }

  private void showSendProgress(String sendingAppName, int actual, AndroidAppInfo androidAppInfo) {

    NotificationCompat.Builder mBuilderSend = new NotificationCompat.Builder(this);
    mBuilderSend.setContentTitle(this.getResources()
        .getString(R.string.spot_share) + " - " + this.getResources()
        .getString(R.string.send))
        .setContentText(this.getResources()
            .getString(R.string.sending) + " " + sendingAppName);
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      mBuilderSend.setSmallIcon(R.drawable.ic_stat_aptoide_notification);
    } else {
      mBuilderSend.setSmallIcon(R.mipmap.ic_launcher);
    }

    mBuilderSend.setProgress(100, actual, false);
    if (mNotifyManager == null) {
      mNotifyManager = NotificationManagerCompat.from(getApplicationContext());
    }
    mNotifyManager.notify(androidAppInfo.getPackageName()
        .hashCode(), mBuilderSend.build());
  }

  public List<FileInfo> getFileInfo(String filePath, String obbsFilePath) {
    List<FileInfo> fileInfoList = new ArrayList<>();
    File apk = new File(filePath);
    FileInfo apkFileInfo = new FileInfo(apk);
    fileInfoList.add(apkFileInfo);

    if (!obbsFilePath.equals("noObbs")) {
      File obbFolder = new File(obbsFilePath);
      File[] list = obbFolder.listFiles();
      if (list != null) {
        if (list.length > 0) {
          for (int i = 0; i < list.length; i++) {
            fileInfoList.add(new FileInfo(list[i]));
          }
        }
      }
    }

    return fileInfoList;
  }

  /**
   * @deprecated Duplicated! {@link TransferRecordActivity#setInitialApConfig()}
   */
  @Deprecated public void setInitialApConfig() {
    WifiManager wifimanager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);

    Method[] methods = wifimanager.getClass()
        .getDeclaredMethods();
    WifiConfiguration wc = DataHolder.getInstance()
        .getWcOnJoin();
    for (Method m : methods) {
      if (m.getName()
          .equals("setWifiApConfiguration")) {

        try {
          Method setConfigMethod = wifimanager.getClass()
              .getMethod("setWifiApConfiguration", WifiConfiguration.class);
          Logger.d(TAG, "Re-seting the wifiAp configuration to what it was before !!! ");
          setConfigMethod.invoke(wifimanager, wc);
        } catch (NoSuchMethodException e) {
          e.printStackTrace();
        } catch (IllegalAccessException e) {
          e.printStackTrace();
        } catch (InvocationTargetException e) {
          e.printStackTrace();
        }
      }
      if (m.getName()
          .equals("setWifiApEnabled")) {

        try {
          Logger.d(TAG, "Desligar o hostpot ");
          m.invoke(wifimanager, wc, false);
        } catch (IllegalAccessException e) {
          e.printStackTrace();
        } catch (InvocationTargetException e) {
          e.printStackTrace();
        }
      }
    }
  }
}
