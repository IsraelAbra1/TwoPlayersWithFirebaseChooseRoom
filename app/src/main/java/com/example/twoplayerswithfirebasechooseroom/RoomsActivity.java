package com.example.twoplayerswithfirebasechooseroom;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class RoomsActivity extends AppCompatActivity {

    private DatabaseReference roomsRef;
    private String playerId;

    private Button btnQuickJoin;
    private Button btnCreateRoom;
    private EditText etRoomId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rooms);

        roomsRef = FirebaseDatabase.getInstance().getReference("rooms");
        playerId = UUID.randomUUID().toString();  // Generate a unique ID for the player

        btnQuickJoin = findViewById(R.id.btnJoinRoom);
        btnCreateRoom = findViewById(R.id.btnCreateRoom);
        etRoomId = findViewById(R.id.etRoomId);

        // When user clicks "Join Room"
        btnQuickJoin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Pass whatever they typed in the EditText to the function
                String typedCode = etRoomId.getText().toString().trim();
                quickJoin(typedCode);
            }
        });

        // When user clicks "Create Room"
        if (btnCreateRoom != null) {
            btnCreateRoom.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    createRoomAndGoLobby();
                }
            });
        }
    }

    private void quickJoin(String roomCode)
    {
        // 1. If the user left the text box empty, just create a new room for them
        if (roomCode.isEmpty()) {
            Toast.makeText(this, "No code entered", Toast.LENGTH_SHORT).show();
            return;
        }

        // 2. Search Firebase for 1 room where the "code" equals the code the user typed
        Query q = roomsRef.orderByChild("code").equalTo(roomCode).limitToFirst(1);

        q.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    // Room found! Extract the real Firebase ID
                    DataSnapshot roomSnap = snapshot.getChildren().iterator().next();// "קח את הפריט הראשון שמצאת ברשימה ושמור אותו במשתנה בשם roomSnap".
                    String realRoomId = roomSnap.getKey();

                    // Check if the room is still waiting for players
                    String status = roomSnap.child("status").getValue(String.class);
                    if ("waiting".equals(status)) {
                        goLobby(realRoomId);
                    } else {
                        Toast.makeText(RoomsActivity.this, "This room is already playing!", Toast.LENGTH_SHORT).show();
                    }

                } else {
                    // No room found with that code
                    Toast.makeText(RoomsActivity.this, "Room not found! Check the code.", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(RoomsActivity.this, "Firebase error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void createRoomAndGoLobby() {
        String roomId = roomsRef.push().getKey(); // Create a new room ID in the Firebase
        if (roomId == null) {
            Toast.makeText(this, "שגיאה ביצירת חדר", Toast.LENGTH_SHORT).show();
            return;
        }

        // קוד חדר קצר (6 ספרות)
        String roomCode = String.valueOf(100000 + new java.util.Random().nextInt(900000));

        Map<String, Object> room = new HashMap<>();
        room.put("hostId", playerId);
        room.put("status", "waiting");
        room.put("code", roomCode); // Saving the 6-digit code here

        Map<String, Object> players = new HashMap<>();
        players.put(playerId, true);
        room.put("players", players);

        // Explicit traditional callback for simple Java consistency
        roomsRef.child(roomId).setValue(room, new DatabaseReference.CompletionListener() {
            @Override
            public void onComplete(DatabaseError error, @NonNull DatabaseReference ref) {
                if (error != null) {
                    Toast.makeText(RoomsActivity.this, "שגיאה: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    return;
                }
                goLobby(roomId);
            }
        });
    }

    private void goLobby(String roomId) {
        Intent i = new Intent(this, RoomLobbyActivity.class);
        i.putExtra("roomId", roomId);
        i.putExtra("playerId", playerId);
        startActivity(i);

        finish(); // never return to this activity
    }

}