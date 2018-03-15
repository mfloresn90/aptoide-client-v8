package cm.aptoide.pt.social.view.viewholder;

import android.graphics.Color;
import android.text.ParcelableSpan;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.widget.Button;
import cm.aptoide.pt.R;
import cm.aptoide.pt.social.data.CardTouchEvent;
import cm.aptoide.pt.social.data.TimelineStatsTouchEvent;
import cm.aptoide.pt.social.view.TimelineUser;
import cm.aptoide.pt.view.spannable.SpannableFactory;
import rx.subjects.PublishSubject;

/**
 * Created by jdandrade on 05/07/2017.
 */

public class TimelineStatsViewHolder extends PostViewHolder<TimelineUser> {
  private final SpannableFactory spannableFactory;
  private final PublishSubject<CardTouchEvent> cardTouchEventPublishSubject;
  private Button followers;
  private Button following;

  public TimelineStatsViewHolder(View view, SpannableFactory spannableFactory,
      PublishSubject<CardTouchEvent> cardTouchEventPublishSubject) {
    super(view, cardTouchEventPublishSubject);
    this.spannableFactory = spannableFactory;
    this.cardTouchEventPublishSubject = cardTouchEventPublishSubject;
    this.followers = (Button) itemView.findViewById(R.id.followers);
    this.following = (Button) itemView.findViewById(R.id.following);
  }

  @Override public void setPost(TimelineUser card, int position) {
    followers.setText(spannableFactory.createSpan(itemView.getContext()
            .getString(R.string.timeline_button_followers, card.getFollowers()),
        new ParcelableSpan[] { new ForegroundColorSpan(Color.BLACK) },
        String.valueOf(card.getFollowers()), String.valueOf(card.getFollowers())));
    following.setText(spannableFactory.createSpan(itemView.getContext()
            .getString(R.string.timeline_button_followed, card.getFollowing()),
        new ParcelableSpan[] { new ForegroundColorSpan(Color.BLACK) },
        String.valueOf(card.getFollowing()), String.valueOf(card.getFollowing())));

    followers.setOnClickListener(click -> cardTouchEventPublishSubject.onNext(
        new TimelineStatsTouchEvent(card, TimelineStatsTouchEvent.ButtonClicked.FOLLOWERS,
            CardTouchEvent.Type.TIMELINE_STATS, position)));
    following.setOnClickListener(click -> cardTouchEventPublishSubject.onNext(
        new TimelineStatsTouchEvent(card, TimelineStatsTouchEvent.ButtonClicked.FOLLOWING,
            CardTouchEvent.Type.TIMELINE_STATS, position)));
  }
}
