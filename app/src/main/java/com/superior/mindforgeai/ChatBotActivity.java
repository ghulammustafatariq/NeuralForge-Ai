package com.superior.mindforgeai;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ChatBotActivity extends AppCompatActivity {

    private static final String TAG = "ChatBotActivity";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private RecyclerView rvMessages;
    private EditText etInput;
    private ImageButton btnSend;
    private ChatAdapter adapter;
    private OkHttpClient client;
    private String topic;

    private final List<ChatMessage> messages = new ArrayList<>();
    private boolean loading = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chatbot);

        topic = getIntent().getStringExtra("TOPIC");
        if (topic == null) topic = "This topic";

        TextView tvTitle = findViewById(R.id.tvChatTitle);
        tvTitle.setText("Chat: " + topic);

        rvMessages = findViewById(R.id.rvChatMessages);
        etInput = findViewById(R.id.etChatInput);
        btnSend = findViewById(R.id.btnSend);

        ImageButton btnBack = findViewById(R.id.btnChatBack);
        btnBack.setOnClickListener(v -> finish());

        client = new OkHttpClient();

        adapter = new ChatAdapter(messages);
        rvMessages.setLayoutManager(new LinearLayoutManager(this));
        rvMessages.setAdapter(adapter);

        // Entrance animation for the chat container
        findViewById(R.id.rvChatMessages).setAlpha(0f);
        findViewById(R.id.rvChatMessages).animate().alpha(1f).setDuration(500).start();

        messages.add(new ChatMessage("👋 Ask me anything about \"" + topic + "\"!", false));
        adapter.notifyItemInserted(0);

        btnSend.setOnClickListener(v -> sendMessage());
        
        runEntranceAnimations();
    }

    private void runEntranceAnimations() {
        Animation fallDown = AnimationUtils.loadAnimation(this, R.anim.fall_down);
        View titleParent = (View) findViewById(R.id.tvChatTitle).getParent();
        if (titleParent != null) {
            titleParent.startAnimation(fallDown);
        }
        
        View inputContainer = (View) etInput.getParent();
        if (inputContainer != null) {
            inputContainer.setTranslationY(100f);
            inputContainer.animate().translationY(0f).setDuration(500).start();
        }
    }

    private void sendMessage() {
        String text = etInput.getText().toString().trim();
        if (text.isEmpty() || loading) return;

        etInput.setText("");
        messages.add(new ChatMessage(text, true));
        adapter.notifyItemInserted(messages.size() - 1);
        rvMessages.scrollToPosition(messages.size() - 1);

        loading = true;
        callDeepSeek(text);
    }

    private void callDeepSeek(String userMessage) {
        JSONObject body = new JSONObject();
        try {
            body.put("model", "deepseek-chat");
            body.put("temperature", 0.5);
            body.put("max_tokens", 2048);
            body.put("stream", false);

            JSONArray msgs = new JSONArray();
            JSONObject sys = new JSONObject();
            sys.put("role", "system");
            sys.put("content", "You are a helpful tutor. The user is learning about: " + topic +
                    ". Answer concisely and clearly. Stay on topic. Use markdown for formatting.");
            msgs.put(sys);

            JSONObject user = new JSONObject();
            user.put("role", "user");
            user.put("content", userMessage);
            msgs.put(user);

            body.put("messages", msgs);
        } catch (Exception e) {
            return;
        }

        SharedPreferences prefs = getSharedPreferences("MindForgePrefs", MODE_PRIVATE);
        String apiKey = prefs.getString("apiKey", "");
        if (apiKey.isEmpty()) apiKey = "sk-c63f425226ce4ffba7b80c3daa758105";

        Request request = new Request.Builder()
                .url("https://api.deepseek.com/chat/completions")
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(body.toString(), JSON))
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    loading = false;
                    messages.add(new ChatMessage("Error: " + e.getMessage(), false));
                    adapter.notifyItemInserted(messages.size() - 1);
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String respBody = response.body() != null ? response.body().string() : "";
                try {
                    if (!response.isSuccessful()) {
                        runOnUiThread(() -> {
                            loading = false;
                            messages.add(new ChatMessage("API error " + response.code(), false));
                            adapter.notifyItemInserted(messages.size() - 1);
                        });
                        return;
                    }

                    JSONObject json = new JSONObject(respBody);
                    String reply = json.getJSONArray("choices")
                            .getJSONObject(0)
                            .getJSONObject("message")
                            .getString("content");

                    runOnUiThread(() -> {
                        loading = false;
                        messages.add(new ChatMessage(reply, false));
                        adapter.notifyItemInserted(messages.size() - 1);
                        rvMessages.scrollToPosition(messages.size() - 1);
                    });
                } catch (Exception e) {
                    runOnUiThread(() -> {
                        loading = false;
                        messages.add(new ChatMessage("Parse error", false));
                        adapter.notifyItemInserted(messages.size() - 1);
                    });
                } finally {
                    response.close();
                }
            }
        });
    }

    static class ChatMessage {
        String text;
        boolean isUser;

        ChatMessage(String text, boolean isUser) {
            this.text = text;
            this.isUser = isUser;
        }
    }

    class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ViewHolder> {
        private static final int TYPE_USER = 0;
        private static final int TYPE_BOT = 1;

        private List<ChatMessage> items;

        ChatAdapter(List<ChatMessage> items) { this.items = items; }

        @Override
        public int getItemViewType(int position) {
            return items.get(position).isUser ? TYPE_USER : TYPE_BOT;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            if (viewType == TYPE_USER) {
                View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_user, parent, false);
                return new ViewHolder(v);
            } else {
                View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_bot, parent, false);
                return new ViewHolder(v);
            }
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            holder.tvText.setText(items.get(position).text);
            
            // Animate only the last inserted item
            if (position == items.size() - 1) {
                Animation anim = AnimationUtils.loadAnimation(holder.itemView.getContext(), 
                    items.get(position).isUser ? R.anim.slide_in_right : android.R.anim.fade_in);
                holder.itemView.startAnimation(anim);
            }
        }

        @Override
        public int getItemCount() { return items.size(); }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvText;
            ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvText = itemView.findViewById(R.id.tvChatMessage);
            }
        }
    }
}
