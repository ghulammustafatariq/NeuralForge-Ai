package com.superior.mindforgeai;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PlaylistActivity extends AppCompatActivity {

    private static final String[] COLORS = {
            "#6366F1","#8B5CF6","#EC4899","#EF4444",
            "#F97316","#EAB308","#22C55E","#06B6D4","#3B82F6"
    };
    private static final String[] EMOJIS = {
            "📚","🧠","⚗️","🔬","💻","📐","🎯","🌍","🎨","✍️","🚀","💡"
    };

    private RecyclerView rvPlaylists, rvAllNotes;
    private TextView tabPlaylists, tabAllNotes;
    private AppDatabase db;
    private String uid;
    private PlaylistAdapter playlistAdapter;
    private HistoryAdapter allNotesAdapter;
    private int selectedColorIndex = 0;
    private int selectedEmojiIndex = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_playlist);

        db = AppDatabase.getInstance(this);
        uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : "anonymous";

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.playlistRoot), (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });

        rvPlaylists = findViewById(R.id.rvPlaylists);
        rvAllNotes = findViewById(R.id.rvAllNotes);
        tabPlaylists = findViewById(R.id.tabPlaylists);
        tabAllNotes = findViewById(R.id.tabAllNotes);

        setupPlaylistGrid();
        setupAllNotes();
        setupTabs();

        ExtendedFloatingActionButton fab = findViewById(R.id.fabNewPlaylist);
        fab.setOnClickListener(v -> showCreatePlaylistDialog());

        loadPlaylists();
        loadAllNotes();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh counts every time user returns (e.g. after adding a note to a collection)
        loadPlaylists();
        loadAllNotes();
    }

    private void setupTabs() {
        tabPlaylists.setOnClickListener(v -> showTab(true));
        tabAllNotes.setOnClickListener(v -> showTab(false));
    }

    private void showTab(boolean showPlaylists) {
        if (showPlaylists) {
            rvPlaylists.setVisibility(View.VISIBLE);
            rvAllNotes.setVisibility(View.GONE);
            tabPlaylists.setBackgroundResource(R.drawable.bg_pill_purple);
            tabPlaylists.getBackground().setTint(getColor(R.color.brand_purple));
            tabPlaylists.setTextColor(getColor(R.color.text_on_brand));
            tabPlaylists.setTypeface(android.graphics.Typeface.create("sans-serif-black", android.graphics.Typeface.NORMAL));
            tabAllNotes.setBackgroundColor(Color.TRANSPARENT);
            tabAllNotes.setTextColor(getColor(R.color.text_sub));
            tabAllNotes.setTypeface(android.graphics.Typeface.create("sans-serif-bold", android.graphics.Typeface.NORMAL));
        } else {
            rvPlaylists.setVisibility(View.GONE);
            rvAllNotes.setVisibility(View.VISIBLE);
            tabAllNotes.setBackgroundResource(R.drawable.bg_pill_purple);
            tabAllNotes.getBackground().setTint(getColor(R.color.brand_purple));
            tabAllNotes.setTextColor(getColor(R.color.text_on_brand));
            tabAllNotes.setTypeface(android.graphics.Typeface.create("sans-serif-black", android.graphics.Typeface.NORMAL));
            tabPlaylists.setBackgroundColor(Color.TRANSPARENT);
            tabPlaylists.setTextColor(getColor(R.color.text_sub));
            tabPlaylists.setTypeface(android.graphics.Typeface.create("sans-serif-bold", android.graphics.Typeface.NORMAL));
        }
    }

    // ── Playlists Grid ──────────────────────────────────────────────────
    private void setupPlaylistGrid() {
        playlistAdapter = new PlaylistAdapter();
        rvPlaylists.setLayoutManager(new GridLayoutManager(this, 2));
        rvPlaylists.setAdapter(playlistAdapter);
    }

    private void loadPlaylists() {
        new Thread(() -> {
            List<PlaylistEntity> playlists = db.playlistDao().getAllByUid(uid);
            // Build display list: "All Notes" special card first
            List<PlaylistItem> items = new ArrayList<>();
            // All Notes card
            int totalCount = db.historyDao().getAllByUid(uid).size();
            items.add(new PlaylistItem(null, "All Notes", "#6366F1", "🗂️", totalCount));
            for (PlaylistEntity p : playlists) {
                int count = db.historyDao().countByPlaylist(uid, p.getId());
                items.add(new PlaylistItem(p, p.getName(), p.getColor(), p.getEmoji(), count));
            }
            runOnUiThread(() -> playlistAdapter.setItems(items));
        }).start();
    }

    // ── All Notes ────────────────────────────────────────────────────────
    private void setupAllNotes() {
        allNotesAdapter = new HistoryAdapter(new ArrayList<>(), entity -> {
            Intent intent = new Intent(this, ContentActivity.class);
            intent.putExtra("TOPIC", entity.getTopic());
            intent.putExtra("AI_RESULT", entity.getAiResult());
            intent.putExtra("DOC_ID", entity.getId());
            startActivity(intent);
        });
        rvAllNotes.setLayoutManager(new LinearLayoutManager(this));
        rvAllNotes.setAdapter(allNotesAdapter);
    }

    private void loadAllNotes() {
        new Thread(() -> {
            List<HistoryEntity> notes = db.historyDao().getAllByUid(uid);
            runOnUiThread(() -> allNotesAdapter.setItems(notes));
        }).start();
    }

    // ── Create Playlist Dialog ──────────────────────────────────────────
    private void showCreatePlaylistDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_input_single, null);
        EditText etName = dialogView.findViewById(R.id.etField1);
        etName.setHint("Collection name (e.g. Machine Learning)");

        new MaterialAlertDialogBuilder(this, R.style.MindForge_Dialog)
                .setTitle("New Collection")
                .setView(dialogView)
                .setPositiveButton("Create", (d, w) -> {
                    String name = etName.getText().toString().trim();
                    if (name.isEmpty()) { Toast.makeText(this, "Enter a name", Toast.LENGTH_SHORT).show(); return; }
                    String color = COLORS[selectedColorIndex % COLORS.length];
                    String emoji = EMOJIS[selectedEmojiIndex % EMOJIS.length];
                    selectedColorIndex++; selectedEmojiIndex++;
                    PlaylistEntity p = new PlaylistEntity(UUID.randomUUID().toString(), uid,
                            name, color, emoji, System.currentTimeMillis());
                    new Thread(() -> {
                        db.playlistDao().insert(p);
                        runOnUiThread(() -> {
                            Toast.makeText(this, "\"" + name + "\" created!", Toast.LENGTH_SHORT).show();
                            loadPlaylists();
                        });
                    }).start();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // ── Playlist Adapter ─────────────────────────────────────────────────
    static class PlaylistItem {
        PlaylistEntity entity; // null = "All Notes"
        String name, color, emoji;
        int count;
        PlaylistItem(PlaylistEntity entity, String name, String color, String emoji, int count) {
            this.entity = entity; this.name = name;
            this.color = color; this.emoji = emoji; this.count = count;
        }
    }

    class PlaylistAdapter extends RecyclerView.Adapter<PlaylistAdapter.VH> {
        private List<PlaylistItem> items = new ArrayList<>();

        void setItems(List<PlaylistItem> list) { items = list; notifyDataSetChanged(); }

        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_playlist_card, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int pos) {
            PlaylistItem item = items.get(pos);
            h.tvEmoji.setText(item.emoji);
            h.tvName.setText(item.name);
            h.tvCount.setText(item.count + (item.count == 1 ? " note" : " notes"));
            try {
                h.root.setBackgroundColor(Color.parseColor(item.color));
            } catch (Exception e) {
                h.root.setBackgroundColor(Color.parseColor("#6366F1"));
            }
            h.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(PlaylistActivity.this, HistoryActivity.class);
                if (item.entity != null) {
                    intent.putExtra("PLAYLIST_ID", item.entity.getId());
                    intent.putExtra("PLAYLIST_NAME", item.name);
                } else {
                    intent.putExtra("PLAYLIST_NAME", "All Notes");
                }
                startActivity(intent);
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            });
            h.itemView.setOnLongClickListener(v -> {
                if (item.entity != null) {
                    new MaterialAlertDialogBuilder(PlaylistActivity.this, R.style.MindForge_Dialog)
                            .setTitle("Delete \"" + item.name + "\"?")
                            .setMessage("What do you want to do?")
                            .setPositiveButton("Delete Collection Only", (d, w) -> {
                                new Thread(() -> {
                                    db.playlistDao().delete(item.entity);
                                    runOnUiThread(() -> { loadPlaylists(); Toast.makeText(PlaylistActivity.this, "Collection deleted, notes kept", Toast.LENGTH_SHORT).show(); });
                                }).start();
                            })
                            .setNegativeButton("Delete All Notes Too", (d, w) -> {
                                new MaterialAlertDialogBuilder(PlaylistActivity.this, R.style.MindForge_Dialog)
                                    .setTitle("Delete Everything?")
                                    .setMessage("This will permanently delete the collection and ALL " + item.count + " notes inside it. This cannot be undone.")
                                    .setPositiveButton("Yes, Delete All", (d2, w2) -> {
                                        new Thread(() -> {
                                            List<HistoryEntity> notes = db.historyDao().getByPlaylistId(uid, item.entity.getId());
                                            for (HistoryEntity n : notes) {
                                                db.historyDao().delete(n);
                                                if (FirebaseAuth.getInstance().getCurrentUser() != null) {
                                                    FirebaseFirestore.getInstance().collection("users")
                                                        .document(FirebaseAuth.getInstance().getCurrentUser().getUid())
                                                        .collection("history").document(n.getId()).delete();
                                                }
                                            }
                                            db.playlistDao().delete(item.entity);
                                            runOnUiThread(() -> { loadPlaylists(); Toast.makeText(PlaylistActivity.this, "Deleted collection and all notes", Toast.LENGTH_SHORT).show(); });
                                        }).start();
                                    })
                                    .setNegativeButton("Cancel", null).show();
                            })
                            .setNeutralButton("Cancel", null).show();
                }
                return true;
            });
        }

        @Override public int getItemCount() { return items.size(); }

        class VH extends RecyclerView.ViewHolder {
            TextView tvEmoji, tvName, tvCount; LinearLayout root;
            VH(@NonNull View v) {
                super(v);
                tvEmoji = v.findViewById(R.id.tvPlaylistEmoji);
                tvName = v.findViewById(R.id.tvPlaylistName);
                tvCount = v.findViewById(R.id.tvNoteCount);
                root = v.findViewById(R.id.playlistCardRoot);
            }
        }
    }
}
