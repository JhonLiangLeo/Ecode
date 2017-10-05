package com.example.nametoolong.myapp1_2017104;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private Button button,button_ecode;
    private TextView textView;

    private List<String> permissionList=new ArrayList<>();

    private static final int PERMISSION_FOR_SCAN=0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textView=(TextView)findViewById(R.id.text_main);
        button=(Button)findViewById(R.id.button_main);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addPermissionToList(Manifest.permission.CAMERA);
                //addPermissionToList(Manifest.permission.WRITE_EXTERNAL_STORAGE);
                if(!permissionList.isEmpty()) {
                    String permissions[]=permissionList.toArray(new String[permissionList.size()]);
                    ActivityCompat.requestPermissions(MainActivity.this, permissions,PERMISSION_FOR_SCAN);
                }
                else
                    startScan();
                permissionList.clear();
                //permissionList=null;
            }
        });
        button_ecode=(Button)findViewById(R.id.button_ecode_main);
        button_ecode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent=new Intent(MainActivity.this,EncodeActivity.class);
                startActivity(intent);
            }
        });
    }

    private void startScan(){
        Intent intent=new Intent(this,ScanActivity.class);
        startActivity(intent);
    }

    private void addPermissionToList(String permission){
        if(ContextCompat.checkSelfPermission(this,permission)!= PackageManager.PERMISSION_GRANTED)
            permissionList.add(permission);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,String[] permissions, int[] grantResults) {
        switch (requestCode){
            case PERMISSION_FOR_SCAN:
                if(permissions.length>0){
                    for(int i=0;i<grantResults.length;i++){
                        if(grantResults[i]!=PackageManager.PERMISSION_GRANTED){
                            Toast.makeText(this,"Permission:"+permissions[i]+" denied!",Toast.LENGTH_SHORT).show();
                            return;
                        }
                    }
                    startScan();
                }
                break;
        }
    }
}
