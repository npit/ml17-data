package io;

import io.sentsplit.BasicSentenceSplitter;
import io.sentsplit.ISentenceSplitter;
import io.sentsplit.OpenNLPSentenceSplitter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

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
    ISentenceSplitter SentenceSplitter;

    public TextfileCollection getTextfileCollection() {
        return TextfileColl;
    }

    TextfileCollection TextfileColl;

    public MultilingFileReader(Properties props)
    {
        TextfileColl =new TextfileCollection();
        Languages = new HashSet<>();
        humansFolder = props.getProperty("human_texts","");
        machinesFolder = props.getProperty("machine_texts","");
        replFolder = props.getProperty("replacement_texts","");

        String splitMode = props.getProperty("split_mode","");
        if( splitMode.equals("opennlp"))
            SentenceSplitter = new OpenNLPSentenceSplitter(props.getProperty("sentenceSplitter_model_paths"));
        else if (splitMode.equals("basic"))
            SentenceSplitter = new BasicSentenceSplitter();
        else
        {
            SentenceSplitter = null;
            System.err.println("Undefined split_mode : [" +  splitMode + "]");
        }
    }

    // Read data from all assigned paths
    public void read()
    {
        if(SentenceSplitter == null) return ;

        System.out.println("Parsing humans' data...");
        // for peers, read per peer ID
        for(final String id : new File(humansFolder).list())
        {
            String foldername = toFolderPath(humansFolder+id);
            if(! new File(foldername).isDirectory()) {
                System.out.println("Skipping non-directory:" + foldername);
                continue;
            }
            readFolder(foldername,TextfileColl.INPUT);
        }
        System.out.println("Parsing machines' data...");
        readFolder(machinesFolder,TextfileColl.INPUT);
        System.out.println("Parsing source data...");
        readFolder(replFolder, TextfileColl.REPL);

        System.out.println("Done reading.");
    }

    // Read data from a folder. Format expected is
    // basedir/language1/text1,text2,...
    void readFolder(final String rootFolder, final String label)
    {
        ArrayList<Textfile> textfiles = new ArrayList<>();
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
                    List<String> sentences = SentenceSplitter.splitToSentences(fileContent,lang);

                    Textfile tf = new Textfile(fileContent, lang);
                    tf.setSentences(sentences);
                    textfiles.add(tf);
                    TextfileColl.AllLanguages.add(lang);
                } catch (IOException e) {
                    System.err.printf("Error reading file %s\n",textfile);
                }
            }
        }
        Path p = Paths.get(rootFolder);
        TextfileColl.addInputTextfileList(textfiles,label);
    }

}
