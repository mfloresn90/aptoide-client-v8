package cm.aptoide.pt.analytics.analytics;

import rx.Completable;

/**
 * Created by trinkes on 11/01/2018.
 */

public interface AptoideBiEventService {
  Completable send(Event event);
}
