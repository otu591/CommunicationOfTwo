package com.alexei.communicationoftwo.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.alexei.communicationoftwo.R;
import com.alexei.communicationoftwo.model.StreamBlockData;
import com.alexei.communicationoftwo.Const;

import java.util.List;


public class AdapterBlockConnectStream extends RecyclerView.Adapter<AdapterBlockConnectStream.StreamViewHolder> {

    private List<StreamBlockData> blockStreamsList;


    public AdapterBlockConnectStream(List<StreamBlockData> list) {
        this.blockStreamsList = list;
    }

    @NonNull
    public StreamViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new StreamViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.block_connect_stream_item, parent, false));
    }

    public void onBindViewHolder(final StreamViewHolder holder, int position) {
        StreamBlockData blockData = blockStreamsList.get(position);

        holder.tvIP.setText(blockData.getIP());
        holder.tvName.setText(blockData.getName());
        holder.tvAccess.setText(Const.formatTimeDate.format(blockData.getAccess()));

    }

    public int getItemCount() {
        return this.blockStreamsList.size();
    }

    public static class StreamViewHolder extends RecyclerView.ViewHolder {

        public final TextView tvIP;
        public final TextView tvName;
        public final TextView tvAccess;

        public StreamViewHolder(View itemView) {
            super(itemView);

            this.tvIP = (TextView) itemView.findViewById(R.id.tvBlockIP);
            this.tvName = (TextView) itemView.findViewById(R.id.tvBlockName);
            this.tvAccess = (TextView) itemView.findViewById(R.id.tvBlockAccess);

        }
    }
}

