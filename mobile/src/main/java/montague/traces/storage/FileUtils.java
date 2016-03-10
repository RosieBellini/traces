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

import android.os.Environment;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;

/**
 * Created by kylemontague on 06/08/15.
 */
public class FileUtils {

    public static String APP_DIR = Environment.getExternalStorageDirectory().getPath()+"/Traces";

    public static ArrayList<String> listAllSubFolders(){
        File dir = new File(APP_DIR);
        ArrayList<String> folders = new ArrayList<>();
        if(dir.exists()) {
            File[] fList = dir.listFiles();
            for (File file : fList) {
                if (file.isDirectory()) {
                    String[] data = listAllZipFiles(file.getAbsolutePath());
                    if (data != null && data.length > 0)
                        folders.add(file.getAbsolutePath());
                }
            }
        }
        return folders;
    }


    public static String[] listAllZipFiles(){
        File dir = new File(APP_DIR);
        if(!dir.exists())
            return null;

        return dir.list(new ZipFilenameFilter());
    }

    public static String[] listAllZipFiles(String directory){
        File dir = new File(directory);
        if(!dir.exists())
            return null;

        return dir.list(new ZipFilenameFilter());
    }


    public static class ZipFilenameFilter implements FilenameFilter {

        public boolean accept(File dir, String name) {
            if (name == null) {
                return false;
            }
            return name.toLowerCase().endsWith(".zip");
        }
    }


}
