package com.alexei.communicationoftwo.adapter;

import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.alexei.communicationoftwo.R;
import com.alexei.communicationoftwo.database.model.DataContact;
import com.alexei.communicationoftwo.model.UserChat;
import com.alexei.communicationoftwo.socket.HostConnections;

import java.io.File;
import java.util.List;

public class AdapterChatList extends RecyclerView.Adapter<AdapterChatList.ViewHolder> {
    private List<UserChat> list;

    private OnSelectListener onSelectItemListener;

    public interface OnSelectListener {
        void onSelItemView(UserChat chat);

        void onSelItemDel(DataContact contact);
    }

    public void setSelectListener(OnSelectListener listener) {
        this.onSelectItemListener = listener;
    }

    public AdapterChatList(List<UserChat> list) {
        this.list = list;
    }


    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.userchat_item, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        UserChat chat = list.get(position);
        DataContact contact = HostConnections.getInstance().getContact(chat.getIdContact());
        loadDataAndAvatar(holder, contact);

        holder.ibViewChat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onSelectItemListener.onSelItemView(chat);
            }
        });
        holder.ibDelChat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onSelectItemListener.onSelItemDel(contact);
            }
        });
    }

    private void loadDataAndAvatar(@NonNull ViewHolder holder, @Nullable DataContact contact) {
        if (contact != null) {
            holder.tvIpChat.setText(contact.getIp());
            holder.tvNameChat.setText(contact.getName());

            File f = new File(contact.getPathAvatar());
            if (f.exists()) {
                holder.ivAvatarChat.setImageURI(Uri.fromFile(f));
            } else {
                if (contact.getPathAvatar().equals("")) {
                    holder.ivAvatarChat.setImageResource(R.drawable.ic_baseline_account_circle_24);
                } else {
                    holder.ivAvatarChat.setImageResource(R.drawable.ic_baseline_block_24);
                }
            }
        }

    }


    @Override
    public int getItemCount() {
        return this.list.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public final TextView tvNameChat;
        public final TextView tvIpChat;

        public final ImageView ivAvatarChat;
        public final ImageButton ibViewChat;
        public final ImageButton ibDelChat;

        public ViewHolder(View itemView) {
            super(itemView);

            this.tvNameChat = (TextView) itemView.findViewById(R.id.tvNameChat);
            this.tvIpChat = (TextView) itemView.findViewById(R.id.tvIpChat);
            this.ivAvatarChat = (ImageView) itemView.findViewById(R.id.ivAvatarChat);
            this.ibViewChat = (ImageButton) itemView.findViewById(R.id.ibViewChat);
            this.ibDelChat = (ImageButton) itemView.findViewById(R.id.ibDelChat);

        }
    }
}
