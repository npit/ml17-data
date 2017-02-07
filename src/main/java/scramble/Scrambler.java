package scramble;


import io.Textfile;
import io.TextfileCollection;
import io.Utils;
import io.sentsplit.BasicSentenceSplitter;
import io.sentsplit.ISentenceSplitter;
import io.sentsplit.OpenNLPSentenceSplitter;


import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * Created by nik on 2/3/17.
 */
public class Scrambler {
    Randomizer Rand;
    Properties Props;

    public void setTextfileColl(TextfileCollection textfileColl) {
        TextfileColl = textfileColl;
    }

    TextfileCollection TextfileColl;
    boolean Verbosity;
    public Scrambler(Properties props)
    {
        String modifiers = props.getProperty("modifiers","");
        if(modifiers != null && Arrays.asList(modifiers.split(",")).contains("verbose"))
            Verbosity = true;
        else
            Verbosity = false;

        Rand = new Randomizer(props);
        Props = props;
    }




    public void run()
    {
        // size percentages
        String [] pcnt = Props.getProperty("method_percent").split(",");
        String [] thresh = Props.getProperty("method_decision_prob").split(",");
        ArrayList<Integer> size_percentages = new ArrayList<>();
        ArrayList<Integer> decision_thresholds = new ArrayList<>();
        int runningSum=0;
        for(String s : pcnt)
        {
            size_percentages.add(Integer.parseInt(s));
            runningSum += size_percentages.get(size_percentages.size()-1);
        }
        for(String s : thresh)
        {
            decision_thresholds.add(Integer.parseInt(s));
            if (size_percentages.get(size_percentages.size()-1)>100 || size_percentages.get(size_percentages.size()-1) < 0)
            {
                System.err.println("Gotta provide deision threshold probability between 0 and 100.");
                System.err.println("Provided: " + decision_thresholds);
                return;
            }
        }
        if (runningSum != 100)
        {
            System.err.println("Gotta provide percentages adding up to 100.");
            System.err.println("Provided: " + size_percentages);
            return;
        }

        doSO(size_percentages, decision_thresholds);
        doSR(size_percentages, decision_thresholds);
        doME(size_percentages, decision_thresholds);

    }
    // rearrange sentence order in a file
    public void doSO(ArrayList<Integer> percentages, ArrayList<Integer> thresholds)
    {
        System.out.print("Applying SO...");
        // apply SO
        List<Textfile> inputTexts = TextfileColl.getAllFilesInCategory(TextfileColl.INPUT);

        int num = percentages.get(0) * inputTexts.size() / 100;
        ArrayList<Map<Integer,Integer>> SOSwaps = new ArrayList<>();

        // write folder
        String outputFolder = Props.getProperty("output_folder","output");
        outputFolder = Utils.toFolderPath(outputFolder + "SO");
        File outputFolderFile = new File(outputFolder);

        for(int tfidx=TextfileColl.CurrentProcessedIndex;tfidx<num;++tfidx)
        {
            SOSwaps.add(new HashMap<>());
            Textfile tf = new Textfile(inputTexts.get(tfidx));
            String lang = tf.getLanguage();

            String [] outSentences = new String[tf.getSentences().size()];
            // sentences from which to pick ones
            List<Integer>  sentenceSwapPool = new ArrayList<>();
            for(int j=0;j<tf.getSentences().size();++j) sentenceSwapPool.add(j);

            for(int sidx=0;sidx<tf.getSentences().size();++sidx)
            {
                if(outSentences[sidx] != null) {
                    if(Verbosity)
                        System.out.printf("Already swapped sentence %d/%d \n", 1 + sidx, outSentences.length);
                    continue;
                }

                    int targetIndex = sidx;
                // remove current sentence from the swapables. Since we only forward-swap, it is valid
                // to remove it now rather that in the case of true if-check below
                sentenceSwapPool.remove(sentenceSwapPool.indexOf(sidx));
                // decide if it's going to be reordered
                if(Rand.coinToss(thresholds.get(0)) &&
                        outSentences [sidx] == null &&
                        sentenceSwapPool.size() >0)
                {

                    // pick with which to swap
                    Collections.shuffle(sentenceSwapPool, Rand.getR());
                    targetIndex = sentenceSwapPool.get(0);
                    sentenceSwapPool.remove(sentenceSwapPool.indexOf(targetIndex));


                    SOSwaps.get(SOSwaps.size() -1).put(sidx,targetIndex);
                    SOSwaps.get(SOSwaps.size() -1).put(targetIndex,sidx);

                    outSentences[targetIndex] = tf.getSentences().get(sidx);
                }
                if(Verbosity)
                    System.out.printf("Swapping sentence %d/%d with %d\n",1+sidx,outSentences.length,1+targetIndex);
                outSentences[sidx] = tf.getSentences().get(targetIndex);
            }
            Textfile outTextfile = new Textfile(lang, Arrays.asList(outSentences));
            TextfileColl.addOutputTextfile(outTextfile);

            try {
                String [] outdirFiles = outputFolderFile.list();
                if(outdirFiles == null || !Arrays.asList(outdirFiles).contains(lang)) {
                        Path langFolder = Paths.get(outputFolder + lang);
                        Files.createDirectories(langFolder);


                }
                String fileBaseWritePath = Utils.toFolderPath(outputFolder + lang);
                Utils.write(fileBaseWritePath + tfidx + ".txt",outTextfile.getText());
                Utils.write(fileBaseWritePath + tfidx + ".orig.txt",tf.getText());
                Utils.write(fileBaseWritePath + tfidx + ".assoc",Utils.associationToString(SOSwaps.get(tfidx)));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        TextfileColl.CurrentProcessedIndex += num;
        System.out.println("done.");
    }



    public void doSR(ArrayList<Integer> percentages, ArrayList<Integer> thresholds)
    {
        System.out.print("Applying SR...");
        // apply SO

        List<Textfile> inputTexts = TextfileColl.getAllFilesInCategory(TextfileColl.INPUT);
        int num = percentages.get(2) * inputTexts.size() / 100;
        ArrayList<Map<Integer,Map<Integer,Integer>>> SRSwaps = new ArrayList<>();

        // write folder
        String outputFolder = Props.getProperty("output_folder","output");
        outputFolder = Utils.toFolderPath(outputFolder + "SR");
        File outputFolderFile = new File(outputFolder);

        for(int tfidx=TextfileColl.CurrentProcessedIndex;tfidx<TextfileColl.CurrentProcessedIndex + num;++tfidx)
        {
            SRSwaps.add(new HashMap<>());
            Textfile tf = new Textfile(inputTexts.get(tfidx));
            String lang = tf.getLanguage();

            String [] outSentences = new String[tf.getSentences().size()];
            // sentences from which to pick ones

            for(int sidx=0;sidx<tf.getSentences().size();++sidx)
            {
                if(Verbosity)
                    System.out.printf("Textfile %d/%d sentence %d / %d lang %s\n",tfidx,num,sidx, outSentences.length,lang);
                // decide if it's going to be reordered
                if(Rand.coinToss(thresholds.get(1)) )
                {
                    // pick with which article to swap
                    int replIndex = TextfileColl.getRandomArticle(TextfileColl.REPL,lang,Rand.getR());
                    int sentIndex = TextfileColl.getRandomSentence(TextfileColl.REPL,lang,replIndex,Rand.getR());

                    SRSwaps.get(SRSwaps.size() -1).put(sidx,new HashMap<>());
                    SRSwaps.get(SRSwaps.size() -1).get(sidx).put(replIndex, sentIndex);

                    try {
                        outSentences[sidx] = TextfileColl.getInputFiles().get(TextfileColl.REPL).get(lang).get(replIndex).getSentences().get(sentIndex);
                    }
                    catch (IndexOutOfBoundsException ex)
                    {
                        ex.printStackTrace();
                    }
                    if(Verbosity)
                        System.out.printf("Swapping sentence %d/%d with %d - %d\n",1+sidx,outSentences.length,1+replIndex, 1+sentIndex);
                }
                else
                {
                    SRSwaps.get(SRSwaps.size() -1).put(sidx,new HashMap<>());
                    SRSwaps.get(SRSwaps.size() -1).get(sidx).put(-1,-1);
                    outSentences[sidx] = tf.getSentences().get(sidx);
                }
            }

            Textfile outTextfile = new Textfile(lang, Arrays.asList(outSentences));
            TextfileColl.addOutputTextfile(outTextfile);

            try {
                String [] outdirFiles = outputFolderFile.list();
                if(outdirFiles == null || !Arrays.asList(outdirFiles).contains(lang)) {
                    Path langFolder = Paths.get(outputFolder + lang);
                    Files.createDirectories(langFolder);


                }
                String fileBaseWritePath = Utils.toFolderPath(outputFolder + lang);
                Utils.write(fileBaseWritePath + tfidx + ".txt",outTextfile.getText());
                Utils.write(fileBaseWritePath + tfidx + ".orig.txt",tf.getText());
                Utils.write(fileBaseWritePath + tfidx + ".assoc",Utils.replAssociationToString(SRSwaps.get(tfidx - TextfileColl.CurrentProcessedIndex)));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        TextfileColl.CurrentProcessedIndex += num;
        System.out.println("done.");
    }

    public void doME(ArrayList<Integer> percentages, ArrayList<Integer> thresholds) // heehehehe
    {
        System.out.print("Applying ME...");
        // apply SO
        List<Textfile> inputTexts = TextfileColl.getAllFilesInCategory(TextfileColl.INPUT);

        int num =  TextfileColl.CurrentProcessedIndex + percentages.get(3) * inputTexts.size() / 100;
        if (num>inputTexts.size()) num = inputTexts.size();
        ArrayList<Map<Integer,Integer>> MESwaps = new ArrayList<>();

        // write folder
        String outputFolder = Props.getProperty("output_folder","output");
        outputFolder = Utils.toFolderPath(outputFolder + "ME");
        File outputFolderFile = new File(outputFolder);

        for(int tfidx=TextfileColl.CurrentProcessedIndex;tfidx<num;++tfidx)
        {
            Textfile tf = new Textfile(inputTexts.get(tfidx));
            String lang = tf.getLanguage();
            Textfile outTextfile = null;
            MESwaps.add(new HashMap<>());

            if(Rand.coinToss(thresholds.get(3)))
            {
                // do the merge
                ArrayList<String>  outSentences = new ArrayList<>();
                ArrayList<String>  keptSentences = new ArrayList<>();

                // decide which half to keep
                boolean KeepFirstHalf = Props.getProperty("merge_keep_which_half","first").equals("first");
                if(KeepFirstHalf)
                {
                    for(int s = tf.getSentences().size() / 2; s < tf.getSentences().size(); ++s) keptSentences.add(tf.getSentences().get(s));
                }
                else
                    for(int s=0; s < tf.getSentences().size() / 2;++s) keptSentences.add(tf.getSentences().get(s));

                // select a random article
                int artIdx = TextfileColl.getRandomArticle(TextfileColl.INPUT,lang,Rand.getR());
                Textfile other = TextfileColl.getInputFiles().get(TextfileColl.INPUT).get(lang).get(artIdx);

                if(KeepFirstHalf)
                {
                    for(int s = other.getSentences().size() / 2; s < tf.getSentences().size(); ++s) keptSentences.add(tf.getSentences().get(s));
                }
                else {
                    for (int s = 0; s < other.getSentences().size() / 2; ++s)
                        outSentences.add(tf.getSentences().get(s));
                }
                outSentences.addAll(keptSentences);

                MESwaps.get(MESwaps.size()-1).put(artIdx,KeepFirstHalf ? 1:0);

                if(Verbosity)
                    System.out.printf("Merging %d/%d with %d, half : ",tfidx,num,artIdx, KeepFirstHalf?1:0);
                outTextfile = new Textfile(lang,outSentences);
            }
            else
            {
                outTextfile = tf;
                if(Verbosity)
                    System.out.printf("Will not merge %d/%d \n",tfidx,num);
                MESwaps.get(MESwaps.size()-1).put(-1,-1);

            }
            TextfileColl.addOutputTextfile(outTextfile);

            try {
                String [] outdirFiles = outputFolderFile.list();
                if(outdirFiles == null || !Arrays.asList(outdirFiles).contains(lang)) {
                    Path langFolder = Paths.get(outputFolder + lang);
                    Files.createDirectories(langFolder);


                }
                String fileBaseWritePath = Utils.toFolderPath(outputFolder + lang);
                Utils.write(fileBaseWritePath + tfidx + ".txt",outTextfile.getText());
                Utils.write(fileBaseWritePath + tfidx + ".orig.txt",tf.getText());
                Utils.write(fileBaseWritePath + tfidx + ".assoc",Utils.associationToString(MESwaps.get(MESwaps.size()-1)));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        TextfileColl.CurrentProcessedIndex += num;
        System.out.println("done.");

    }

}
