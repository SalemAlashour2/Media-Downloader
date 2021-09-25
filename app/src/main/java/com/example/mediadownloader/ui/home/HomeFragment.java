package com.example.mediadownloader.ui.home;

import static android.os.Environment.DIRECTORY_DOWNLOADS;
import static android.os.Environment.getDownloadCacheDirectory;
import static android.os.Environment.getExternalStorageDirectory;

import android.Manifest;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.example.mediadownloader.MainActivity;
import com.example.mediadownloader.R;
import com.example.mediadownloader.databinding.FragmentHomeBinding;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

import retrofit2.http.Header;

public class HomeFragment extends Fragment {

    private static final int PERMISSION_STORAGE_CODE = 1000;
    private HomeViewModel homeViewModel;
    private FragmentHomeBinding binding;
    private EditText urltext;
    private Button downloadbtn;
    SharedPreferences preferenceManager;
    String Download_ID = "DOWNLOAD_ID";
    DownloadManager downloadManager;
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        homeViewModel =
                new ViewModelProvider(this).get(HomeViewModel.class);

        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        urltext = binding.url;
        downloadbtn = binding.downloadbutton;

        preferenceManager = PreferenceManager.getDefaultSharedPreferences(getContext());
        downloadManager = (DownloadManager)getActivity().getSystemService(getContext().DOWNLOAD_SERVICE);

        downloadbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Uri Download_Uri = Uri.parse(urltext.getText().toString());
                DownloadManager.Request request = new DownloadManager.Request(Download_Uri);
                long download_id = downloadManager.enqueue(request);

                //Save the download id
                SharedPreferences.Editor PrefEdit = preferenceManager.edit();
                PrefEdit.putLong(Download_ID, download_id);
                PrefEdit.commit();
                getActivity().registerReceiver(downloadReceiver,new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));

            }
        });

