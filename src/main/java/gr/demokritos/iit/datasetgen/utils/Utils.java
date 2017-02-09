package gr.demokritos.iit.datasetgen.utils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

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
    public static void write(String file, String text) throws IOException {
        BufferedWriter bw = new BufferedWriter(new FileWriter(file));
        bw.write(text);
        bw.close();
    }
    public static void writeMap(String filepath, Map<?,?> map) throws IOException {

        BufferedWriter bw = new BufferedWriter(new FileWriter(filepath));

        for(Object key : map.keySet())
        {
            bw.write(key.toString() + "\t" + map.get(key).toString() + "\n");
        }
        bw.close();
    }
    public static String replAssociationToString(Map<Integer,Map<Integer,Integer>> map)
    {
        String out = "";
        SortedSet<Integer> keys = new TreeSet<Integer>(map.keySet());
        for(Integer key : keys)
        {
            for(Integer rkey : map.get(key).keySet()) {
                out += key.toString() + " " + rkey + " " + map.get(key).get(rkey).toString() + "\n";
            }
        }
        return out;
    }

    public static String listToString(ArrayList<?> list)
    {
        String res = "";
        for(Object o : list)
            res +=(o.toString() + "\n");
        return res;
    }
    public static boolean csvStringContains(String csvstr, String what)
    {
        if(csvstr == null) return false;
        if(csvstr.isEmpty()) return false;
        String [] arr = csvstr.split(",");
        for(int i=0;i<arr.length;++i)
        {
            String ss = arr[i].trim();
            arr[i] = ss;
        }
        return Arrays.asList(arr).contains(what);
    }

}
