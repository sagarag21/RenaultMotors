package com.example.android.ntbn2;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;

public class MainActivity extends AppCompatActivity {
    static int count = 0;

    ImageView imageView;
    EditText editText;
    Button memoryUploadButton;
    Spinner spinner;
    Button btnUpload,btnCapture,btnDisplay;

    String currentPhotoPath = null;
    private static final int IMAGE_REQUEST = 1;
    private static final String apiurl = "http://192.168.0.14/fileuploa.php";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        imageView = (ImageView) findViewById(R.id.im);
        memoryUploadButton = (Button)findViewById(R.id.memoryUploadId);
        editText = (EditText)findViewById(R.id.editTextTextPersonName2);
        spinner = (Spinner)findViewById(R.id.spinner);
        btnCapture = (Button)findViewById(R.id.captureImg);
        btnUpload = (Button)findViewById(R.id.upld);

        File f = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File[] files = f.listFiles();

        if(files.length > 0)
        {
            memoryUploadButton.setVisibility(View.VISIBLE);
        }

        // Spinner Drop down elements
        List<String> categories = new ArrayList<String>();
        categories.add("id-");
        categories.add("carphoto-");
        categories.add("payment-");

         // Creating adapter for spinner
        ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, categories);

        // Drop down layout style - list view with radio button
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        // attaching data adapter to spinner
        spinner.setAdapter(dataAdapter);

        btnUpload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                File f = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
                File[] files = f.listFiles();

                // Get the names of the files by using the .getName() method
                for (int i = 0; i < files.length; i++) {
                    String filePath = files[i].getAbsolutePath();
                    String encodedImage = encodebitmap(BitmapFactory.decodeFile(filePath));
                    uploadtoserver(encodedImage, filePath);
                }

                memoryUploadButton.setVisibility(View.GONE);
            }
        });

    }

    //this code will opens camera and take photo
    public void captureImage(View view) throws InterruptedException {
        Intent cameraintent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (cameraintent.resolveActivity(getPackageManager()) != null) {

            File imagefile = null;

            try {
                imagefile = getImageFile();
            } catch (IOException e) {
                e.printStackTrace();
            }

            if (imagefile != null) {
                Uri imageUri = FileProvider.getUriForFile(this, "com.example.android.fileprovider", imagefile);
                cameraintent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
                startActivityForResult(cameraintent, IMAGE_REQUEST);
            }
        }
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == IMAGE_REQUEST) {
            displayImage();
        }
    }

    public void displayImage() {
        Bitmap bitmap = BitmapFactory.decodeFile(currentPhotoPath);
        imageView.setImageBitmap(bitmap);
        encodebitmap(bitmap);
    }

    private File getImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    private String encodebitmap(Bitmap bitmap)
    {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG,100,byteArrayOutputStream);
        byte[] byteOfImages = byteArrayOutputStream.toByteArray();
        String encodedImage = Base64.encodeToString(byteOfImages, Base64.DEFAULT);
        return encodedImage;
    }


    private void uploadtoserver(final String encodedImage, final String filePath)
    {
        StringRequest request = new StringRequest(Request.Method.POST, apiurl, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                imageView.setImageResource(R.drawable.ic_launcher_background);
                Toast.makeText(getApplicationContext(), "File Uploaded Successfully", Toast.LENGTH_SHORT).show();

                //After Uploading Image to Server the photo will be deleted from device
                new File(filePath).delete();
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(getApplicationContext(), error.toString(), Toast.LENGTH_SHORT).show();
                count++;

                if(count <= 10)
                {
                    try {
                        Thread.sleep(1000);
                    }
                    catch(InterruptedException exception)
                    {
                        //log an exception
                    }
                    uploadtoserver(encodedImage, filePath);
                }
                else
                {
                    count = 0;
                    memoryUploadButton.setVisibility(View.VISIBLE);
                }
            }
        })
        {
            protected Map<String, String> getParams() throws AuthFailureError
            {
                Spinner spinner = (Spinner)findViewById(R.id.spinner);
                String spinnertext = spinner.getSelectedItem().toString();

                Map<String,String> map = new HashMap<String, String>();
                map.put("whatpaperwork", spinnertext);
                map.put("reg", editText.getText().toString());
                map.put("upload", encodedImage);

                return map;
            }
        };

        RequestQueue queue = Volley.newRequestQueue(getApplicationContext());
        queue.add(request);
    }

    public void memoryUpload(View view) {
        File f = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File[] files = f.listFiles();

        // Get the names of the files by using the .getName() method
        for (int i = 0; i < files.length; i++) {
            String filePath = files[i].getAbsolutePath();
            String encodedImage = encodebitmap(BitmapFactory.decodeFile(filePath));
            uploadtoserver(encodedImage, filePath);
        }

        memoryUploadButton.setVisibility(View.GONE);
    }
}