//        downloadbtn.setOnClickListener(new View.OnClickListener() {
//            @RequiresApi(api = Build.VERSION_CODES.N)
//            @Override
//            public void onClick(View v) {
//                    if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.M)
//                    {
//                        if(ActivityCompat.checkSelfPermission(getContext(),Manifest.permission.WRITE_EXTERNAL_STORAGE)== PackageManager.PERMISSION_DENIED)
//                        {
//                             //Premission denied , request it
//                             String[] permissions = {Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.INTERNET};
//                             requestPermissions(permissions,PERMISSION_STORAGE_CODE);
//                        }
//                        else{
//                               //Premission already granted perform download
//                            startDownload();
//                        }
//
//                    }else{
//                          //system os is less than marshmellow perform download
//
//                        startDownload();
//                    }
//            }
//        });
        return root;
    }
    private BroadcastReceiver downloadReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context arg0, Intent arg1) {
            // TODO Auto-generated method stub
            DownloadManager.Query query = new DownloadManager.Query();
            query.setFilterById(preferenceManager.getLong(Download_ID, 0));
            Cursor cursor = downloadManager.query(query);

            if(cursor.moveToFirst()){
                int columnIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS);
                int status = cursor.getInt(columnIndex);
                int columnReason = cursor.getColumnIndex(DownloadManager.COLUMN_REASON);
                int columnfilename = cursor.getColumnIndex(DownloadManager.COLUMN_TITLE);
                int columnExtn = cursor.getColumnIndex(DownloadManager.COLUMN_MEDIA_TYPE);
                int reason = cursor.getInt(columnReason);
                String filename = cursor.getString(columnfilename);
                String fileExtn = cursor.getString(columnExtn);

                if(status == DownloadManager.STATUS_SUCCESSFUL){
                    //Retrieve the saved download id
                    long downloadID = preferenceManager.getLong(Download_ID, 0);

                    ParcelFileDescriptor file;

                    try {
                        file = downloadManager.openDownloadedFile(downloadID);
                        writeFileOnInternalStorage(getContext(),file,filename,fileExtn);

                        System.out.println("SALEM"+file);
                       Toast.makeText(getContext(),
                                "File Downloaded: " + file.toString(),
                               Toast.LENGTH_LONG).show();
                    } catch (FileNotFoundException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                        Toast.makeText(getContext(),
                                e.toString(),
                                Toast.LENGTH_LONG).show();
                    }

                }else if(status == DownloadManager.STATUS_FAILED){
//                    Toast.makeText(AndroidDownloadActivity.this,
//                            "FAILED!\n" + "reason of " + reason,
//                            Toast.LENGTH_LONG).show();
                }else if(status == DownloadManager.STATUS_PAUSED){
//                    Toast.makeText(AndroidDownloadActivity.this,
//                            "PAUSED!\n" + "reason of " + reason,
//                            Toast.LENGTH_LONG).show();
                }else if(status == DownloadManager.STATUS_PENDING){
//                    Toast.makeText(AndroidDownloadActivity.this,
//                            "PENDING!",
//                            Toast.LENGTH_LONG).show();
                }else if(status == DownloadManager.STATUS_RUNNING){
//                    Toast.makeText(AndroidDownloadActivity.this,
//                            "RUNNING!",
//                            Toast.LENGTH_LONG).show();
                }
            }
        }

    };


    public void writeFileOnInternalStorage(Context mcoContext, ParcelFileDescriptor descriptor,String filename , String fileExtn){
         File dir = new File(getExternalStorageDirectory(),"mydir");
         if(!dir.exists())
             dir.mkdir();

        System.out.println("SALEM 1");
        File file = new File(dir,filename);
        try {
            System.out.println("SALEM 2");
            InputStream inputStream = new FileInputStream(descriptor.getFileDescriptor());
            System.out.println("SALEM 3");
            OutputStream outputStream = new FileOutputStream(file);
            System.out.println("SALEM 4");
            byte[] buffer = new byte[1024];
            int length;
            while((length = inputStream.read(buffer)) > 0)
            {
                outputStream.write(buffer, 0, length);
            }
            System.out.println("SALEM 5");
            outputStream.flush();
            inputStream.close();
            outputStream.close();
        } catch (Exception e){
            e.printStackTrace();
        }
    }
    @RequiresApi(api = Build.VERSION_CODES.N)
    private void startDownload() {

        String url = urltext.getText().toString();
        String filename = null;
        String fileExtention = MimeTypeMap.getFileExtensionFromUrl(url);
        // if(filename.length() == 0 || filename.length() == 1)
        filename = System.currentTimeMillis() + "";
//        else
//            filename+= "."+fileExtention;


        //create download request
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
        request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI | DownloadManager.Request.NETWORK_MOBILE);
       // request.setTitle("Download");
       // request.setDescription("downloading...");
        request.allowScanningByMediaScanner();
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
      //  request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS); //name of the file is a time stamp

        //get download service and enque file
        DownloadManager manager = (DownloadManager) getActivity().getSystemService(getContext().DOWNLOAD_SERVICE);
        long id = manager.enqueue(request);




    }
    public static String getFileNameFromURL(String url) {
        if (url == null) {
            return "";
        }
        try {
            URL resource = new URL(url);
            String host = resource.getHost();
            if (host.length() > 0 && url.endsWith(host)) {
                // handle ...example.com
                return "";
            }
        }
        catch(MalformedURLException e) {
            return "";
        }

        int startIndex = url.lastIndexOf('/') + 1;
        int length = url.length();

        // find end index for ?
        int lastQMPos = url.lastIndexOf('?');
        if (lastQMPos == -1) {
            lastQMPos = length;
        }

        // find end index for #
        int lastHashPos = url.lastIndexOf('#');
        if (lastHashPos == -1) {
            lastHashPos = length;
        }

        // calculate the end index
        int endIndex = Math.min(lastQMPos, lastHashPos);
        return url.substring(startIndex, endIndex);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode){
            case PERMISSION_STORAGE_CODE:
            if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                //permission granted perfrom download
               // startDownload();
            }else{
                //permission denied show error message
                Toast.makeText(getContext(),"Permission denied..!",Toast.LENGTH_LONG).show();
            }


        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}