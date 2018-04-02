package cm.aptoide.pt.social.commentslist;

import android.support.annotation.NonNull;
import cm.aptoide.accountmanager.AptoideAccountManager;
import cm.aptoide.pt.crashreports.CrashReport;
import cm.aptoide.pt.logger.Logger;
import cm.aptoide.pt.presenter.Presenter;
import cm.aptoide.pt.presenter.View;
import rx.Scheduler;
import rx.exceptions.OnErrorNotImplementedException;

/**
 * Created by jdandrade on 28/09/2017.
 */

public class PostCommentsPresenter implements Presenter {

  private final PostCommentsView view;
  private final Comments comments;
  private final CommentsNavigator commentsNavigator;
  private final String postId;
  private final Scheduler viewScheduler;
  private final CrashReport crashReporter;
  private final AptoideAccountManager accountManager;
  private boolean shouldShowCommentDialog;

  PostCommentsPresenter(@NonNull PostCommentsView commentsView, Comments comments,
      CommentsNavigator commentsNavigator, Scheduler viewScheduler, CrashReport crashReporter,
      String postId, AptoideAccountManager accountManager, boolean shouldShow) {
    this.view = commentsView;
    this.comments = comments;
    this.commentsNavigator = commentsNavigator;
    this.viewScheduler = viewScheduler;
    this.crashReporter = crashReporter;
    this.postId = postId;
    this.accountManager = accountManager;
    this.shouldShowCommentDialog = shouldShow;
  }

  @Override public void present() {

    onCreateShowComments();

    onPullRefreshShowComments();

    onBottomReachedShowMoreComments();

    onCommentReplyShowDialog();

    onPostReplyShowDialog();

    onDialogResultShowComment();

    onDialogResultErrorNavigate();
  }

  private void onDialogResultErrorNavigate() {
    view.getLifecycle()
        .filter(lifecycleEvent -> lifecycleEvent.equals(View.LifecycleEvent.CREATE))
        .flatMap(__ -> commentsNavigator.commentDialogOnError())
        .observeOn(viewScheduler)
        .doOnNext(postId -> commentsNavigator.navigateToPostCommentInTimelineError(postId))
        .compose(view.bindUntilEvent(View.LifecycleEvent.DESTROY))
        .subscribe(__ -> {
        }, throwable -> {
          throw new OnErrorNotImplementedException(throwable);
        });
  }

  private void onDialogResultShowComment() {
    view.getLifecycle()
        .filter(lifecycleEvent -> lifecycleEvent.equals(View.LifecycleEvent.CREATE))
        .flatMap(__ -> commentsNavigator.commentDialogResult()
            .flatMapSingle(commentDataWrapper -> accountManager.accountStatus()
                .first()
                .toSingle()
                .map(account -> comments.mapToComment(commentDataWrapper, account)))
            .observeOn(viewScheduler)
            .doOnNext(comment -> {
              view.showCommentSubmittedMessage();
              view.showNewComment(comment);
              commentsNavigator.navigateToPostCommentInTimeline(postId, comment.getBody());
            })
            .doOnError(throwable -> Logger.w(this.getClass()
                .getSimpleName(), "present: ", throwable))
            .retry())
        .compose(view.bindUntilEvent(View.LifecycleEvent.DESTROY))
        .subscribe(__ -> {
        }, throwable -> {
          throw new OnErrorNotImplementedException(throwable);
        });
  }

  private void onPostReplyShowDialog() {
    view.getLifecycle()
        .filter(lifecycleEvent -> lifecycleEvent.equals(View.LifecycleEvent.CREATE))
        .flatMap(created -> view.repliesPost())
        .doOnNext(__ -> commentsNavigator.showCommentDialog(postId))
        .compose(view.bindUntilEvent(View.LifecycleEvent.DESTROY))
        .subscribe(__ -> {
        }, throwable -> {
          throw new OnErrorNotImplementedException(throwable);
        });
  }

  private void onCommentReplyShowDialog() {
    view.getLifecycle()
        .filter(lifecycleEvent -> lifecycleEvent.equals(View.LifecycleEvent.CREATE))
        .flatMap(created -> view.repliesComment()
            .doOnNext(commentId -> commentsNavigator.showCommentDialog(postId, commentId))
            .retry())
        .compose(view.bindUntilEvent(View.LifecycleEvent.DESTROY))
        .subscribe(commentId -> {
        }, throwable -> {
          throw new OnErrorNotImplementedException(throwable);
        });
  }

  private void onBottomReachedShowMoreComments() {
    view.getLifecycle()
        .filter(lifecycleEvent -> lifecycleEvent.equals(View.LifecycleEvent.CREATE))
        .flatMap(created -> view.reachesBottom()
            .filter(__ -> comments.hasMore())
            .observeOn(viewScheduler)
            .doOnNext(__ -> view.showLoadMoreProgressIndicator())
            .flatMapSingle(__ -> comments.getNextComments(postId))
            .filter(comments -> !comments.isEmpty())
            .observeOn(viewScheduler)
            .doOnNext(comments -> view.showMoreComments(comments))
            .doOnNext(__ -> view.hideLoadMoreProgressIndicator())
            .retry())
        .compose(view.bindUntilEvent(View.LifecycleEvent.DESTROY))
        .subscribe(comments -> {
        }, throwable -> crashReporter.log(throwable));
  }

  private void onPullRefreshShowComments() {
    view.getLifecycle()
        .filter(lifecycleEvent -> lifecycleEvent.equals(View.LifecycleEvent.CREATE))
        .flatMap(created -> view.refreshes()
            .flatMapSingle(__ -> comments.getFreshComments(postId))
            .observeOn(viewScheduler)
            .doOnNext(comments -> view.showComments(comments))
            .doOnNext(comments -> view.hideRefresh())
            .retry())
        .compose(view.bindUntilEvent(View.LifecycleEvent.DESTROY))
        .subscribe(comments -> {
        }, throwable -> crashReporter.log(throwable));
  }

  private void onCreateShowComments() {
    view.getLifecycle()
        .filter(lifecycleEvent -> lifecycleEvent.equals(View.LifecycleEvent.CREATE))
        .filter(__ -> shouldShowCommentDialog)
        .doOnNext(__ -> {
          shouldShowCommentDialog = false;
          commentsNavigator.showCommentDialog(postId);
        })
        .compose(view.bindUntilEvent(View.LifecycleEvent.DESTROY))
        .subscribe(__ -> {
        }, throwable -> crashReporter.log(throwable));

    view.getLifecycle()
        .filter(lifecycleEvent -> lifecycleEvent.equals(View.LifecycleEvent.CREATE))
        .doOnNext(created -> view.showLoading())
        .flatMapSingle(created -> comments.getComments(postId))
        .observeOn(viewScheduler)
        .doOnNext(comments -> view.showComments(comments))
        .doOnNext(comments -> view.hideLoading())
        .compose(view.bindUntilEvent(View.LifecycleEvent.DESTROY))
        .subscribe(comments -> {
        }, throwable -> crashReporter.log(throwable));
  }
}
