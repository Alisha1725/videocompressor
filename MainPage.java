package com.example.aryanvermaproject;

import android.Manifest;
import android.app.Dialog;
import android.app.DownloadManager;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.iceteck.silicompressorr.SiliCompressor;

import java.io.File;
import java.net.URISyntaxException;

public class MainPage extends AppCompatActivity {
    Button btSelect,btndownload;
    VideoView videoview1,videoview2;
    TextView textview1,textview2,textview3;
    StorageReference storage;
    public static String videourl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_page);

        storage= FirebaseStorage.getInstance().getReference();
        btSelect=findViewById(R.id.btnselect);
        btndownload=findViewById(R.id.btndownload);
        videoview1=findViewById(R.id.videoview);
        videoview2=findViewById(R.id.videoview2);
        textview1=findViewById(R.id.textview);
        textview2=findViewById(R.id.textview2);
        textview3=findViewById(R.id.textview3);

        btSelect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ContextCompat.checkSelfPermission(MainPage.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        == PackageManager.PERMISSION_GRANTED){
                    selectVideo();
                }
                else {
                    ActivityCompat.requestPermissions(MainPage.this,
                            new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},1);
                }
            }
        });
    }

    private void selectVideo() {
        Intent intent=new Intent(Intent.ACTION_PICK);
        intent.setType("video/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent,"Select Video"),100);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode==1 && grantResults.length>0 && grantResults[0]==PackageManager.PERMISSION_GRANTED){
            selectVideo();
        }
        else {
            Toast.makeText(getApplicationContext(),"Permission Denied!",Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode==100 && resultCode==RESULT_OK && data!=null){
            Uri uri=data.getData();
            videoview1.setVideoURI(uri);
            File file=new File(Environment.getExternalStorageDirectory().getAbsolutePath());
            new CompressVideo().execute("false",uri.toString(),file.getPath());
        }
    }

    private class CompressVideo extends AsyncTask<String,String,String> {
        Dialog dialog;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            dialog= ProgressDialog.show(MainPage.this,"","Compressing...");
        }

        @Override
        protected String doInBackground(String... strings) {

            String videoPath=null;
            try {
                Uri uri=Uri.parse(strings[1]);
                videoPath= SiliCompressor.with(MainPage.this).compressVideo(uri,strings[2]);
            }
            catch (URISyntaxException e){
                e.printStackTrace();
            }
            return videoPath;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            dialog.dismiss();

            videoview1.setVisibility(View.VISIBLE);
            textview1.setVisibility(View.VISIBLE);
            videoview2.setVisibility(View.VISIBLE);
            textview2.setVisibility(View.VISIBLE);
            textview3.setVisibility(View.VISIBLE);
            btndownload.setVisibility(View.VISIBLE);

            File file=new File(s);
            Uri uri=Uri.fromFile(file);
            videoview2.setVideoURI(uri);
            FirebaseDatabase database=FirebaseDatabase.getInstance();
            StorageReference videoreferance=storage.child("compressedvideos/"+System.currentTimeMillis()+".mp4");
            videoreferance.putFile(uri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    videoreferance.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                        @Override
                        public void onSuccess(Uri uri) {
                            videourl=uri.toString();
                        }
                    });
                }
            });
            videoview1.start();
            videoview2.start();

            float size=file.length()/1024f;
            textview3.setText(String.format("Size: %.2f KB",size));

            btndownload.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
            videoreferance.getMetadata().addOnSuccessListener(new OnSuccessListener<StorageMetadata>() {
                @Override
                public void onSuccess(StorageMetadata storageMetadata) {
                    String fileName= storageMetadata.getName();
                    String fileType= storageMetadata.getContentType();
                    String fileDirectory=Environment.DIRECTORY_DOWNLOADS;
                    DownloadManager downloadManager=(DownloadManager) getBaseContext().getSystemService(DOWNLOAD_SERVICE);
                    Uri uri1=uri.parse(videourl);
                    DownloadManager.Request request=new DownloadManager.Request(uri1);
                    request.setTitle("Compressed Video");
                    request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                    request.setDestinationInExternalPublicDir(""+fileDirectory,""+fileName);
                    downloadManager.enqueue(request);
                }
            });
                    Toast.makeText(MainPage.this, "Video Downloading", Toast.LENGTH_LONG).show();
                }
            });
        }
    }

}
