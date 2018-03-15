package cm.aptoide.pt.spotandshare.view;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import cm.aptoide.pt.R;
import cm.aptoide.pt.spotandshare.presenter.TransferRecordView;
import cm.aptoide.pt.spotandshare.transference.TransferRecordItem;
import java.util.List;

/**
 * Created by filipegoncalves on 12-08-2016.
 */
public class TransferRecordCustomAdapter extends BaseAdapter {

  private Context context;
  private ViewHolder viewHolder;
  private List<TransferRecordItem> listOfItems;
  private MyOnClickListenerToInstall myOnClickListenerToInstall;
  private MyOnClickListenerToDelete myOnClickListenerToDelete;
  private TransferRecordView.TransferRecordListener listener;

  public TransferRecordCustomAdapter(Context context, List<TransferRecordItem> listOfItems) {
    this.context = context;
    this.listOfItems = listOfItems;
  }

  public void setListener(TransferRecordView.TransferRecordListener listener) {
    this.listener = listener;
  }

  @Override public int getCount() {
    return listOfItems.size();
  }

  @Override public TransferRecordItem getItem(int position) {

    return listOfItems.get(position);
  }

  @Override public long getItemId(int position) {
    return position;
  }

  @Override public View getView(int position, View convertView, ViewGroup parent) {
    viewHolder = null;
    if (convertView == null) {

      viewHolder = new ViewHolder();
      int type = getItemViewType(position);
      LayoutInflater layoutInflater =
          (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

      if (type == 0) {
        convertView = layoutInflater.inflate(
            R.layout.activity_spot_and_share_transfer_record_received_app_row, parent, false);
        viewHolder.transfRecRecvRowIcons =
            (RelativeLayout) convertView.findViewById(R.id.transfRecRecvRowIcons);
        viewHolder.transfRecRecvRowDeletedMessage =
            (LinearLayout) convertView.findViewById(R.id.transfRecRecvRowDeletedMessage);
        viewHolder.deleteFile = (Button) convertView.findViewById(R.id.transfRecRowDelete);
        viewHolder.installButton = (Button) convertView.findViewById(R.id.transfRecRowInstall);
      } else {
        convertView =
            layoutInflater.inflate(R.layout.activity_spot_and_share_transfer_record_sent_app_row,
                parent, false);

        viewHolder.reSendButton = (Button) convertView.findViewById(R.id.transfRecReSendButton);
      }

      viewHolder.appImageIcon = (ImageView) convertView.findViewById(R.id.transfRecRowImage);
      viewHolder.appNameLabel = (TextView) convertView.findViewById(R.id.transfRecRowText);
      viewHolder.appVersionLabel = (TextView) convertView.findViewById(R.id.transfRecRowAppVersion);
      viewHolder.senderInfo = (TextView) convertView.findViewById(R.id.apkOrigin);
      viewHolder.rowImageLayout =
          (LinearLayout) convertView.findViewById(R.id.transfRecRowImageLayout);
      viewHolder.appInfoLayout = (RelativeLayout) convertView.findViewById(R.id.appInfoLayout);

      convertView.setTag(viewHolder);
    } else {
      viewHolder = (ViewHolder) convertView.getTag();
    }

    if (listOfItems.get(position)
        .isReceived()) {
      viewHolder.senderInfo.setText(R.string.youReceived);

      myOnClickListenerToInstall = new MyOnClickListenerToInstall(position);
      viewHolder.installButton.setOnClickListener(myOnClickListenerToInstall);

      myOnClickListenerToDelete = new MyOnClickListenerToDelete(position);
      viewHolder.deleteFile.setOnClickListener(myOnClickListenerToDelete);

      if (listOfItems.get(position)
          .isDeleted()) {
        viewHolder.transfRecRecvRowIcons.setVisibility(View.GONE);
        viewHolder.transfRecRecvRowDeletedMessage.setVisibility(View.VISIBLE);
      } else {
        viewHolder.transfRecRecvRowIcons.setVisibility(View.VISIBLE);
        viewHolder.transfRecRecvRowDeletedMessage.setVisibility(View.GONE);
      }
    } else {
      viewHolder.reSendButton.setVisibility(View.GONE);
      viewHolder.senderInfo.setTextColor(context.getResources()
          .getColor(R.color.grey));
      if (listOfItems.get(position)
          .isSent()) {
        viewHolder.senderInfo.setText(R.string.youSent);
      } else {
        viewHolder.senderInfo.setText(R.string.youAreSending);
      }
    }
    viewHolder.appNameLabel.setText(listOfItems.get(position)
        .getAppName());
    viewHolder.appImageIcon.setImageDrawable(listOfItems.get(position)
        .getIcon());
    viewHolder.appVersionLabel.setText(listOfItems.get(position)
        .getVersionName());
    return convertView;
  }

  @Override public int getItemViewType(int position) {

    if (listOfItems.get(position)
        .isReceived()) {
      return 0;
    } else {
      return 1;
    }
  }

  @Override public int getViewTypeCount() {
    return 2;
  }

  public void clearListOfItems() {
    if (listOfItems != null) {
      listOfItems.clear();
    }
  }

  public void clearListOfItems(List<TransferRecordItem> list) {
    if (listOfItems != null && list != null) {
      listOfItems.removeAll(list);
    }
  }

  public void addTransferedItem(TransferRecordItem item) {
    listOfItems.add(item);
    notifyDataSetChanged();
  }

  public void updateItem(int positionToUpdate, boolean isSent, boolean needReSend) {
    if (positionToUpdate < listOfItems.size()) {
      listOfItems.get(positionToUpdate)
          .setNeedReSend(needReSend);
      listOfItems.get(positionToUpdate)
          .setSent(isSent);
    }
  }

  public static class ViewHolder {
    TextView senderInfo;
    ImageView appImageIcon;
    TextView appNameLabel;
    TextView appVersionLabel;
    Button installButton;
    LinearLayout rowImageLayout;
    RelativeLayout appInfoLayout;
    Button deleteFile;
    Button reSendButton;
    RelativeLayout transfRecRecvRowIcons;
    LinearLayout transfRecRecvRowDeletedMessage;
  }

  class MyOnClickListenerToInstall implements View.OnClickListener {
    private final int position;

    public MyOnClickListenerToInstall(int position) {
      this.position = position;
    }

    public void onClick(View v) {

      if (listener != null) {
        listener.onInstallApp(getItem(position));
      }
    }
  }

  class MyOnClickListenerToDelete implements View.OnClickListener {
    private final int position;

    public MyOnClickListenerToDelete(int position) {
      this.position = position;
    }

    public void onClick(View v) {

      if (listener != null) {
        listener.onDeleteApp(getItem(position));
      }
    }
  }
}
