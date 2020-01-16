package com.abhi.dcnutrilabels;

import android.Manifest;
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
import android.widget.ProgressBar;
import android.widget.SearchView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcode;
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcodeDetector;
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcodeDetectorOptions;
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

public class MainActivity extends AppCompatActivity implements OnSuccessListener{

    ImageView retPic;
    Button cameraButton, galleryButton, rotateButton;
    Bitmap bitmap;
    String readText, filePath;
    ProgressBar progressBar;
    SearchView search;
    private StringBuilder textRecogVal, barcodeVal;

    private static final int CAMERA_RESULT = 1, GALLERY_RESULT = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        String [] permissionsForCameraAndExternalStorage = {
                Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};
        requestPermissions(permissionsForCameraAndExternalStorage, GALLERY_RESULT);

        retPic = findViewById(R.id.imageGOC);
        retPic.setEnabled(false);
        retPic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                retPic.setEnabled(false);
                Intent analyze = new Intent(getApplicationContext(), PictureAnalysis.class);
                analyze.putExtra("readText", readText);
                startActivity(analyze);
                retPic.setEnabled(bitmap != null && readText != null && readText.length() > 0);
            }
        });
        progressBar = findViewById(R.id.analysisRunning);
        progressBar.setVisibility(View.INVISIBLE);
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
                    runImageRecognition(FirebaseVisionImage.fromBitmap(bitmap));
                }
                rotateButton.setEnabled(true);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == GALLERY_RESULT) {
                assert data != null : "data should never be null";
                Uri selectedImage = data.getData();
                String[] filePathColumn = {MediaStore.Images.Media.DATA};
                assert selectedImage != null : "selected image is null for some reason";
                Cursor cursor = getContentResolver().query(selectedImage,
                        filePathColumn, null, null, null);
                assert cursor != null : "cursor is null for some reason, shouldn't happen";
                cursor.moveToFirst();
                filePath = cursor.getString(cursor.getColumnIndex(filePathColumn[0]));
                cursor.close();
            }
            bitmap = BitmapFactory.decodeFile(filePath);
            retPic.setImageBitmap(bitmap);
            runImageRecognition(FirebaseVisionImage.fromBitmap(bitmap));
            String message;
            retPic.setEnabled(true);
            Log.d("fin", "final Builders" + readText);
            if (!barcodeVal.toString().isEmpty()) {
                readText = barcodeVal.toString();
                message = "barcode found! Click image to explore product";
            } else if (!textRecogVal.toString().isEmpty()) {
                readText = textRecogVal.toString();
                message = "yay! Ingredients found. Click image to analyze";
            } else {
                readText = "";
                retPic.setEnabled(false);
                message = "oh no! we didn't find a barcode or ingredients rotate the image or" +
                        "choose another picture";
            }
            Toast imageResults = Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT);
            imageResults.show();
        }
    }

    private void runImageRecognition(FirebaseVisionImage fvImage) {
        FirebaseVisionBarcodeDetector barcodeDetector = FirebaseVision.getInstance()
                .getVisionBarcodeDetector(new FirebaseVisionBarcodeDetectorOptions.Builder()
                        .setBarcodeFormats(FirebaseVisionBarcode.FORMAT_UPC_A,
                                FirebaseVisionBarcode.FORMAT_UPC_E).build());
        textRecogVal = new StringBuilder();
        barcodeVal = new StringBuilder();
        barcodeDetector.detectInImage(fvImage).addOnSuccessListener(new OnSuccessListener<List<FirebaseVisionBarcode>>() {
            @Override
            public void onSuccess(List<FirebaseVisionBarcode> firebaseVisionBarcodes) {
                if (!firebaseVisionBarcodes.isEmpty())
                    barcodeVal.append(firebaseVisionBarcodes.get(0).getRawValue());
                Log.d("brcd", "listener: " + barcodeVal);
            }
        });

        FirebaseVisionTextRecognizer detector = FirebaseVision.getInstance().getOnDeviceTextRecognizer();
        detector.processImage(fvImage).addOnSuccessListener(new OnSuccessListener<FirebaseVisionText>() {
            @Override
            public void onSuccess(FirebaseVisionText firebaseVisionText) {
                textRecogVal.append(processTextRecognitionResults(firebaseVisionText));
                if (!textRecogVal.toString().isEmpty()) {
                    readText = textRecogVal.toString();
                }
                Log.d("txtr", "listener: " + textRecogVal);
            }
        });
        Log.d("brcd", "Out: " + barcodeVal);

    }

    private String processTextRecognitionResults(FirebaseVisionText firebaseVisionText) {
        List<FirebaseVisionText.TextBlock> blocks = firebaseVisionText.getTextBlocks();
        StringBuilder ingredients = new StringBuilder();
        for (FirebaseVisionText.TextBlock block : blocks) {
            if (block.getText().toLowerCase().contains("ingredients"))
                ingredients.append(block.getText());
        }
        return ingredients.toString();
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

    @Override
    public void onSuccess(Object o) {}
}
