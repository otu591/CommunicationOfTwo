package com.alexei.communicationoftwo.adapter;

import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.alexei.communicationoftwo.Const;
import com.alexei.communicationoftwo.R;
import com.alexei.communicationoftwo.socket.ConnectionObject;
import com.alexei.communicationoftwo.socket.client.CommunicationNode;
import com.alexei.communicationoftwo.socket.server.ConnectStream;

import java.io.File;
import java.util.List;

public class AdapterConnectionsByCriteria extends RecyclerView.Adapter<AdapterConnectionsByCriteria.ViewHolder> {
    private List<ConnectionObject> list;

    private OnSelectListener onSelectItemListener;

    public interface OnSelectListener {
        void onSelItem(ConnectionObject object);
    }

    public void setSelectListener(OnSelectListener listener) {
        this.onSelectItemListener = listener;
    }

    public AdapterConnectionsByCriteria(List<ConnectionObject> list) {
        this.list = list;
    }


    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.connections_by_criteria_item, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ConnectionObject object = list.get(position);

        loadDataAndAvatar(holder, object);

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onSelectItemListener.onSelItem(object);
            }
        });
    }

    private void loadDataAndAvatar(@NonNull ViewHolder holder, ConnectionObject object) {
        String pathF = "";
        if (object.getType() == Const.TYPE_CONNECT_STREAM) {
            ConnectStream stream = (ConnectStream) object.getCommunication();
            holder.tvNameConnection.setText(stream.getContact().getName());
            holder.tvDateMessageLast.setText(Const.formatTimeDate.format(stream.newMail.getLastDate()));
            holder.tvCountNewMessage.setText(stream.newMail.getCount() + "/");
            pathF = stream.getContact().getPathAvatar();
        } else if (object.getType() == Const.TYPE_COMMUNICATION_NODE) {
            CommunicationNode commNode = (CommunicationNode) object.getCommunication();
            holder.tvNameConnection.setText(commNode.contact.getName());
            holder.tvDateMessageLast.setText(Const.formatTimeDate.format(commNode.newMail.getLastDate()));
            holder.tvCountNewMessage.setText(commNode.newMail.getCount() + "/");
            pathF = commNode.contact.getPathAvatar();
        }

        File f = new File(pathF);
        if (f.exists()) {
            holder.ivAvatarConnection.setImageURI(Uri.fromFile(f));
        } else {
            if (pathF.equals("")) {
                holder.ivAvatarConnection.setImageResource(R.drawable.ic_baseline_account_circle_24);
            } else {
                holder.ivAvatarConnection.setImageResource(R.drawable.ic_baseline_block_24);
            }
        }
    }


    @Override
    public int getItemCount() {
        return this.list.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public final TextView tvNameConnection;
        public final TextView tvCountNewMessage;
        public final TextView tvDateMessageLast;
        public final ImageView ivAvatarConnection;

        public ViewHolder(View itemView) {
            super(itemView);

            this.tvNameConnection = (TextView) itemView.findViewById(R.id.tvNameConnection);
            this.ivAvatarConnection = (ImageView) itemView.findViewById(R.id.ivAvatarConnection);
            this.tvCountNewMessage = (TextView) itemView.findViewById(R.id.tvCountNewMessage);
            this.tvDateMessageLast = (TextView) itemView.findViewById(R.id.tvDateMessageLast);
        }
    }
}
