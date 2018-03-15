package cm.aptoide.pt.social.data;

import java.util.ArrayList;
import java.util.List;
import rx.Single;

/**
 * Created by jdandrade on 01/08/2017.
 */

public class TimelinePostsRepository {

  private final PostsRemoteDataSource postsRemoteDataSource;

  private List<Post> cachedPosts;

  public TimelinePostsRepository(PostsRemoteDataSource postsRemoteDataSource) {
    this.postsRemoteDataSource = postsRemoteDataSource;
  }

  public Single<List<Post>> getCards() {
    if (cachedPosts != null) {
      return Single.just(new ArrayList<>(cachedPosts));
    }
    return postsRemoteDataSource.getCards()
        .doOnSuccess(posts -> cachedPosts = posts)
        .map(posts -> new ArrayList<>(posts));
  }

  public Single<List<Post>> getFreshCards() {
    return postsRemoteDataSource.getCards()
        .doOnSuccess(posts -> cachedPosts = posts);
  }

  public Single<List<Post>> getFreshCards(String postId) {
    return postsRemoteDataSource.getCards(postId)
        .doOnSuccess(posts -> cachedPosts = posts);
  }

  public Single<List<Post>> getNextCards() {
    return postsRemoteDataSource.getNextCards()
        .doOnSuccess(posts -> cachedPosts.addAll(posts));
  }

  public void clearLoading() {
    postsRemoteDataSource.clearLoading();
  }
}
