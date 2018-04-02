package cm.aptoide.pt.analytics.analytics;

import android.content.Context;
import cm.aptoide.pt.logger.Logger;
import com.flurry.android.FlurryAgent;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by trinkes on 11/01/2018.
 */

public class FlurryEventLogger implements EventLogger, SessionLogger {
  private static final String TAG = FlurryEventLogger.class.getSimpleName();
  private Context context;

  public FlurryEventLogger(Context context) {
    this.context = context;
  }

  @Override
  public void log(String eventName, Map<String, Object> data, AnalyticsManager.Action action,
      String context) {
    if (data != null) {
      FlurryAgent.logEvent(eventName, map(data));
    } else {
      FlurryAgent.logEvent(eventName);
    }
    Logger.d(TAG, "log() called with: "
        + "eventName = ["
        + eventName
        + "], data = ["
        + data
        + "], action = ["
        + action
        + "], context = ["
        + context
        + "]");
  }

  @Override public void setup() {

  }

  private Map<String, String> map(Map<String, Object> data) {
    Map<String, String> map = new HashMap<>();
    for (Map.Entry<String, Object> entry : data.entrySet()) {
      if (entry.getValue() != null) {
        map.put(entry.getKey(), entry.getValue()
            .toString());
      }
    }
    return map;
  }

  @Override public void startSession() {
    FlurryAgent.onStartSession(context);
  }

  @Override public void endSession() {
    FlurryAgent.onEndSession(context);
  }
}
