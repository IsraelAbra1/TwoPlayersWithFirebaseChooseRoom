package com.example.twoplayerswithfirebasechooseroom;

import android.os.Bundle;
import android.view.View;
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

public class GameActivity extends AppCompatActivity {

    private String roomId;
    private String playerId;
    private DatabaseReference roomRef;

    private TextView tvTurnStatus;
    private TextView tvGameInfo;
    private Button btnAction;

    private boolean isMyTurn = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        roomId = getIntent().getStringExtra("roomId");
        playerId = getIntent().getStringExtra("playerId");

        if (roomId == null || playerId == null) {
            Toast.makeText(this, "Error: Missing Game Data", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        roomRef = FirebaseDatabase.getInstance().getReference("rooms").child(roomId);

        tvTurnStatus = findViewById(R.id.tvTurnStatus);
        tvGameInfo = findViewById(R.id.tvGameInfo);
        btnAction = findViewById(R.id.btnAction);

        // Initialize turn if not set (Host starts)
        roomRef.child("turn").get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult().getValue() == null) {
                roomRef.child("turn").setValue(playerId);
            }
        });

        listenToTurn();

        btnAction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isMyTurn) {
                    makeMove();
                }
            }
        });
    }

    private void listenToTurn() {
        roomRef.child("turn").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String currentTurnId = snapshot.getValue(String.class);
                if (currentTurnId == null) return;

                isMyTurn = currentTurnId.equals(playerId);
                updateUI();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void updateUI() {
        if (isMyTurn) {
            tvTurnStatus.setText("IT'S YOUR TURN!");
            tvTurnStatus.setTextColor(0xFF4CAF50); // Green
            btnAction.setEnabled(true);
            btnAction.setAlpha(1.0f);
        } else {
            tvTurnStatus.setText("Waiting for opponent...");
            tvTurnStatus.setTextColor(0xFFF44336); // Red
            btnAction.setEnabled(false);
            btnAction.setAlpha(0.5f);
        }
    }

    private void makeMove() {
        // Here you would add your actual game logic.
        // For now, we just switch turns to the other player.
        
        roomRef.child("players").get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                for (DataSnapshot playerSnapshot : task.getResult().getChildren()) {
                    String otherPlayerId = playerSnapshot.getKey();
                    if (!otherPlayerId.equals(playerId)) {
                        // Switch turn to the other player
                        roomRef.child("turn").setValue(otherPlayerId);
                        break;
                    }
                }
            }
        });
    }
}