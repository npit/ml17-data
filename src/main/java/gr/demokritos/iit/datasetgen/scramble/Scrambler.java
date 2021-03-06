package gr.demokritos.iit.datasetgen.scramble;


import gr.demokritos.iit.datasetgen.io.Textfile;
import gr.demokritos.iit.datasetgen.io.TextfileCollection;
import gr.demokritos.iit.datasetgen.utils.Pair;
import gr.demokritos.iit.datasetgen.utils.Utils;


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



    boolean WriteDebugFiles;
    Options optsSR;
    Options optsSO;
    Options optsME;
    List<Textfile> InputTexts;
    Randomizer Rand;
    Properties Props;
    static final String [] MethodNames = {"SO","SR","ME"};
    int CurrentProcessedIndex;
    public void setTextfileColl(TextfileCollection textfileColl) {
        TextfileColl = textfileColl;

    }

    int NumRunsPerMethod[];
    TextfileCollection TextfileColl;
    boolean Verbosity;
    public Scrambler(Properties props)
    {
        String modifiers = props.getProperty("modifiers","");
        Verbosity = Utils.csvStringContains(modifiers,"verbose");
        WriteDebugFiles = Utils.csvStringContains(modifiers,"write_debug");

        Rand = new Randomizer(props);
        Props = props;
        CurrentProcessedIndex = 0;
    }




    public void run()
    {

        // read options per method
        optsSR = new Options(Props,Options.methods.SR);
        optsSO = new Options(Props,Options.methods.SO);
        optsME = new Options(Props,Options.methods.ME);
        System.out.println();
        optsSO.tell(); System.out.println();
        optsSR.tell(); System.out.println();
        optsME.tell(); System.out.println();

        // get shuffled input file indices
        InputTexts = TextfileColl.getAllFilesInCategory(TextfileColl.INPUT);
        System.out.println("Will process " + InputTexts.size() + " input files.");
        int numInputFiles = InputTexts.size();
        Collections.shuffle(InputTexts,Rand.getR());

        if(WriteDebugFiles) {
            // write index to path associations
            Map<Integer, String> IndexToPath = new HashMap<>();
            for (int t = 0; t < InputTexts.size(); ++t)
                IndexToPath.put(t, InputTexts.get(t).getFilePath());

            String outputFolder = Props.getProperty("output_folder", "output");
            try {
                Utils.writeMap(outputFolder + "indexMapping.txt", IndexToPath);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        // run each method cyclically
        Pair<Integer,Integer> idxs;
        int [] numFilesPerMethod = new int[Options.methods.values().length];
        NumRunsPerMethod = new int[Options.methods.values().length];
        for(int i=0;i<NumRunsPerMethod.length;++i) NumRunsPerMethod[i]=0;

        numFilesPerMethod[Options.methods.SR.idx] = (int) Math.ceil((float) (optsSR.Percentage * numInputFiles) / 100.0f);;
        numFilesPerMethod[Options.methods.SO.idx] = (int) Math.ceil((float) (optsSO.Percentage * numInputFiles) / 100.0f);;
        numFilesPerMethod[Options.methods.ME.idx] = (int) Math.ceil((float) (optsME.Percentage * numInputFiles) / 100.0f);;


        String content = Props.getProperty("run_order","SO,SR,ME");
        String runOrder[] = content.split(",");
        if(! Options.checkRunOrder(runOrder))
        {
            System.err.println("Run order configuration error, aborting.");
            return;
        }
        System.out.println("Will run methods in the order of :" + Arrays.asList(runOrder).toString());
        for(String method : runOrder)
        {
            int numLocalRuns=0;
            int numDesired = numFilesPerMethod[Options.methods.valueOf(method).idx];

            while(true) {
                idxs = decideIndices(numDesired);
                if(idxs.second()>0 || numLocalRuns > 0)
                    System.out.print(">Running " + method + " requires wrap around. Pass #" + ++numLocalRuns + " : ");
                System.out.println(method + " (" + idxs.first() + " articles)");
                if (method.equals(Options.methods.SO.toString())) {
                    doSO(idxs.first());
                } else if (method.equals(Options.methods.SR.toString())) {
                    doSR(idxs.first());
                } else {
                    doME(idxs.first());
                }
                numDesired = idxs.second();
                if(numDesired == 0) break;
            }
            // method run done
            NumRunsPerMethod[Options.methods.valueOf(method).idx]++;

        }

//        int numDesired = numSO;
//        while(true) {
//            idxs = decideIndices(numDesired);
//            doSO(idxs.first());
//            numDesired = idxs.second();
//            if(numDesired == 0) break;
//        }
//        numDesired = numSR;
//        while(true) {
//            idxs = decideIndices(numDesired);
//            doSR(idxs.first());
//            numDesired = idxs.second();
//            if(numDesired == 0) break;
//        }
//        numDesired = numME;
//        while(true) {
//            idxs = decideIndices(numDesired);
//            doME(idxs.first());
//            numDesired = idxs.second();
//            if(numDesired == 0) break;
//        }


    }

    Pair<Integer,Integer> decideIndices(int amountDesired)
    {
        // if the number of files desired exceeds the total, do all remaining, reshuffle and
        // start over
        // first value of the pair
        int numToProcessNextRun = amountDesired;
        int numAvailableArticlesLeft = 0;
        int numArticlesTotal = TextfileColl.getAllFilesInCategory(TextfileColl.INPUT).size();
        int amountLeft = numArticlesTotal - CurrentProcessedIndex;
        // if exactly zero left, just start over
        if( amountLeft == 0)
        {
            amountLeft = numArticlesTotal;
            CurrentProcessedIndex = 0;
        }

        if( amountLeft < amountDesired)
        {
            numToProcessNextRun = amountLeft;
            numAvailableArticlesLeft = amountDesired - amountLeft;
        }
        return new Pair<>(numToProcessNextRun,numAvailableArticlesLeft);

    }
    // rearrange sentence order in a file
    public void doSO(int num)
    {

        ArrayList<Pair<Integer,Integer>> swapLog = new ArrayList<>();

        // write folder
        String outputFolder = Props.getProperty("output_folder","output");
        outputFolder += "SO";
        if(NumRunsPerMethod[Options.methods.SO.idx] > 0)
            outputFolder+=Integer.toString(1+NumRunsPerMethod[Options.methods.SO.idx]);
        outputFolder = Utils.toFolderPath(outputFolder);

        File outputFolderFile = new File(outputFolder);

        // index to path mapping

        for(int tfidx=CurrentProcessedIndex;tfidx<CurrentProcessedIndex+num;++tfidx)
        {
            swapLog.clear();

            Textfile tf = InputTexts.get(tfidx);
            String lang = tf.getLanguage();
            if(Verbosity)
                System.out.printf("Textfile %d (%d/%d) lang %s\n",tfidx,tfidx - CurrentProcessedIndex, num,lang);
            int numSentences = tf.getSentences().size();
            for(int k=0;k<numSentences;++k) swapLog.add(new Pair<>());
            String [] outSentences = new String[numSentences ];
            // sentences from which to pick ones
            List<Integer>  sentenceSwapPool = new ArrayList<>();
            for(int j=0;j<tf.getSentences().size();++j) sentenceSwapPool.add(j);

            for(int sidx=0;sidx<tf.getSentences().size();++sidx)
            {
                if(Verbosity)
                    System.out.printf("\tSentence %d / %d  : ",sidx, outSentences.length);
                if(outSentences[sidx] != null) {
                    if(Verbosity)
                        System.out.printf("already swapped\n");
                    continue;
                }

                    int targetIndex = sidx;
                // remove current sentence from the swapables. Since we only forward-swap, it is valid
                // to remove it now rather that in the case of true if-check below
                sentenceSwapPool.remove(sentenceSwapPool.indexOf(sidx));
                // decide if it's going to be reordered
                if(Rand.coinToss(optsSO.DecisionProb) &&
                        outSentences [sidx] == null &&
                        sentenceSwapPool.size() >0)
                {

                    // pick with which to swap. Pick a position in the pool
                    int randIndex = Rand.getInt(sentenceSwapPool.size());
                    targetIndex = sentenceSwapPool.get(randIndex); // sentence chosen
                    sentenceSwapPool.remove(randIndex);

                    swapLog.get(sidx).set(1+sidx,1+targetIndex);
                    swapLog.get(targetIndex).set(1+targetIndex,1+sidx);

                    outSentences[targetIndex] = tf.getSentences().get(sidx);
                }
                else
                {
                    // did not swap.
                    swapLog.get(sidx).set(1+sidx,1+sidx);
                }
                if(Verbosity)
                    System.out.printf("swapping with %d\n",1+targetIndex);
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
                if(WriteDebugFiles) {
                    Utils.write(fileBaseWritePath + tfidx + ".orig.txt", tf.getText());
                    Utils.write(fileBaseWritePath + tfidx + ".assoc", Utils.listToString(swapLog));
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        CurrentProcessedIndex += num;

    }



    public void doSR(int num)
    {
        // apply SR

        // check replacement articles exist
        if(TextfileColl.getAllFilesInCategory(TextfileColl.REPL).isEmpty())
        {
            System.err.println("\nNo files in replacement category, cannot run SR.");
            System.err.flush();
            return;
        }

        ArrayList<Pair<String,Integer>> swapLog = new ArrayList<>();

        // write folder
        String outputFolder = Props.getProperty("output_folder","output");
        outputFolder+="SR";
        if(NumRunsPerMethod[Options.methods.SO.idx] > 0)
            outputFolder+=Integer.toString(1+NumRunsPerMethod[Options.methods.SR.idx]);
        outputFolder = Utils.toFolderPath(outputFolder);
        File outputFolderFile = new File(outputFolder);



        boolean noMoreSentences = false;
        for(int tfidx=CurrentProcessedIndex;tfidx<CurrentProcessedIndex + num;++tfidx)
        {
            swapLog.clear();
            Textfile tf = InputTexts.get(tfidx);
            String lang = tf.getLanguage();
            if(Verbosity)
                System.out.printf("Textfile %d (%d/%d) lang %s\n",tfidx, tfidx - CurrentProcessedIndex,num,lang);
            String [] outSentences = new String[tf.getSentences().size()];
            int count = 0;
            for(int sidx=0;sidx<tf.getSentences().size();++sidx)
            {
                ++count;

                if(Verbosity)
                    System.out.printf("\tSentence %d/%d ",1+sidx,outSentences.length);

                // decide if the sentence is to be replaced
                if(noMoreSentences == false && Rand.coinToss(optsSR.DecisionProb) )
                {
                    // pick with which article to swap
                    int replIndex = TextfileColl.getRandomArticle(TextfileColl.REPL,lang,Rand.getR(),optsSR.hasMisc(Options.modifiers.REUSE_A));
                    int sentIndex = TextfileColl.getRandomSentence(TextfileColl.REPL,lang,replIndex,Rand.getR(),optsSR.hasMisc(Options.modifiers.REUSE_S));
                    if(replIndex < 0 || sentIndex < 0)
                    {
                        System.err.printf("No (more?) replacement resources. Stopping at article %d/%d (%d) \n",count,num,tfidx);
                        System.err.println("articleInfo : " + tf.toString());

                        noMoreSentences = true;
                        //swapLog.add(new Pair<>("none",-1));
                        outSentences[sidx] = tf.getSentences().get(sidx);
                        break;
                    }
                    // do the swap
                    try {
                        Textfile other = TextfileColl.getInputFiles().get(TextfileColl.REPL).get(lang).get(replIndex);
                        outSentences[sidx] = other.getSentences().get(sentIndex);
                        swapLog.add(new Pair<>(other.getFilePath(),1+sentIndex));
                    }
                    catch (IndexOutOfBoundsException ex)
                    {
                        ex.printStackTrace();
                    }
                    if(Verbosity)
                        System.out.printf("Swapping with %d - %d\n",1+replIndex, 1+sentIndex);
                }
                else
                {
                    // no swap, prob check failed
                    if(Verbosity) System.out.println("No swap.");
                    swapLog.add(new Pair<>("none",-1));
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
                if(WriteDebugFiles) {
                    Utils.write(fileBaseWritePath + tfidx + ".orig.txt", tf.getText());
                    Utils.write(fileBaseWritePath + tfidx + ".assoc", Utils.listToString(swapLog));
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        CurrentProcessedIndex += num;


    }

    // huehue
    public void doME(int num)
    {

        Pair<String,String> swapLog = new Pair<>();

        // write folder
        String outputFolder = Props.getProperty("output_folder","output");
        outputFolder+="ME";
        if(NumRunsPerMethod[Options.methods.ME.idx] > 0)
            outputFolder+=Integer.toString(1+NumRunsPerMethod[Options.methods.ME.idx]);
        outputFolder = Utils.toFolderPath(outputFolder);
        File outputFolderFile = new File(outputFolder);

        for(int tfidx=CurrentProcessedIndex;tfidx<CurrentProcessedIndex+num;++tfidx)
        {
            Textfile tf = InputTexts.get(tfidx);

            String lang = tf.getLanguage();
            Textfile outTextfile = null;
            if(Verbosity)
                System.out.printf("Textfile %d (%d/%d) lang %s ",tfidx,tfidx - CurrentProcessedIndex,num,lang);
            if(Rand.coinToss(optsME.DecisionProb))
            {
                // do the merge
                ArrayList<String>  outSentences = new ArrayList<>();
                ArrayList<String>  keptSentences = new ArrayList<>();

                // decide which half to keep
                boolean KeepFirstHalf = optsME.hasMisc("keep_first");
                if( ! KeepFirstHalf)
                {
                    for(int s = tf.getSentences().size() / 2; s < tf.getSentences().size(); ++s) keptSentences.add(tf.getSentences().get(s));
                }
                else
                    for(int s=0; s < tf.getSentences().size() / 2;++s) keptSentences.add(tf.getSentences().get(s));

                // select a random article
                int artIdx = TextfileColl.getRandomArticle(TextfileColl.INPUT,lang,Rand.getR(),optsME.hasMisc(Options.modifiers.REUSE_A));
                Textfile other = TextfileColl.getInputFiles().get(TextfileColl.INPUT).get(lang).get(artIdx);

                if(KeepFirstHalf)
                {
                    // take second half from other article
                    for(int s = other.getSentences().size() / 2; s < other.getSentences().size(); ++s) keptSentences.add(other.getSentences().get(s));
                }
                else {
                    for (int s = 0; s < other.getSentences().size() / 2; ++s)
                        outSentences.add(other.getSentences().get(s));
                }
                outSentences.addAll(keptSentences);

                swapLog.set(other.getFilePath(),KeepFirstHalf ? "second" : "first");

                if(Verbosity)
                    System.out.printf("Merging  %d, half : %s\n",artIdx, KeepFirstHalf ? "second" : "first");
                outTextfile = new Textfile(lang,outSentences);
            }
            else
            {
                outTextfile = tf;
                if(Verbosity)
                    System.out.printf("Will not merge %d/%d \n",tfidx,num);
                swapLog.set("none","no-merge");

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
                if(WriteDebugFiles) {
                    Utils.write(fileBaseWritePath + tfidx + ".orig.txt", tf.getText());
                    Utils.write(fileBaseWritePath + tfidx + ".assoc", swapLog.toString());
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        CurrentProcessedIndex += num;


    }

}
