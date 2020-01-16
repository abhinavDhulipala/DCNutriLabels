package com.abhi.dcnutrilabels;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class PictureAnalysis extends AppCompatActivity {

    TextView displayReadText;
    String readText;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.picture_analysis);
        Intent receiveIntent = getIntent();
        readText = receiveIntent.getStringExtra("readText");
        displayReadText = findViewById(R.id.readText);
        displayReadText.setText(readText);
    }

    //todo: auto-correct potential misspelled words
    //todo: ranking system
    //todo: retrieve correct data
}
