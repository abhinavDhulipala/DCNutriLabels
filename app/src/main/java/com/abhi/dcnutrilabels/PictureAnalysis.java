package com.abhi.dcnutrilabels;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PictureAnalysis extends AppCompatActivity {

    TextView displayReadText;
    String readText;
    private static final String TXT = "READ TEXT";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.picture_analysis);
        Intent receiveIntent = getIntent();
        readText = receiveIntent.getStringExtra("readText");
        Log.d("quer", readText);
        displayReadText = findViewById(R.id.readText);
        displayReadText.setText(readText);
    }

    private List<String> getQueries() {
        return null;
    }

    private static String printStingArr(String[] arr) {
        StringBuilder out = new StringBuilder();
        return out.toString();
    }

    //todo: auto-correct potential misspelled words
    //todo: ranking system
    //todo: retrieve correct data
}
