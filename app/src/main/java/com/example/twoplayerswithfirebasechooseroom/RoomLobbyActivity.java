package com.example.twoplayerswithfirebasechooseroom;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class RoomLobbyActivity extends AppCompatActivity {

    private TextView tvRoomId;
    private TextView tvWaiting;
    private Button btnBackLobby;

    private DatabaseReference roomRef;
    private ValueEventListener roomListener;

    private String roomId;
    private String playerId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_room_lobby);

        // IDs מה-XML שלך
        tvRoomId = findViewById(R.id.tvRoomId);
        tvWaiting = findViewById(R.id.tvWaiting);
        btnBackLobby = findViewById(R.id.btnBackLobby);

        // extras
        roomId = getIntent().getStringExtra("roomId");
        playerId = getIntent().getStringExtra("playerId");

        if (roomId == null || playerId == null) {
            Toast.makeText(this, "Missing roomId/playerId", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        roomRef = FirebaseDatabase.getInstance().getReference("rooms").child(roomId);

        // הוספת השחקן הנוכחי לרשימת השחקנים בחדר (עבור השחקן המצטרף)
        roomRef.child("players").child(playerId).setValue(true);

        // כפתור חזרה
        btnBackLobby.setOnClickListener(v -> finish());

        listenRoom();
    }

    private void listenRoom() {
        roomListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    Toast.makeText(RoomLobbyActivity.this, "Room does not exist", Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }

                // מציגים קוד קצר במקום roomId הארוך
                String code = snapshot.child("code").getValue(String.class);
                if (code != null && !code.isEmpty()) {
                    tvRoomId.setText(code);
                } else {
                    // אם משום מה אין code עדיין, מראים מקפים
                    tvRoomId.setText("-----");
                }

                long playersCount = snapshot.child("players").getChildrenCount();
                tvWaiting.setText("Waiting for another player (" + playersCount + "/2)");

                String status = snapshot.child("status").getValue(String.class);
                String hostId = snapshot.child("hostId").getValue(String.class);

                // אם יש 2 שחקנים והסטטוס הוא עדיין waiting, המארח מעדכן ל-started כדי להתחיל את המשחק
                if (playersCount >= 2 && "waiting".equals(status)) {
                    if (playerId.equals(hostId)) {
                        roomRef.child("status").setValue("started");
                    }
                }

                if ("started".equals(status)) {
                    goToGame();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(RoomLobbyActivity.this, "Firebase error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        };

        roomRef.addValueEventListener(roomListener);
    }

    private void goToGame() {
        // למנוע מעבר כפול (אם מגיעים כמה עדכונים מהר)
        if (isFinishing()) return;

        Intent i = new Intent(this, GameActivity.class);
        i.putExtra("roomId", roomId);
        i.putExtra("playerId", playerId);
        startActivity(i);

        // שלא יחזרו ללובי ב-Back מתוך המשחק
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (roomRef != null && roomListener != null) {
            roomRef.removeEventListener(roomListener);
        }
    }
}