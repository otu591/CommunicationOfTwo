package com.alexei.communicationoftwo.adapter;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;


import com.alexei.communicationoftwo.R;
import com.alexei.communicationoftwo.Const;
import com.alexei.communicationoftwo.database.model.DataContact;

import java.io.File;
import java.util.List;

public class AdapterContacts extends RecyclerView.Adapter<AdapterContacts.ViewHolder> {
    private List<DataContact> list;

    private OnSelectListener onSelectItemListener;

    public interface OnSelectListener {
        void onCmdRunConnection(DataContact contact);

        void onEditItem(DataContact contact);

        void onNotifyConnection(DataContact contact);

        void onCloseConnection(DataContact contact);
    }

    public void setSelectListener(OnSelectListener listener) {
        this.onSelectItemListener = listener;
    }

    public AdapterContacts(List<DataContact> list) {
        this.list = list;
    }


    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.contacts_item, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DataContact contact = list.get(position);

        holder.tvName.setText(contact.getName());
        holder.tvIp.setText(contact.getIp());
        holder.switchRestart.setChecked(contact.isRestart());


        loadAvatar(holder, contact);

        if (contact.isNotificationSoundOfConnection()) {
            holder.ibNotify.setImageResource(R.drawable.ic_baseline_notifications_24);
        } else {
            holder.ibNotify.setImageResource(R.drawable.ic_baseline_notifications_off_24);
        }


        switch (contact.getStatus()) {

            case Const.NO_CONNECT_CONTACT_STATUS:
                holder.itemView.setEnabled(true);
                holder.switchRestart.setText(R.string.no_connected);
                holder.switchRestart.setTextColor(Color.RED);
                holder.switchRestart.setEnabled(true);

                break;
            case Const.CONNECT_CONTACT_STATUS:
                holder.itemView.setEnabled(true);
                holder.switchRestart.setText(R.string.connected);
                holder.switchRestart.setTextColor(Color.GREEN);

                break;
            case Const.CONNECTING_CONTACT_STATUS:
                holder.itemView.setEnabled(true);
                holder.switchRestart.setText(R.string.connection);
                holder.switchRestart.setTextColor(Color.BLUE);

                break;
            case Const.BLOCKED_IP_CONTACT_STATUS:
                holder.itemView.setEnabled(true);
                holder.switchRestart.setText(R.string.block_ip);
                holder.switchRestart.setTextColor(Color.RED);

                break;
            case Const.DROP_IP_CONTACT_STATUS:
                holder.switchRestart.setText(R.string.drop_connection);
                holder.switchRestart.setTextColor(Color.RED);

                break;
        }


        holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                onSelectItemListener.onEditItem(contact);
                return true;
            }
        });

        holder.ibNotify.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (contact.isNotificationSoundOfConnection()) {//отключаем
                    contact.setNotificationSoundOfConnection(false);
                    holder.ibNotify.setImageResource(R.drawable.ic_baseline_notifications_off_24);
                } else {//включаем
                    contact.setNotificationSoundOfConnection(true);
                    holder.ibNotify.setImageResource(R.drawable.ic_baseline_notifications_24);
                }
                onSelectItemListener.onNotifyConnection(contact);
            }
        });

        holder.switchRestart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                boolean b = ((Switch) view).isChecked();
                contact.setRestart(b);
                if (b) {//ЗАПУСК
//                    contact.setStatus(Const.CONNECTING_CONTACT_STATUS);
                    onSelectItemListener.onCmdRunConnection(contact);
                }else {
                    view.setEnabled(false);
                    onSelectItemListener.onCloseConnection(contact);
                }
            }
        });
    }

    private void loadAvatar(@NonNull ViewHolder holder, DataContact contact) {
        File f = new File(contact.getPathAvatar());
        if (f.exists()) {
            holder.ivAvatarContact.setImageURI(Uri.parse(contact.getPathAvatar()));
        } else {
            holder.ivAvatarContact.setImageResource(R.drawable.ic_baseline_account_circle_24);
        }
    }


    @Override
    public int getItemCount() {
        return this.list.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public final TextView tvName;
        public final TextView tvIp;
        @SuppressLint("UseSwitchCompatOrMaterialCode")
        public final Switch switchRestart;
        public final ImageButton ibNotify;
        public final ImageView ivAvatarContact;

        public ViewHolder(View itemView) {
            super(itemView);
            this.tvIp = (TextView) itemView.findViewById(R.id.tvContactIP);
            this.tvName = (TextView) itemView.findViewById(R.id.tvContactName);

            this.switchRestart = (Switch) itemView.findViewById(R.id.switchRestart);
            this.ibNotify = (ImageButton) itemView.findViewById(R.id.ibNotifyOfConnection);
            this.ivAvatarContact = (ImageView) itemView.findViewById(R.id.ivAvatarContact);
        }
    }
}
