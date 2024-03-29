package net.xn__n6x.communication.android;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import net.xn__n6x.communication.R;
import net.xn__n6x.communication.identity.Id;
import net.xn__n6x.communication.watchdog.Watchdog;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Objects;
import java.util.stream.Collectors;

public class PeerSelectionActivity extends AppCompatActivity {

    /* UI-related structures. */
    protected RecyclerView peersView;

    /* Watchdog interface and functionality. */
    protected Watchdog.Binder watchdog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_peer_selection);

        this.peersView = Objects.requireNonNull(this.findViewById(R.id.peerList), "Kill me");
        this.peersView.setLayoutManager(new LinearLayoutManager(this));

        /* Connect to the Watchdog. */
        Intent watchdog = new Intent(this, Watchdog.class);
        this.bindService(
            watchdog,
            new ServiceConnection() {
                @Override
                public void onServiceConnected(ComponentName name, IBinder service) {
                    PeerSelectionActivity.this.watchdog = (Watchdog.Binder) service;
                    PeerSelectionActivity.this.watchdog.watchDiscovery(PeerSelectionActivity.this::onPeerListChanged);
                }

                @Override
                public void onServiceDisconnected(ComponentName name) {
                    Log.e("MessagingActivity", "Lost connection to the Watchdog");
                    PeerSelectionActivity.this.finish();
                }
            },
            Context.BIND_AUTO_CREATE);
    }

    protected void onPeerListChanged(HashSet<Id> peers) {
        Log.d("PeerSelectionActivity", "The peer list changed to:");
        for(Id peer : peers)
            Log.d("PeerSelectionActivity", "    * " + peer);
        this.peersView.setAdapter(new PeerSetAdapter(peers));
    }

    protected class PeerSetAdapter extends RecyclerView.Adapter<PeerSetEntryHolder> {
        protected final ArrayList<Id> peers;

        public PeerSetAdapter(HashSet<Id> peers) {
            this.peers = new ArrayList<>(peers);
        }
        @Override
        public int getItemViewType(int position) {
            return R.layout.layout_peer_entry;
        }
        @NonNull
        @Override
        public PeerSetEntryHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                .inflate(viewType, parent, false);
            return new PeerSetEntryHolder(v);
        }
        @Override
        public void onBindViewHolder(@NonNull PeerSetEntryHolder holder, int position) {
            Id current = this.peers.get(position);
            holder.peerDisplayName.setText(current.toString());
            holder.peerEntry.setOnClickListener(view -> startMessaging(current));
        }
        @Override
        public int getItemCount() {
            return this.peers.size();
        }
    }

    protected void startMessaging(Id target) {
        Intent start = new Intent(this, MessagingActivity.class);
        start.putExtra(MessagingActivity.EXTRA_PEER_ID, target);

        this.startActivity(start);
    }

    protected static class PeerSetEntryHolder extends RecyclerView.ViewHolder {
        protected final ImageView peerAvatar;
        protected final TextView peerDisplayName;
        protected final ConstraintLayout peerEntry;

        public PeerSetEntryHolder(@NonNull View itemView) {
            super(itemView);

            this.peerEntry = Objects.requireNonNull(itemView.findViewById(R.id.peerEntry), "Required view is null");
            this.peerAvatar = Objects.requireNonNull(itemView.findViewById(R.id.peerAvatar), "Required view is null");
            this.peerDisplayName = Objects.requireNonNull(itemView.findViewById(R.id.peerDisplayName), "Required view is null");
        }
    }

}
