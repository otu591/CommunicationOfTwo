package com.alexei.communicationoftwo.adapter;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.ExifInterface;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.alexei.communicationoftwo.Const;
import com.alexei.communicationoftwo.R;
import com.alexei.communicationoftwo.database.model.HistoryMessagePacket;
import com.alexei.communicationoftwo.socket.packet.MessagePacket;

import java.io.File;
import java.util.List;

public class AdapterHistoryMessageList extends RecyclerView.Adapter<AdapterHistoryMessageList.ViewHolder> {
    final int VIEW_TYPE_ONE = 1;
    final int VIEW_TYPE_TWO = 2;
    private List<HistoryMessagePacket> msgList;

    private OnAdapterListener onListener;

    public interface OnAdapterListener {


        void onClickByLocationText(String[] location);

        void onOpenVideoAudioFile(MessagePacket msgPacket);

        void onOpenFile(MessagePacket msgPacket);

        void onChooseApp(MessagePacket msgPacket);
    }

    public void setListeners(OnAdapterListener listener) {
        this.onListener = listener;
    }

    public List<HistoryMessagePacket> getMsgList() {
        return msgList;
    }

    public void setMsgList(List<HistoryMessagePacket> msgList) {
        this.msgList = msgList;
    }

    public AdapterHistoryMessageList(List<HistoryMessagePacket> list) {
        this.msgList = list;
    }

