package com.example.nametoolong.myapp1_2017104;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ContentUris;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import cn.bingoogolapple.qrcode.core.QRCodeView;
import cn.bingoogolapple.qrcode.zbar.ZBarView;
import cn.bingoogolapple.qrcode.zxing.QRCodeDecoder;

/**
 * Created by NameTooLong on 2017/10/4.
 */

public class ScanActivity extends Activity implements QRCodeView.Delegate,View.OnClickListener{
    private static final String TAG = "ScanActivity";

    private QRCodeView qrCodeView;
    private Button button_spot,button_openAlbum;
    private TextView textView;

    private static final int CHOOSE_FROM_ALBUM=0;
    private static final int ASK_PERMISSION=1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan);
        qrCodeView=(ZBarView)findViewById(R.id.zBarView);
        qrCodeView.setDelegate(this);
        button_spot=(Button)findViewById(R.id.button_scan);
        button_spot.setOnClickListener(this);
        button_openAlbum=(Button)findViewById(R.id.button_openAlbum_scan);
        button_openAlbum.setOnClickListener(this);
        textView=(TextView)findViewById(R.id.text_scan);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.button_scan:
                qrCodeView.startSpot();
                break;
            case R.id.button_openAlbum_scan:
                if(ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)!= PackageManager.PERMISSION_GRANTED)
                    ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},ASK_PERMISSION);
                else
                    openAlbum();
                break;
        }
    }

    private void openAlbum(){
        Intent intent=new Intent("android.intent.action.GET_CONTENT");
        intent.setType("image/*");
        startActivityForResult(intent,CHOOSE_FROM_ALBUM);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,String[] permissions,int[] grantResults) {
        switch (requestCode){
            case ASK_PERMISSION:
                if(grantResults.length>0&&grantResults[0]==PackageManager.PERMISSION_GRANTED)
                    openAlbum();
                else
                    Toast.makeText(this,"Permission denied!",Toast.LENGTH_SHORT).show();
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode){
            case CHOOSE_FROM_ALBUM:
                if(Build.VERSION.SDK_INT>=19)
                    handleImageOnKitKat(data);
                else
                    handleImageBeforeKitKat(data);
                break;
        }
    }

    @TargetApi(21)
    private void handleImageOnKitKat(Intent data){
        String imagePath=null;
        Uri uri=data.getData();
        if(DocumentsContract.isDocumentUri(this,uri)){
            String docId=DocumentsContract.getDocumentId(uri);
            if("com.android.providers.media.documents".equals(uri.getAuthority())){
                String id=docId.split(":")[1];
                String selection= MediaStore.Images.Media._ID+"="+id;
                imagePath=getImagePath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,selection);
            }
            else if("com.example.providers.downloads.documents".equals(uri.getAuthority())){
                Uri contentUri= ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"),Long.valueOf(docId));
                imagePath=getImagePath(contentUri,null);
            }
        }
        else if("content".equalsIgnoreCase(uri.getScheme()))
            imagePath=getImagePath(uri,null);
        else if("file".equalsIgnoreCase(uri.getScheme()))
            imagePath=uri.getPath();
        spotImageFromAlbum(imagePath);
    }

    private void spotImageFromAlbum(final String imagePath){
        if(imagePath==null)
            Toast.makeText(this,"Can't get image path correctly",Toast.LENGTH_SHORT).show();
        else {
            new AsyncTask<String,String,Void>(){
                @Override
                protected Void doInBackground(String... params) {
                    String message=QRCodeDecoder.syncDecodeQRCode(params[0]);
                    publishProgress(message);
                    return null;
                }

                @Override
                protected void onProgressUpdate(String... values) {
                    textView.setText("result:"+values[0]);
                }
            }.execute(imagePath);
        }
    }

    private String getImagePath(Uri uri,String selection){
        String imagePath=null;
        Cursor cursor=getContentResolver().query(uri,null,selection,null,null);
        if(cursor!=null){
            if(cursor.moveToFirst())
                imagePath=cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
            cursor.close();
        }
        return imagePath;
    }

    private void handleImageBeforeKitKat(Intent data){
        Uri uri=data.getData();
        String imagePath=getImagePath(uri,null);
        spotImageFromAlbum(imagePath);
    }

    @Override
    protected void onStart() {
        super.onStart();
        qrCodeView.startCamera();
        qrCodeView.showScanRect();
    }

    @Override
    protected void onStop() {
        super.onStop();
        qrCodeView.hiddenScanRect();
        qrCodeView.stopCamera();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        qrCodeView.onDestroy();
    }

    @Override
    public void onScanQRCodeOpenCameraError() {
        Toast.makeText(this,"Can't spot!",Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onScanQRCodeSuccess(String result) {
        textView.setText("result:"+result);
    }
}
