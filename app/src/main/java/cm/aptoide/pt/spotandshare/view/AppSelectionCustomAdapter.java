package cm.aptoide.pt.spotandshare.view;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import cm.aptoide.pt.R;
import cm.aptoide.pt.spotandshare.app.AppViewModel;
import cm.aptoide.pt.spotandshare.presenter.AppSelectionView;
import java.util.List;

/**
 * Created by filipegoncalves on 28-07-2016.
 */
public class AppSelectionCustomAdapter extends BaseAdapter {

  private Context context;
  private List<AppViewModel> appsList;
  private ViewHolder viewHolder;
  private View view;
  private AppSelectionView.AppSelectionListener listener;

  public AppSelectionCustomAdapter(AppSelectionView appSelectionView, Context context,
      List<AppViewModel> appsList, boolean isHotspot) {
    this.context = context;
    this.appsList = appsList;
  }

  public void setListener(AppSelectionView.AppSelectionListener listener) {
    this.listener = listener;
  }

  public void removeListener() {
    listener = null;
  }

  @Override public final int getCount() {

    return appsList.size();
  }

  @Override public AppViewModel getItem(int position) {
    return appsList.get(position);
  }

  @Override public long getItemId(int position) {
    return position;
  }

  @Override public View getView(int position, View convertView, ViewGroup parent) {
    viewHolder = null;
    if (convertView == null) {

      viewHolder = new ViewHolder();

      LayoutInflater layoutInflater =
          (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
      view = layoutInflater.inflate(R.layout.activity_spot_and_share_app_selection_item, parent,
          false);

      viewHolder.appImageIcon = (ImageView) view.findViewById(R.id.highwayGridViewItemIcon);
      viewHolder.appNameLabel = (TextView) view.findViewById(R.id.highwayGridViewItemName);
      viewHolder.linearLayout = (LinearLayout) view.findViewById(R.id.HighwayGridViewItem);
    } else {
      view = convertView;
      viewHolder = (ViewHolder) convertView.getTag();
    }
    viewHolder.position = position;
    viewHolder.appNameLabel.setText(appsList.get(position)
        .getAppName());
    viewHolder.appImageIcon.setImageDrawable(appsList.get(position)
        .getImageIcon());

    if (appsList.get(position)
        .isSelected()) {
      viewHolder.linearLayout.setBackgroundColor(context.getResources()
          .getColor(R.color.grey_fog_normal));
      //            notifyDataSetChanged();
    } else {
      viewHolder.linearLayout.setBackgroundColor(context.getResources()
          .getColor(R.color.white));
    }
    view.setTag(viewHolder);

    viewHolder.linearLayout.setOnClickListener(new MyOnClickListener(position));

    return view;
  }

  public static class ViewHolder {
    ImageView appImageIcon;
    TextView appNameLabel;
    LinearLayout linearLayout;
    int position;
  }

  class MyOnClickListener implements View.OnClickListener {
    private final int position;

    public MyOnClickListener(int position) {
      this.position = position;
    }

    public void onClick(View v) {
      if (listener != null) {
        listener.onAppSelected(getItem(position));
      }
    }
  }
}
