package io;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;

/**
 * Created by nik on 2/3/17.
 */
public class Utils {
    public static ArrayList<String> readFileLinesDropComments(String path)
    {

        if(path.isEmpty()) {
            System.err.println("Empty path supplied to readFileLinesDropComments ");
            return new ArrayList<>();
        }
        ArrayList<String> res = new ArrayList<>();
        try {
            BufferedReader bf = new BufferedReader(new FileReader(new File(path)));
            String line;
            while((line = bf.readLine()) != null)
            {
                if(line.isEmpty()) continue;
                if(line.startsWith("#")) continue;
                res.add(line.trim());
            }
        } catch (FileNotFoundException e) {
            System.out.println("readFileLinesDropComments: Exception during read of ["+path+"]");
            e.printStackTrace();
            return null;
        } catch (IOException e) {
            System.out.println("readFileLinesDropComments: Exception during read of ["+path+"]");
            e.printStackTrace();
            return null;
        }
        finally {

        }
        return res;
    }

    public static String readFileToString(String filepath) throws IOException {
        return new String(Files.readAllBytes(Paths.get(filepath)));
    }
    public static String toFolderPath(String path)
    {
        if(! path.endsWith(File.separator)) path+=File.separator;
        return path;
    }

}
