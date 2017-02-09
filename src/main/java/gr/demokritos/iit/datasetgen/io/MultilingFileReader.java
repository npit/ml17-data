package gr.demokritos.iit.datasetgen.io;

import gr.demokritos.iit.datasetgen.io.sentsplit.BasicSentenceSplitter;
import gr.demokritos.iit.datasetgen.io.sentsplit.ISentenceSplitter;
import gr.demokritos.iit.datasetgen.io.sentsplit.OpenNLPSentenceSplitter;
import gr.demokritos.iit.datasetgen.utils.Utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static gr.demokritos.iit.datasetgen.utils.Utils.toFolderPath;

/**
 * Created by nik on 2/3/17.
 */

/**
 * Class to read folders with multiling summaries from humans & models, as well
 * as source articles for Sentence Replacement
 */
public class MultilingFileReader {
    String [] inputFolders;
    String machinesFolder;
    String replFolder;
    Set<String> Languages;
    ISentenceSplitter SentenceSplitter;
    boolean Verbosity;

    public TextfileCollection getTextfileCollection() {
        return TextfileColl;
    }

    TextfileCollection TextfileColl;

    public MultilingFileReader(Properties props)
    {
        Verbosity = Utils.csvStringContains(props.getProperty("modifiers"),"verbose");
        TextfileColl =new TextfileCollection();
        Languages = new HashSet<>();
        String content = props.getProperty("input_texts","");
        inputFolders = content.split(",");

        replFolder = props.getProperty("replacement_texts","");

        String splitMode = props.getProperty("split_mode","");
        if( splitMode.equals("opennlp"))
            SentenceSplitter = new OpenNLPSentenceSplitter(props.getProperty("sentenceSplitter_model_paths"));
        else if (splitMode.equals("basic"))
            SentenceSplitter = new BasicSentenceSplitter(props);
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

        System.out.println("\nParsing input data...");
        for(String inputFolder : inputFolders)
            readFolder(inputFolder,TextfileColl.INPUT);

        System.out.println("\nParsing replacement data...");
        readFolder(replFolder, TextfileColl.REPL);

        System.err.flush();
        System.out.println("\nDone reading.");
    }

    // Read data from a folder. Format expected is
    // basedir/language1/text1,text2,...
    void readFolder(final String rootFolder, final String label)
    {
        ArrayList<Textfile> textfiles = new ArrayList<>();
        if(rootFolder.isEmpty()){
            System.err.println("\tEmpty root folder: [" + rootFolder + "]");
            return;
        }

        // we expect each subfolder to be a language folder
        File rootFileFolder = new File(rootFolder);
        if(!rootFileFolder.exists())
        {
            System.err.println("Failed to open folder: [" + rootFolder+"]");
            return;
        }
        for(final String lang : rootFileFolder.list())
        {

            // parse folder of each language
            String langFolder = toFolderPath(rootFolder + lang);
            if(! new File(langFolder).isDirectory()) {
                System.out.println("\tExpecting language folder - skipping non-directory:" + langFolder);
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
                    if(sentences == null)
                    {
                        System.err.println("\tFailed to read file " + textFilePath);
                        System.err.println("\tAborting language folder read : " + langFolder);
                        System.err.flush();
                        break;
                    }

                    Textfile tf = new Textfile(fileContent, lang);
                    tf.setSentences(sentences);
                    tf.setFilePath(textFilePath);
                    textfiles.add(tf);
                    TextfileColl.AllLanguages.add(lang);
                    if(Verbosity)
                        System.out.println("\tDid read file: " + textFilePath);
                } catch (IOException e) {
                    System.err.printf("Error reading file %s\n",textfile);
                }
            }
        }
        Path p = Paths.get(rootFolder);
        TextfileColl.addInputTextfileList(textfiles,label);
    }

}
