/*
 * The MIT License (MIT)
 * Copyright (c) 2016 Kyle Montague
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */

package montague.traces.storage;

/**
 * Created by kyle montague on 11/05/15.
 */


import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;


public class Compress {
    private static final int BUFFER = 1024;

    private String[] _files;
    private String _zipFile;

    public Compress(String[] files, String zipFile) {
        _files = files;
        _zipFile = zipFile;
    }

    public Compress(String parent, String zipFile){
        _zipFile = zipFile;
        List<String> listOfFiles = getListFilePaths(new File(parent));
        _files = listOfFiles.toArray(new String[listOfFiles.size()]);
    }

    public boolean zip() {
        if(_files.length == 0)
            return false;
        try  {
            BufferedInputStream origin = null;
            FileOutputStream dest = new FileOutputStream(_zipFile);
            ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(dest));

            byte data[] = new byte[BUFFER];

            for(int i=0; i < _files.length; i++) {
                FileInputStream fi = new FileInputStream(_files[i]);
                origin = new BufferedInputStream(fi, BUFFER);
                ZipEntry entry = new ZipEntry(_files[i].substring(_files[i].lastIndexOf("/") + 1));
                out.putNextEntry(entry);
                int count;
                while ((count = origin.read(data, 0, BUFFER)) != -1) {
                    out.write(data, 0, count);
                }
                origin.close();
            }

            out.close();
        } catch(Exception e) {
            e.printStackTrace();
        }
        return true;
    }

    private List<File> getListFiles(File parentDir) {
        ArrayList<File> inFiles = new ArrayList<File>();
        File[] files = parentDir.listFiles();
        if(files == null || files.length == 0) {
            Log.d("COMPRESS","NO FILES IN: "+parentDir.getPath());
            return inFiles;
        }
        for (File file : files) {
            if (file.isDirectory()) {
                inFiles.addAll(getListFiles(file));
            } else {
                if(file.getName().endsWith(".csv")){
                    inFiles.add(file);
                }
            }
        }
        return inFiles;
    }

    private ArrayList<String> getListFilePaths(File parentDir) {
        File[] files = parentDir.listFiles();
        if(files == null || files.length == 0) {
            Log.d("COMPRESS","NO FILES IN: "+parentDir.getPath());
            return null;
        }
        parentDir = null;
        ArrayList<String> inFiles = new ArrayList<String>();
        for (File file : files) {
            if (file.isDirectory()) {
                inFiles.addAll(getListFilePaths(file));
            } else {
                if(file.getName().endsWith(".csv")){
                    inFiles.add(file.getAbsolutePath());
                }
            }
        }
        return inFiles;
    }

    public static void DeleteRecursive(File fileOrDirectory) {

        if (fileOrDirectory.isDirectory())
            for (File child : fileOrDirectory.listFiles())
                DeleteRecursive(child);

        fileOrDirectory.delete();

    }


    public static void Delete(String[] files) {
        if(files != null && files.length > 0)
            for(String filename: files) {
                File f = new File(filename);
                if(f.isFile())
                    f.delete();
            }
    }

    public static String ZipRecursive(File Directory) {
        String path="";
        ArrayList<String> filesToZip = new ArrayList<String>();
        if(Directory != null && Directory.listFiles().length > 0)
            for (File child : Directory.listFiles()) {
                if(child.isDirectory())
                    filesToZip.add(Compress.ZipRecursive(child));
                else
                    filesToZip.add(child.getAbsolutePath());
            }
        if(filesToZip.size() > 0) {
            String zip = Directory.getName()+".zip";
            String[] files = (String[])filesToZip.toArray();
            Compress c = new Compress(files,zip);
            if(c.zip()) {
                path = zip;
                Compress.Delete(files);
            }
        }
        return path;
    }

    public static ArrayList<String> ZipRecursive2(File Directory) {
        List<File> dirs = Compress.getListDirs(Directory);
        ArrayList<String> zipFiles = new ArrayList<String>();
        if(dirs.size() > 0){
            Log.d("ZIPPING", "Started Recursive Zip ("+dirs.size()+" dirs)");
            for( File d: dirs){
                String name = d.getName();
                String zipPath = Directory.getPath()+"/"+name+"_all.zip";
                Compress c = new Compress(d.getAbsolutePath(),zipPath);
                Log.d("ZIPPING", "Starting dir: "+name);
                if(c.zip()){
                    zipFiles.add(zipPath);
                }
            }

            if(zipFiles.size()>1){
                String[] files = new String[zipFiles.size()];
                for(int x=0;x<files.length;x++)
                    files[x] = zipFiles.get(x);


                String zipName = Directory.getPath()+"/"+System.currentTimeMillis()+"_COMPLETE.zip";
                Compress c = new Compress(files,zipName);
                try {
                    if (c.zip()) {
                        Log.d("ZIPPING", "BIG ZIP WORKED");
                        ArrayList<String> list = new ArrayList<>();
                        list.add(zipName);
                        return list;
                    }
                }catch(OutOfMemoryError error){
                    Log.d("ZIPPING","Out of Memory error");
                }
            }
            return zipFiles;
        }
        return null;
    }

    private static List<File> getListDirs(File parentDir) {
        ArrayList<File> inDirs = new ArrayList<File>();
        File[] files = parentDir.listFiles();
        for (File file : files) {
            if (file.isDirectory()) {
                inDirs.add(file);
            }
        }
        return inDirs;
    }

    /**
     *
     * GET LIST OF DIRS
     * ZIP EACH DIR
     * ZIP ALL DIRS
     * SEND EACH ZIPPED DIR
     *
     */

}