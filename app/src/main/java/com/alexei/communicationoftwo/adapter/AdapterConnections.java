package com.alexei.communicationoftwo.adapter;

import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.AnimationDrawable;
import android.media.AudioManager;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.alexei.communicationoftwo.Const;
import com.alexei.communicationoftwo.R;
import com.alexei.communicationoftwo.database.model.DataContact;
import com.alexei.communicationoftwo.socket.ConnectionObject;
import com.alexei.communicationoftwo.socket.client.CommunicationNode;
import com.alexei.communicationoftwo.socket.server.ConnectStream;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;
import java.util.List;

public class AdapterConnections extends RecyclerView.Adapter<AdapterConnections.StreamViewHolder> {

    private List<ConnectionObject> list;

    private OnListeners onListener;

    public interface OnListeners {
        void onSelectItem(ConnectionObject streamData);

        void onCreateContactFromConnection(ConnectionObject object);

        void onNewActionPhone(ConnectionObject object);

        void onClickInfoMissedCalls(List<Long> list, int position);

        void onSelectAvatar(ConnectionObject object);

    }

    public void setListeners(OnListeners listener) {
        this.onListener = listener;
    }

    public AdapterConnections(List<ConnectionObject> list) {
        this.list = list;
    }

    @NonNull
    public StreamViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new StreamViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.connections_item, parent, false));
    }

    public void onBindViewHolder(@NonNull final StreamViewHolder holder, int position) {

        ConnectionObject object = list.get(position);

        holder.ibAddNewContact.setVisibility(View.GONE);
        holder.optSelectItem.setChecked(object.isSelected());
        holder.tvWarningConnection.setVisibility(View.GONE);


        if (object.getType() == Const.TYPE_CONNECT_STREAM) {
            ConnectStream stream = (ConnectStream) object.getCommunication();

            if (stream.getContact().getId() <= 0)
                holder.tvWarningConnection.setVisibility(View.VISIBLE);

            holder.tvTime.setText(Const.formatTimeDate.format(stream.timeCreate));
            holder.tvTime.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_baseline_call_received_24, 0);

            holder.ibAddNewContact.setVisibility(stream.getContact().getId() <= 0 ? View.VISIBLE : View.GONE);

            holder.tvInfoMissedCalls.setVisibility((stream.missedCalls.size() > 0) ? View.VISIBLE : View.GONE);

            getMissedCalls(holder, stream.missedCalls);

        } else {
            CommunicationNode commNode = (CommunicationNode) object.getCommunication();

            holder.tvTime.setText(Const.formatTimeDate.format(commNode.timeCreate));
            holder.tvTime.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_baseline_call_made_24, 0);

            holder.tvInfoMissedCalls.setVisibility((commNode.missedCalls.size() > 0) ? View.VISIBLE : View.GONE);

            getMissedCalls(holder, commNode.missedCalls);
        }

        holder.tvName.setText(object.getContact().getName());

        holder.ibMail.setVisibility((object.getMail().isArrived()) ? View.VISIBLE : View.GONE);

        gettingAvatar(holder, object.getContact());

        drawIMGPhone(holder,object.getVoiceCommReqState());

        holder.ivAvatar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onListener.onSelectAvatar(object);
            }
        });

        holder.ibMail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selectedItem(object);
            }
        });

        holder.ibAddNewContact.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (object.getType() == Const.TYPE_CONNECT_STREAM) {
                    onListener.onCreateContactFromConnection(object);
                }
            }
        });

        holder.optSelectItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selectedItem(object);
            }
        });

        holder.tvPhone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onListener.onNewActionPhone(object);
            }
        });

    }




    private void gettingAvatar(@NonNull StreamViewHolder holder, DataContact c) {

        if (c.getId() <= 0) {//если не известный
            if (c.getBytesPhoto() != null && c.getBytesPhoto().length > 0) {
                Bitmap bitmap = BitmapFactory.decodeByteArray(c.getBytesPhoto(),
                        0,
                        c.getBytesPhoto().length);
                holder.ivAvatar.setImageBitmap(bitmap);
            } else {
                holder.ivAvatar.setImageResource(R.drawable.ic_baseline_account_circle_24);
            }

        } else {
            if (c.getPathAvatar().equals("")) {
                holder.ivAvatar.setImageResource(R.drawable.ic_baseline_account_circle_24);
            } else {
                File f = new File(c.getPathAvatar());
                if (f.exists()) {
                    holder.ivAvatar.setImageURI(Uri.fromFile(f));
                } else {
                    holder.ivAvatar.setImageResource(R.drawable.ic_baseline_block_24);
                }
            }

        }

    }

    private void getMissedCalls(StreamViewHolder holder, List<Long> missedCalls) {
        holder.tvInfoMissedCalls.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onListener.onClickInfoMissedCalls(missedCalls, holder.getAdapterPosition());
            }
        });
    }

    private void drawIMGPhone(StreamViewHolder holder, int statusPhone) {
        System.out.println("86");
        AnimationDrawable rocketAnimation;
        holder.tvPhone.clearAnimation();

        switch (statusPhone) {

            case Const.OUTGOING_PHONE_CALL://исходяший

                holder.tvPhone.setBackgroundResource(R.drawable.custom_image_button);
                holder.tvPhone.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, R.drawable.list_phone_forwarded);

                rocketAnimation = (AnimationDrawable) holder.tvPhone.getCompoundDrawables()[3];
                rocketAnimation.start();
                break;
            case Const.INCOMING_PHONE_CALL://входящий

                holder.tvPhone.setBackgroundResource(R.drawable.custom_image_button_dial_color);
                holder.tvPhone.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, android.R.drawable.sym_call_incoming);

                Animation anim = new AlphaAnimation(0, 1);
                anim.setInterpolator(new LinearInterpolator());

                anim.setRepeatCount(Animation.INFINITE);
                anim.setRepeatMode(Animation.REVERSE);
                anim.setDuration(1000);

                holder.tvPhone.setAnimation(anim);
                break;
            case Const.VOICE_PHONE://разговор
                holder.tvPhone.setBackgroundResource(R.drawable.custom_image_button);
                holder.tvPhone.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, R.drawable.list_phone_voice);

                rocketAnimation = (AnimationDrawable) holder.tvPhone.getCompoundDrawables()[3];
                rocketAnimation.start();
                break;
            case Const.MISSING_PHONE://missing разговор

                holder.tvPhone.setBackgroundResource(R.drawable.custom_image_button);
                holder.tvPhone.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, R.drawable.ic_phone_missed);


        }
    }

    private void selectedItem(ConnectionObject object) {

        onListener.onSelectItem(object);
    }

    public int getItemCount() {
        return this.list.size();
    }

    public static class StreamViewHolder extends RecyclerView.ViewHolder {
        public final TextView tvTime;
        public final TextView tvPhone;
        public final TextView tvName;
        public final ImageButton ibMail;

        public final ImageButton ibAddNewContact;
        public final ImageView ivAvatar;
        public final TextView tvInfoMissedCalls;
        public final TextView tvWarningConnection;
        public final RadioButton optSelectItem;


        public StreamViewHolder(View itemView) {
            super(itemView);
            this.tvTime = (TextView) itemView.findViewById(R.id.tvTimeConnection);
            this.tvName = (TextView) itemView.findViewById(R.id.tvNameConnection);
            this.tvPhone = (TextView) itemView.findViewById(R.id.tvPhone);

            this.ibAddNewContact = (ImageButton) itemView.findViewById(R.id.ibAddNewContact);
            this.ivAvatar = (ImageView) itemView.findViewById(R.id.ivAvatar);
            this.tvInfoMissedCalls = (TextView) itemView.findViewById(R.id.tvMissedCalls);
            this.tvWarningConnection = (TextView) itemView.findViewById(R.id.tvWarningConnection);
            this.optSelectItem = (RadioButton) itemView.findViewById(R.id.optSelectItem);
            this.ibMail = (ImageButton) itemView.findViewById(R.id.ibMail);

        }
    }
}

