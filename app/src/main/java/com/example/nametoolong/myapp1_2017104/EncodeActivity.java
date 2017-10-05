package com.example.nametoolong.myapp1_2017104;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ContentUris;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
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
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by NameTooLong on 2017/10/5.
 */

public class EncodeActivity extends Activity implements View.OnClickListener{
    private static final String TAG = "EncodeActivity";

    private EditText editText;
    private Button button_ecode,button_uesLogo;
    private ImageView imageView;

    private String message;
    private int width,height;

    private static final int ASK_PERMISSION=0;
    private static final int CHOOSE_FROM_ALBUM=1;

    @Override
    protected void onCreate( Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ecode);
        editText=(EditText)findViewById(R.id.edit_text_ecode);
        button_ecode=(Button)findViewById(R.id.button_ecode);
        button_ecode.setOnClickListener(this);
        button_uesLogo=(Button)findViewById(R.id.button_useLog_ecode);
        button_uesLogo.setOnClickListener(this);
        imageView=(ImageView)findViewById(R.id.image_ecode);
    }

    @Override
    public void onClick(View v) {
        message=editText.getText().toString();
        editText.setText("");
        if(width<=0||height<=0) {
            width = imageView.getWidth();
            height = imageView.getHeight();
        }
        switch (v.getId()){
            case R.id.button_ecode:
                createImage(null);
                break;
            case R.id.button_useLog_ecode:
                if(ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)!= PackageManager.PERMISSION_GRANTED)
                    ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},ASK_PERMISSION);
                else
                    openAlbum();
                break;
        }
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
        createImage(imagePath);
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
        createImage(imagePath);
    }

    private void openAlbum(){
        Intent intent=new Intent("android.intent.action.GET_CONTENT");
        intent.setType("image/*");
        startActivityForResult(intent,CHOOSE_FROM_ALBUM);
    }

    private void createImage(String imagePath){
        new AsyncTask<String,Bitmap,Void>(){
            @Override
            protected Void doInBackground(String... params) {
                Bitmap bitmap=generateBitmap(params[0],width,height);
                if(params[1]!=null){
                    Bitmap logo=BitmapFactory.decodeFile(params[1]);
                    bitmap=addLogo(bitmap,logo);
                }
                publishProgress(bitmap);
                return null;
            }

            @Override
            protected void onProgressUpdate(Bitmap... values) {
                imageView.setImageBitmap(values[0]);
            }

            private Bitmap generateBitmap(String content, int width, int height) {
                QRCodeWriter qrCodeWriter = new QRCodeWriter();
                Map<EncodeHintType, String> hints = new HashMap<>();
                hints.put(EncodeHintType.CHARACTER_SET, "utf-8");
                try {
                    BitMatrix encode = qrCodeWriter.encode(content, BarcodeFormat.QR_CODE, width, height, hints);
                    int[] pixels = new int[width * height];
                    for (int i = 0; i < height; i++) {
                        for (int j = 0; j < width; j++) {
                            if (encode.get(j, i)) {
                                pixels[i * width + j] = 0x00000000;
                            } else {
                                pixels[i * width + j] = 0xffffffff;
                            }
                        }
                    }
                    return Bitmap.createBitmap(pixels, 0, width, width, height, Bitmap.Config.RGB_565);
                } catch (WriterException e) {
                    e.printStackTrace();
                }
                return null;
            }

            private Bitmap addLogo(Bitmap qrBitmap, Bitmap logoBitmap) {
                int qrBitmapWidth = qrBitmap.getWidth();
                int qrBitmapHeight = qrBitmap.getHeight();
                int logoBitmapWidth = logoBitmap.getWidth();
                int logoBitmapHeight = logoBitmap.getHeight();
                Bitmap blankBitmap = Bitmap.createBitmap(qrBitmapWidth, qrBitmapHeight, Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(blankBitmap);
                canvas.drawBitmap(qrBitmap, 0, 0, null);
                canvas.save(Canvas.ALL_SAVE_FLAG);
                float scaleSize = 1.0f;
                while ((logoBitmapWidth / scaleSize) > (qrBitmapWidth / 5) || (logoBitmapHeight / scaleSize) > (qrBitmapHeight / 5)) {
                    scaleSize *= 2;
                }
                float sx = 1.0f / scaleSize;
                canvas.scale(sx, sx, qrBitmapWidth / 2, qrBitmapHeight / 2);
                canvas.drawBitmap(logoBitmap, (qrBitmapWidth - logoBitmapWidth) / 2, (qrBitmapHeight - logoBitmapHeight) / 2, null);
                canvas.restore();
                return blankBitmap;
            }
        }.execute(message,imagePath);
    }
}