    @NonNull
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;
        if (viewType == VIEW_TYPE_ONE) {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.out_message_item, parent, false);
        } else {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.in_message_item, parent, false);
        }
        return new ViewHolder(view);
    }

    @Override
    public int getItemViewType(int position) {

        HistoryMessagePacket message = msgList.get(position);

        if (message.getType() == Const.MESSAGE_TYPE_DIRECTION_OUT) {
            return VIEW_TYPE_ONE;
        } else {
            return VIEW_TYPE_TWO;
        }
    }


    public void onBindViewHolder(final ViewHolder holder, int position) {
        HistoryMessagePacket msgPacket = msgList.get(position);

        holder.tvDate.setVisibility(View.GONE);
        holder.blockPlayTools.setVisibility(View.GONE);
        holder.blockFileTools.setVisibility(View.GONE);
        holder.ivBitmapFile.setImageBitmap(null);
        holder.ivBitmapFile.setRotation(0);

        if (holder.tvSendStatus != null) {
            switch (msgPacket.getMessagePacket().getStatusAccept()) {
                case Const.ACCEPT_MESSAGE_STATUS:
                    holder.tvSendStatus.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_baseline_check_24, 0);
                    break;
                case Const.OUT_MEMORY_MESSAGE_STATUS:
                    holder.tvSendStatus.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_baseline_block_24, 0);
                    break;
                default:
                    holder.tvSendStatus.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_baseline_arrow_right_alt_24, 0);
            }
        }


        if (msgPacket.getMessagePacket().getFilePacket() != null) {
            holder.ibChooseApp.setVisibility(View.VISIBLE);
            String type = msgPacket.getMessagePacket().getFilePacket().getTypeFile();
            if (type != null) {
                switch (type) {
                    case "jpg":
                    case "png":
                    case "bmp":
                    case "gif":
                    case "svg":
                    case "jpeg":
                    case "ico":
                        loadImage(holder, msgPacket.getMessagePacket().getFilePacket().getPathFile());
                        break;
                    case "aac":
                    case "mp3":
                    case "wav":
                    case "iff":
                    case "mid":
                    case "aif":
                    case "mp4":
                    case "3gp":
                    case "avi":
                        holder.tvAudioFile.setText(msgPacket.getMessagePacket().getFilePacket().getNameFile());
                        holder.blockPlayTools.setVisibility(View.VISIBLE);

                        holder.tvAudioFile.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                onListener.onOpenVideoAudioFile(msgPacket.getMessagePacket());
                            }
                        });
                        break;
                    default:
                        holder.tvFile.setText(msgPacket.getMessagePacket().getFilePacket().getNameFile());
                        holder.blockFileTools.setVisibility(View.VISIBLE);
                        holder.tvFile.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                onListener.onOpenFile(msgPacket.getMessagePacket());
                            }
                        });
                        break;
                }
            }

            holder.ibChooseApp.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (msgPacket.getMessagePacket().getFilePacket() != null) {
                        onListener.onChooseApp(msgPacket.getMessagePacket());
                    }
                }
            });
        } else {
            holder.ivBitmapFile.setImageBitmap(null);
            holder.ibChooseApp.setVisibility(View.GONE);
        }

        if (position > 0) {
            long previousDay = msgList.get(position - 1).getMessagePacket().getCreated() / 86400000;
            long currDay = (msgPacket.getMessagePacket().getCreated()) / 86400000;
            if (currDay != previousDay) {
                holder.tvDate.setText(Const.formatDate.format(msgPacket.getMessagePacket().getCreated()));
                holder.tvDate.setVisibility(View.VISIBLE);
            }
        } else if (position == 0) {
            holder.tvDate.setText(Const.formatDate.format(msgPacket.getMessagePacket().getCreated()));
            holder.tvDate.setVisibility(View.VISIBLE);
        }

        holder.tvCreated.setText(Const.formatTime.format(msgPacket.getMessagePacket().getCreated()));
        holder.tvMessage.setText(msgPacket.getMessagePacket().getMessage());
        coordinateToLink(holder.tvMessage);

    }

    private void loadImage(ViewHolder holder, String fPath) {

        File f = new File(fPath);

        if (f.exists()) {
            Bitmap bitmap = BitmapFactory.decodeFile(f.getAbsolutePath());
            holder.ivBitmapFile.setImageBitmap(bitmap);
            defineOrientationBitmap(f.getAbsolutePath(), holder.ivBitmapFile);
        } else {
            holder.ivBitmapFile.setImageResource(R.drawable.ic_baseline_image_24);
        }

    }

    private void defineOrientationBitmap(String filePath, ImageView ivBitmapFile) {
        try {
            ExifInterface exifInterface = new ExifInterface(filePath);
            int orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);

            if (orientation == ExifInterface.ORIENTATION_ROTATE_90) {
                ivBitmapFile.setRotation(90);
            } else if (orientation == ExifInterface.ORIENTATION_ROTATE_180) {
                ivBitmapFile.setRotation(180);
            } else if (orientation == ExifInterface.ORIENTATION_ROTATE_270) {
                ivBitmapFile.setRotation(270);
            }

        } catch (Exception e) {
            System.out.println("AdapterHistoryMessageList -> getOrientationBitmap ERROR : - " + e.getMessage());
        }
    }


    private void coordinateToLink(TextView view) {

        SpannableString ss = new SpannableString(view.getText());
        int pos1 = -1;
        int pos2 = -1;

        for (int i = 0; i < view.length(); i++) {

            if (view.getText().charAt(i) == '{' && pos1 == -1) pos1 = i;
            if (view.getText().charAt(i) == '}' && pos2 == -1) pos2 = i;
            if (pos1 > -1 && pos2 > -1) {

                ss.setSpan(new ClickableSpan() {
                    @Override
                    public void onClick(@NonNull View view) {
                        TextView tv = (TextView) view;

                        Spanned s = (Spanned) tv.getText();
                        int start = s.getSpanStart(this);
                        int end = s.getSpanEnd(this);
                        String[] sp = (s.subSequence(start, end) + "").split(",");
                        onListener.onClickByLocationText(sp);

                    }
                }, pos1 + 1, pos2, 0);
                pos1 = -1;
                pos2 = -1;
            }
        }
        view.setText(ss);
        view.setMovementMethod(LinkMovementMethod.getInstance());
        view.setHighlightColor(Color.TRANSPARENT);

    }

    public int getItemCount() {
        return this.msgList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        public final TextView tvDate;
        public final TextView tvCreated;
        public final TextView tvMessage;
        public final TextView tvAudioFile;
        public final TextView tvFile;
        public final TextView tvSendStatus;
        public final ImageView ivBitmapFile;
        public final ImageButton ibChooseApp;
        public final LinearLayout blockPlayTools;
        public final LinearLayout blockFileTools;


        public ViewHolder(View itemView) {
            super(itemView);
            this.tvMessage = itemView.findViewById(R.id.tvMessage);
            this.ibChooseApp = itemView.findViewById(R.id.ibChooseApp);
            this.tvCreated = itemView.findViewById(R.id.tvCreate);
            this.tvAudioFile = itemView.findViewById(R.id.tvAudioVideoFile);
            this.tvFile = itemView.findViewById(R.id.tvFile);
            this.ivBitmapFile = itemView.findViewById(R.id.ivBitmapFile);
            this.blockPlayTools = itemView.findViewById(R.id.blockPlayTools);
            this.blockFileTools = itemView.findViewById(R.id.blockFileTools);
            this.tvSendStatus = itemView.findViewById(R.id.tvSendStatus);
            this.tvDate = itemView.findViewById(R.id.tvDate);

        }
    }
}

