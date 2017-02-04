package io;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import static io.Utils.toFolderPath;

/**
 * Created by nik on 2/3/17.
 */

/**
 * Class to read folders with multiling summaries from humans & models, as well
 * as source articles for Sentence Replacement
 */
public class MultilingFileReader {
    String humansFolder;
    String machinesFolder;
    String replFolder;
    Set<String> Languages;

    public Set<Textfile> getTextfiles() {
        return Textfiles;
    }

    Set<Textfile> Textfiles;

    public MultilingFileReader(Properties props)
    {
        Textfiles = new HashSet<>();
        Languages = new HashSet<>();
        humansFolder = props.getProperty("human_texts","");
        machinesFolder = props.getProperty("machine_texts","");
        replFolder = props.getProperty("replacement_texts","");

    }

    // Read data from all assigned paths
    public void read()
    {
        System.out.println("Parsing humans' data...");
        // for peers, read per peer ID
        for(final String id : new File(humansFolder).list())
        {
            String foldername = toFolderPath(humansFolder+id);
            if(! new File(foldername).isDirectory()) {
                System.out.println("Skipping non-directory:" + foldername);
                continue;
            }
            readFolder(foldername);
        }
        System.out.println("Parsing machines' data...");
        readFolder(machinesFolder);
        System.out.println("Parsing source data...");
        readFolder(replFolder);
    }

    // Read data from a folder. Format expected is
    // basedir/language1/text1,text2,...
    void readFolder(final String rootFolder)
    {
        if(rootFolder.isEmpty()){
            System.out.println("Empty root folder.");
            return;
        }

        // we expect each subfolder to be a language folder
        File rootFileFolder = new File(rootFolder);

        for(final String lang : rootFileFolder.list())
        {
            // parse folder of each language
            String langFolder = toFolderPath(rootFolder + lang);
            if(! new File(langFolder).isDirectory()) {
                System.out.println("Skipping non-directory:" + langFolder);
                continue;
            }
            // add the language to the total
            if(!Languages.contains(lang)) Languages.add(lang);
            // parse each file in the language folder
            File langFolderFile = new File(langFolder);
            for(final String textfile : langFolderFile.list())
            {
                String textFilePath = langFolder + textfile;
                try {
                    // get file text
                    String fileContent = Utils.readFileToString(textFilePath);
                    Textfiles.add(new Textfile(fileContent, lang));
                } catch (IOException e) {
                    System.err.printf("Error reading file %s\n",textfile);
                }
            }

        }

    }

}
