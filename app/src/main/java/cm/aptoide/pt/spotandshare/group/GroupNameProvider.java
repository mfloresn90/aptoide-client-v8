package cm.aptoide.pt.spotandshare.group;

import rx.Single;

/**
 * Created by filipe on 06-04-2017.
 */

public interface GroupNameProvider {

  Single<String> getName();
}
