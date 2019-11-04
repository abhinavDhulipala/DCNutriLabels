package com.abhi.dcnutrilabels;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import android.Manifest;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

import static android.os.Environment.getExternalStoragePublicDirectory;

public class MainActivity extends AppCompatActivity {

    Button cameraButton, galleryButton;
    ImageView retPic;
    String filePath;
    Bitmap bitmap;
    private static final int CAMERA_RESULT = 1, GALLERY_RESULT = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (Build.VERSION.SDK_INT >= 23) {
            String [] permissionsForCameraAndExternalStorage = {
                    Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};
            requestPermissions(permissionsForCameraAndExternalStorage, 2);
        }

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
                Intent openGallery = new Intent(Intent.ACTION_PICK
                        , MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(openGallery, GALLERY_RESULT);
            }
        });
        retPic = findViewById(R.id.imageGOC);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == CAMERA_RESULT) {
                bitmap = BitmapFactory.decodeFile(filePath);
                retPic.setImageBitmap(bitmap);
            } else if (requestCode == GALLERY_RESULT) {
                assert data != null : "data should never be null";
                Uri selectedImage = data.getData();
                String[] filePathColumn = { MediaStore.Images.Media.DATA };
                assert selectedImage != null : "selected image is null for some reason";
                Cursor cursor = getContentResolver().query(selectedImage,
                        filePathColumn, null, null, null);
                assert cursor != null : "cursor is null for some reason";
                cursor.moveToFirst();

                int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                String picturePath = cursor.getString(columnIndex);
                cursor.close();

                ImageView imageView = findViewById(R.id.imageGOC);
                imageView.setImageBitmap(BitmapFactory.decodeFile(picturePath));
            }

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
            Log.d("picSave", "Exception Thrown: " + e);
        }
        return image;

    }
}
