package com.example.mediadownloader.ui.home;

import static android.os.Environment.getExternalStorageDirectory;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.fragment.app.Fragment;

import com.example.mediadownloader.databinding.FragmentHomeBinding;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private EditText urltext;
    SharedPreferences preferenceManager;
    String Download_ID = "DOWNLOAD_ID";
    DownloadManager downloadManager;
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {


        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        urltext = binding.url;
        Button downloadbtn = binding.downloadbutton;

        preferenceManager = PreferenceManager.getDefaultSharedPreferences(getContext());
        downloadManager = (DownloadManager) requireActivity().getSystemService(Context.DOWNLOAD_SERVICE);

        downloadbtn.setOnClickListener(v -> {
            Uri Download_Uri = Uri.parse(urltext.getText().toString());
            DownloadManager.Request request = new DownloadManager.Request(Download_Uri);
            long download_id = downloadManager.enqueue(request);

            //Save the download id
            SharedPreferences.Editor PrefEdit = preferenceManager.edit();
            PrefEdit.putLong(Download_ID, download_id);
            PrefEdit.apply();
            requireActivity().registerReceiver(downloadReceiver,new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));

        });


        return root;
    }
    private final BroadcastReceiver downloadReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context arg0, Intent arg1) {
            // TODO Auto-generated method stub
            DownloadManager.Query query = new DownloadManager.Query();
            query.setFilterById(preferenceManager.getLong(Download_ID, 0));
            Cursor cursor = downloadManager.query(query);

            if(cursor.moveToFirst()){
                int columnIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS);
                int status = cursor.getInt(columnIndex);
                int columnfilename = cursor.getColumnIndex(DownloadManager.COLUMN_TITLE);
                String filename = cursor.getString(columnfilename);

                if(status == DownloadManager.STATUS_SUCCESSFUL){
                    //Retrieve the saved download id
                    long downloadID = preferenceManager.getLong(Download_ID, 0);

                    ParcelFileDescriptor file;

                    try {
                        file = downloadManager.openDownloadedFile(downloadID);
                        writeFileOnInternalStorage(file,filename);

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

                }
            }
        }

    };


    public void writeFileOnInternalStorage(ParcelFileDescriptor descriptor, String filename){
         File dir = new File(getExternalStorageDirectory(),"mydir");

       if(!dir.exists())
         dir.mkdir();
       { System.out.println("SALEM 1");
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
    }}
    @RequiresApi(api = Build.VERSION_CODES.N)




    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}