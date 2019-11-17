package com.abhi.dcnutrilabels;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.text.FirebaseVisionText;
import com.google.firebase.ml.vision.text.FirebaseVisionTextRecognizer;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import static android.os.Environment.getExternalStoragePublicDirectory;

public class MainActivity extends AppCompatActivity {

    ImageView retPic;
    Button cameraButton, galleryButton, rotateButton;
    Bitmap bitmap;
    String readText, filePath;
    boolean validImageToAnalyze = false;

    private static final int CAMERA_RESULT = 1, GALLERY_RESULT = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        String [] permissionsForCameraAndExternalStorage = {
                Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};
        requestPermissions(permissionsForCameraAndExternalStorage, GALLERY_RESULT);

        retPic = findViewById(R.id.imageGOC);
        retPic.setEnabled(validImageToAnalyze);
        retPic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                retPic.setEnabled(false);
                Intent analyze = new Intent(getApplicationContext(), PictureAnalysis.class);
                analyze.putExtra("readText", readText);
                startActivity(analyze);
                retPic.setEnabled(bitmap != null);
            }
        });
        cameraButton = findViewById(R.id.cameraButton);
        cameraButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                retrieveImageFromCamera();
            }
        });
        galleryButton = findViewById(R.id.galleryButton);
        galleryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                galleryButton.setEnabled(false);
                Intent openGallery = new Intent(Intent.ACTION_PICK
                        , MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(openGallery, GALLERY_RESULT);
                galleryButton.setEnabled(true);
            }
        });
        rotateButton = findViewById(R.id.rotateButton);
        rotateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                rotateButton.setEnabled(false);
                if (bitmap == null) {
                    Toast noImage = Toast.makeText(getApplicationContext()
                            , "No image to rotate! Take a pic or pull from gallery :)"
                            , Toast.LENGTH_LONG);
                    noImage.show();
                } else {
                    Matrix rotate90 = new Matrix();
                    rotate90.postRotate(90);
                    bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth()
                            , bitmap.getHeight(), rotate90,true);
                    retPic.setImageBitmap(bitmap);
                    runTextRecognition(bitmap);
                }
                rotateButton.setEnabled(true);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == CAMERA_RESULT) {
                //TODO(1): not a rigorous fix. Some phones might don't rotate like this
                bitmap = BitmapFactory.decodeFile(filePath);
                retPic.setImageBitmap(bitmap);
                runTextRecognition(bitmap);
            } else if (requestCode == GALLERY_RESULT) {
                assert data != null : "data should never be null";
                Uri selectedImage = data.getData();
                String[] filePathColumn = {MediaStore.Images.Media.DATA};
                assert selectedImage != null : "selected image is null for some reason";
                Cursor cursor = getContentResolver().query(selectedImage,
                        filePathColumn, null, null, null);
                assert cursor != null : "cursor is null for some reason";
                cursor.moveToFirst();
                int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                filePath = cursor.getString(columnIndex);
                cursor.close();
                bitmap = BitmapFactory.decodeFile(filePath);
                runTextRecognition(bitmap);
                retPic.setImageBitmap(bitmap);
            }
        }
    }

    private void runTextRecognition(Bitmap bitmap) {
        FirebaseVisionImage fbImage = FirebaseVisionImage.fromBitmap(bitmap);
        FirebaseVisionTextRecognizer detector = FirebaseVision.getInstance().getOnDeviceTextRecognizer();
        detector.processImage(fbImage).addOnSuccessListener(new OnSuccessListener<FirebaseVisionText>() {
            @Override
            public void onSuccess(FirebaseVisionText firebaseVisionText) {
                processTextRecognitionResults(firebaseVisionText);
            }
        });


    }

    private void processTextRecognitionResults(FirebaseVisionText firebaseVisionText) {
        List<FirebaseVisionText.TextBlock> blocks = firebaseVisionText.getTextBlocks();
        StringBuilder outputToView = new StringBuilder();
        for (FirebaseVisionText.TextBlock block : blocks) {
            if (block.getText().toLowerCase().contains("ingredients")) {
                outputToView.append(block.getText());
            }
        }
        readText = outputToView.toString();
        String message;
        if (outputToView.length() == 0) {
            message = "No ingredients found! Try rotating or try another image";
            Toast noneGleaned = Toast.makeText(getApplicationContext()
                    , message, Toast.LENGTH_SHORT);
            noneGleaned.show();
        } else {
            message = "We found something!! yay, click on image for analysis";
            Toast gleaned = Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG);
            gleaned.show();
            retPic.setEnabled(validImageToAnalyze = true);
        }
    }

    private void retrieveImageFromCamera() {
        Intent openCamera = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (openCamera.resolveActivity(getPackageManager()) != null) {
            File photoFile;
            photoFile = createPhotoFile();
            if (photoFile != null) {
                filePath = photoFile.getAbsolutePath();
                Uri photoURI = FileProvider.getUriForFile(MainActivity.this,
                        "com.abhi.dcnutrilabels.fileprovider", photoFile);
                openCamera.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(openCamera, CAMERA_RESULT);
            }
        }
    }

    private File createPhotoFile() {
        String datedName = new SimpleDateFormat("yyyymmdd_HH:mm:ss", Locale.ENGLISH).format(new Date());
        File storeDir = getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        File image = null;
        try {
            image = File.createTempFile(datedName, ".jpg", storeDir);
        } catch (IOException e) {
            Log.d("picSave", "IOException Thrown: " + e);
        }
        return image;

    }
}
