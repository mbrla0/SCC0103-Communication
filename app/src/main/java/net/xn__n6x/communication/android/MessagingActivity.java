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

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Optional;

public class MessagingActivity extends AppCompatActivity {

    /** Key in the extra bundle containing the peer for this activity. */
    public static final String EXTRA_PEER_ID = "PeerId";

    /* UI-related structures. */
    protected RecyclerView msgView;
    protected Button send;
    protected TextView input;

    /* Watchdog interface and functionality. */
    protected Watchdog.Binder watchdog;
    protected Id target;

    /** Message data. */
    protected ArrayList<Message> messages;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_messaging);

        this.messages = new ArrayList<>();

        /* Get the user interface structures. */
        this.msgView = Objects.requireNonNull(findViewById(R.id.messages), "Required view is null");
        this.send    = Objects.requireNonNull(findViewById(R.id.sendButton), "Required view is null");
        this.input   = Objects.requireNonNull(findViewById(R.id.messageInput), "Required view is null");

        /* Get our target Id. */
        Bundle extras = this.getIntent().getExtras();
        if(extras == null) {
            Log.e("MessagingActivity", "Extra bundle is null.");
            this.finish();

            return;
        }
        this.target = extras.getParcelable(EXTRA_PEER_ID);
        if(this.target == null) {
            Log.e("MessagingActivity", "Extra bundle does not contain a PeerId");
            this.finish();

            return;
        }

        /* Update the UI and set it up. */
        this.setTitle(this.target.toString());
        this.send.setOnClickListener(this::onSendClicked);
        msgView.setAdapter(new MessagingEntrySetAdapter(this.messages));
        msgView.setLayoutManager(new LinearLayoutManager(this));

        /* Bind to the watchdog. */
        Intent watchdog = new Intent(this, Watchdog.class);
        this.bindService(
            watchdog,
            new ServiceConnection() {
                @Override
                public void onServiceConnected(ComponentName name, IBinder service) {
                    MessagingActivity.this.watchdog = (Watchdog.Binder) service;
                    MessagingActivity.this.onWatchdogConnected();
                }

                @Override
                public void onServiceDisconnected(ComponentName name) {
                    Log.e("MessagingActivity", "Lost connection to the Watchdog");
                    MessagingActivity.this.finish();
                }
            },
            Context.BIND_AUTO_CREATE);
    }

    /** When the send button has been clicked. */
    protected void onSendClicked(View view) {
        if(this.watchdog == null) {
            Log.d(
                "MessagingActivity",
                "Called onSendClicked() before we were connected to the Watchdog.");
            return;
        }

        /* Get the text in the text box and clear it. */
        byte[] data = this.input.getText().toString().getBytes(StandardCharsets.UTF_8);
        this.input.setText("");

        /* Send it out. */
        this.watchdog.send(data, this.target);
    }

    /** When messages from our peer become available. */
    protected void onWatchdogMessage(Id source) {
        this.runOnUiThread(() -> {
            Optional<byte[]> optMessage;
            while ((optMessage = this.watchdog.tryReceive(source)).isPresent()) {
                byte[] msgBytes = optMessage.get();
                String contents = StandardCharsets.UTF_8
                    .decode(ByteBuffer.wrap(msgBytes))
                    .toString();
                Log.i("MessagingActivity", "Got " + contents);
                this.messages.add(new Message(source, contents));
                this.msgView.setAdapter(new MessagingEntrySetAdapter(this.messages));
            }
        });
    }

    /** Sets up the activity upon connection to the Watchdog. */
    protected void onWatchdogConnected() {
        /* Set up the listener. */
        this.watchdog.listen(
            this.target,
            this::onWatchdogMessage);
    }

    /** A data class for messages. */
    protected static class Message {
        public final Id sender;
        public final String content;

        public Message(Id sender, String content) {
            this.sender = sender;
            this.content = content;
        }
    }

    protected static class MessagingEntrySetAdapter extends RecyclerView.Adapter<MessagingEntrySetHolder> {
        protected final ArrayList<Message> messages;

        protected MessagingEntrySetAdapter(ArrayList<Message> messages) {
            this.messages = messages;
        }

        @NonNull
        @Override
        public MessagingEntrySetHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                .inflate(viewType, parent, false);
            return new MessagingEntrySetHolder(v);
        }

        @Override
        public int getItemViewType(int position) {
            return R.layout.layout_message;
        }

        @Override
        public void onBindViewHolder(@NonNull MessagingEntrySetHolder holder, int position) {
            Message message = messages.get(position);

            holder.content.setText(message.content);
            holder.sender.setText(message.sender.toString());
        }

        @Override
        public int getItemCount() {
            return this.messages.size();
        }
    }

    protected static class MessagingEntrySetHolder extends RecyclerView.ViewHolder {
        protected final TextView sender;
        protected final TextView content;

        public MessagingEntrySetHolder(@NonNull View itemView) {
            super(itemView);

            this.sender = Objects.requireNonNull(itemView.findViewById(R.id.messageSender), "Required view is null");
            this.content = Objects.requireNonNull(itemView.findViewById(R.id.messageContent), "Required view is null");
        }
    }
}
