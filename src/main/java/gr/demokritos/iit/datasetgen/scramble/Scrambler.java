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

    ArrayList<String> CompositeMethods;
    Map<String,Integer> IndexPerCompositeMethod;
    int [] NumRunsForCompositeMethod;
    int NumRunsPerSingleMethod[];
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
        int [] numFilesPerSingleMethod = new int[Options.methods.values().length];
        NumRunsPerSingleMethod = new int[Options.methods.values().length];
        for(int i = 0; i< NumRunsPerSingleMethod.length; ++i) NumRunsPerSingleMethod[i]=0;

        numFilesPerSingleMethod[Options.methods.SR.idx] = (int) Math.ceil((float) (optsSR.Percentage * numInputFiles) / 100.0f);;
        numFilesPerSingleMethod[Options.methods.SO.idx] = (int) Math.ceil((float) (optsSO.Percentage * numInputFiles) / 100.0f);;
        numFilesPerSingleMethod[Options.methods.ME.idx] = (int) Math.ceil((float) (optsME.Percentage * numInputFiles) / 100.0f);;

        String content = Props.getProperty("run_order","SO,SR,ME");
        String runOrder[] = content.split(",");
        if(! Options.checkRunOrder(runOrder))
        {
            System.err.println("Run order configuration error, aborting.");
            return;
        }

        // initialize composite counts
        // --------------------------
        CompositeMethods = new ArrayList<>();
        IndexPerCompositeMethod = new HashMap<>();
        for(int s=0; s<runOrder.length;++s)
        {
            String str = runOrder[s];
            if(str.contains("+")) CompositeMethods.add(str);
            IndexPerCompositeMethod.put(str,s);
        }
        NumRunsForCompositeMethod = new int[CompositeMethods.size()];
        for(int i=0;i<CompositeMethods.size();++i) NumRunsForCompositeMethod[i] = 0;

        // run the methods
        // -----------------
        System.out.println("Will run methods in the order of :" + Arrays.asList(runOrder).toString());
        for(String method : runOrder)
        {
            int numLocalRuns=0;
            // single method
            int numDesired;
            if(!method.contains("+"))
                numDesired = numFilesPerSingleMethod[Options.methods.valueOf(method).idx];
            else
            {
                // for composite, get the # files of the first method in the combo
                String [] parts = method.split("\\+");
                numDesired = numFilesPerSingleMethod[Options.methods.valueOf(parts[0]).idx];
            }

            while(true) {
                idxs = decideIndices(numDesired);
                if(idxs.second()>0 || numLocalRuns > 0)
                    System.out.print(">Running " + method + " requires wrap around. Pass #" + ++numLocalRuns + " : ");
                System.out.println(method + " (" + idxs.first() + " articles)");

                boolean UseNew = true;
                if(!UseNew) {
                    if (method.equals(Options.methods.SO.toString())) {
                        doSO(idxs.first());
                    } else if (method.equals(Options.methods.SR.toString())) {
                        doSR(idxs.first());
                    } else {
                        doME(idxs.first());
                    }
                }
                else {
                    doMethod(method, idxs.first());
                }

                numDesired = idxs.second();
                if(numDesired == 0) break;
            }
            if(!method.contains("+")) {
                // method run done
                NumRunsPerSingleMethod[Options.methods.valueOf(method).idx]++;
            }
            else
            {
                // composite method run done
                NumRunsForCompositeMethod[IndexPerCompositeMethod.get(method)]++;
            }

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
        if(NumRunsPerSingleMethod[Options.methods.SO.idx] > 0)
            outputFolder+=Integer.toString(1+ NumRunsPerSingleMethod[Options.methods.SO.idx]);
        outputFolder = Utils.toFolderPath(outputFolder);

        File outputFolderFile = new File(outputFolder);

        // index to path mapping

        for(int tfidx=CurrentProcessedIndex;tfidx<CurrentProcessedIndex+num;++tfidx)
        {
            swapLog.clear();

            Textfile tf = InputTexts.get(tfidx);
            String lang = tf.getLanguage();
            if(Verbosity)
                System.out.printf("Textfile %d/%d lang %s\n",tfidx,num,lang);
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

    void doMethod(String methodstr, int num)
    {
        String [] methods;
        if(methodstr.contains("+")) methods = methodstr.split("\\+");
        else {
            methods = new String[1];
            methods[0] = methodstr;
        }

        for(int tfidx=CurrentProcessedIndex;tfidx<CurrentProcessedIndex + num;++tfidx)
        {
            Textfile infile = InputTexts.get(tfidx);
            if(Verbosity)
                System.out.printf("Textfile %d/%d lang %s\n",tfidx,num,infile.getLanguage());
            Textfile tempfile = infile;
            methodResult result = new methodResult();
            for(int s=0;s<methods.length;++s)
            {
                String meth = methods[s];
                if(meth.equals(Options.methods.SR.toString())) {
                    methodResult mr = doSR(tempfile);
                    result.merge(mr);
                }
                else if(meth.equals(Options.methods.SO.toString())) {
                    methodResult mr = doSO(tempfile);
                    result.merge(mr);
                }
                else if(meth.equals(Options.methods.ME.toString())) {
                    methodResult mr = doME(tempfile);
                    result.merge(mr);
                }
                else
                    return;
                if(s < methods.length)
                    tempfile = result.getLast();
            }
            // done processing the file.
            write(infile,result.getLast(),tfidx,methodstr,Utils.listToString(result.log));
        }
        CurrentProcessedIndex += num;
    }
    private class methodResult
    {
        public methodResult(Textfile tf, String log)
        {
            this.log = new ArrayList<>();
            this.tf = new ArrayList<>();
            this.log.add(log);
            this.tf.add(tf);
        }
        Textfile getLast()
        {
            return tf.get(tf.size()-1);
        }
        public methodResult()
        {
            this.log = new ArrayList<>();
            this.tf = new ArrayList<>();
        }
        void merge(methodResult other)
        {
            log.addAll(other.log);
            tf.addAll(other.tf);
        }
        ArrayList<String> log;
        ArrayList<Textfile> tf;
    }

    public methodResult doSO(Textfile tf)
    {
        String lang = tf.getLanguage();
        // log
        ArrayList<Pair<Integer,Integer>> swapLog = new ArrayList<>();
        int numSentences = tf.getSentences().size();
        for(int k=0;k<numSentences;++k) swapLog.add(new Pair<>());
        // result sentences
        String [] outSentences = new String[numSentences];

        // sentences from which to pick ones
        List<Integer>  sentenceSwapPool = new ArrayList<>();
        for(int j=0;j<numSentences;++j) sentenceSwapPool.add(j);
        // for each sentence
        for(int sidx=0;sidx<tf.getSentences().size();++sidx)
        {
            if(Verbosity)
                System.out.printf("\tSentence %d / %d  : ",sidx, outSentences.length);
            // if sentence has been already swapped with a previous one, skip
            if(outSentences[sidx] != null) {
                if(Verbosity)
                    System.out.printf("already swapped\n");
                continue;
            }

            int targetIndex;
            // remove current sentence from the swapables. Since we only forward-swap, it is valid
            // to remove it now rather that in the case of true if-check below
            sentenceSwapPool.remove(sentenceSwapPool.indexOf(sidx));
            // decide if it's going to be swapped, based on probability and if there are sentences left
            if(Rand.coinToss(optsSO.DecisionProb) && sentenceSwapPool.size() >0)
            {
                // pick with which to swap: pick a position in the pool
                int randIndex = Rand.getInt(sentenceSwapPool.size());
                targetIndex = sentenceSwapPool.get(randIndex); // sentence chosen
                sentenceSwapPool.remove(randIndex);
                // log and do the reverse swap (it's a symmetrical relation process)
                swapLog.get(targetIndex).set(1+targetIndex,1+sidx);
                outSentences[targetIndex] = tf.getSentences().get(sidx);
            }
            else
                // did not swap. Log the default sentence mapping to itself
                targetIndex = sidx;
            if(Verbosity) System.out.printf("swapping with %d\n",1+targetIndex);

            // log and do the swap
            swapLog.get(sidx).set(1+sidx,1+targetIndex);
            outSentences[sidx] = tf.getSentences().get(targetIndex);
        } // for all sentences

        Textfile outTextfile = new Textfile(lang, Arrays.asList(outSentences));
        methodResult result = new methodResult(outTextfile, Utils.listToString(swapLog));
        return result;
    }
    public methodResult doME(Textfile tf)
    {
        Pair<String,String> swapLog = new Pair<>();
        String lang = tf.getLanguage();
        Textfile outTextfile = null;
        // random decision
        if(Rand.coinToss(optsME.DecisionProb))
        {
            // do the merge
            ArrayList<String>  outSentences = new ArrayList<>();
            ArrayList<String>  keptSentences = new ArrayList<>();

            // decide which half to keep
            boolean KeepFirstHalf = optsME.hasMisc("keep_first");
            if( ! KeepFirstHalf)    // keep second half
            {
                for(int s = tf.getSentences().size() / 2; s < tf.getSentences().size(); ++s)
                    keptSentences.add(tf.getSentences().get(s));
            }
            else    // keep first half
                for(int s=0; s < tf.getSentences().size() / 2;++s) keptSentences.add(tf.getSentences().get(s));

            // select a random article
            int artIdx = TextfileColl.getRandomArticle(TextfileColl.INPUT,lang,Rand.getR(),optsME.hasMisc(Options.modifiers.REUSE_A));
            Textfile other = TextfileColl.getInputFiles().get(TextfileColl.INPUT).get(lang).get(artIdx);

            if(KeepFirstHalf) // take second half from other article
            {
                for(int s = other.getSentences().size() / 2; s < other.getSentences().size(); ++s) keptSentences.add(other.getSentences().get(s));
            }
            else // take first half from other article
            {
                for (int s = 0; s < other.getSentences().size() / 2; ++s)
                    outSentences.add(other.getSentences().get(s));
            }
            // merge sentences in correct order
            outSentences.addAll(keptSentences);
            // log the merge
            swapLog.set(other.getFilePath(),KeepFirstHalf ? "second" : "first");
            if(Verbosity) System.out.printf("Merging  %d, half : %s\n",artIdx, KeepFirstHalf ? "second" : "first");
            // assign the new textfile
            outTextfile = new Textfile(lang,outSentences);
        }
        else
        {
            // did not merge. Assign the original textfile to the output one
            outTextfile = tf;
            if(Verbosity)
                System.out.printf("Will not merge \n");
            swapLog.set("none","no-merge");
        }
        methodResult result = new methodResult(outTextfile,swapLog.toString());
        return result;
    }
    public methodResult doSR(Textfile tf)
    {
        ArrayList<Pair<String,Integer>> swapLog = new ArrayList<>();

        String lang = tf.getLanguage();

        String [] outSentences = new String[tf.getSentences().size()];
        int count = 0;
        for(int sidx=0;sidx<tf.getSentences().size();++sidx) {
            ++count;

            if (Verbosity)
                System.out.printf("\tSentence %d/%d ", 1 + sidx, outSentences.length);

            // decide if the sentence is to be replaced
            if ( Rand.coinToss(optsSR.DecisionProb))
            {
                // pick with which article to swap
                int replIndex = TextfileColl.getRandomArticle(TextfileColl.REPL, lang, Rand.getR(), optsSR.hasMisc(Options.modifiers.REUSE_A));
                int sentIndex = TextfileColl.getRandomSentence(TextfileColl.REPL, lang, replIndex, Rand.getR(), optsSR.hasMisc(Options.modifiers.REUSE_S));
                if (replIndex < 0 || sentIndex < 0) {
                    System.err.printf("No (more?) replacement resources, stopping at sentence %d.\n", count);
                    System.err.println("articleInfo : " + tf.toString());

                    outSentences[sidx] = tf.getSentences().get(sidx);
                    break;
                }
                // do the swap
                try {
                    Textfile other = TextfileColl.getInputFiles().get(TextfileColl.REPL).get(lang).get(replIndex);
                    outSentences[sidx] = other.getSentences().get(sentIndex);
                    swapLog.add(new Pair<>(other.getFilePath(), 1 + sentIndex));
                } catch (IndexOutOfBoundsException ex) {
                    ex.printStackTrace();
                }
                if (Verbosity)
                    System.out.printf("Swapping with %d - %d\n", 1 + replIndex, 1 + sentIndex);
            } else {
                // no swap, prob check failed
                if (Verbosity) System.out.println("No swap.");
                swapLog.add(new Pair<>("none", -1));
                outSentences[sidx] = tf.getSentences().get(sidx);
            }
        }
        Textfile outTextFile = new Textfile(lang,Arrays.asList(outSentences));
        outTextFile.setFilePath(tf.getFilePath());

        methodResult result = new methodResult(outTextFile,Utils.listToString(swapLog));
        return result;
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
        if(NumRunsPerSingleMethod[Options.methods.SO.idx] > 0)
            outputFolder+=Integer.toString(1+ NumRunsPerSingleMethod[Options.methods.SR.idx]);
        outputFolder = Utils.toFolderPath(outputFolder);
        File outputFolderFile = new File(outputFolder);



        boolean noMoreSentences = false;
        for(int tfidx=CurrentProcessedIndex;tfidx<CurrentProcessedIndex + num;++tfidx)
        {
            swapLog.clear();
            Textfile tf = InputTexts.get(tfidx);
            String lang = tf.getLanguage();
            if(Verbosity)
                System.out.printf("Textfile %d/%d lang %s\n",tfidx,num,lang);
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


    // write a file to its destination
    void write(Textfile inputTextfile, Textfile outTextfile, int tfidx, String methodName, String swapLog)
    {
        String lang = outTextfile.getLanguage();
        // specify output destination
        String outputFolder = Props.getProperty("output_folder", "output") + methodName;

        if(methodName.contains("+"))
        {
            if(NumRunsForCompositeMethod[IndexPerCompositeMethod.get(methodName)] > 0)
                outputFolder += Integer.toString(1 + NumRunsForCompositeMethod[IndexPerCompositeMethod.get(methodName)]);
        }
        else {
            Options.methods meth = Options.methods.valueOf(methodName);
            // fix the output folder for multiple method runs
            if (NumRunsPerSingleMethod[meth.idx] > 0)
                outputFolder += Integer.toString(1 + NumRunsPerSingleMethod[meth.idx]);
        }
         // pathify and make the file
        outputFolder = Utils.toFolderPath(outputFolder);

        try {
            createLanguageFolder(outputFolder,lang);
            // write the file
            String fileBaseWritePath = Utils.toFolderPath(outputFolder + lang);
            Utils.write(fileBaseWritePath + tfidx + ".txt",outTextfile.getText());
            if(WriteDebugFiles) {
                Utils.write(fileBaseWritePath + tfidx + ".orig.txt", inputTextfile.getText());
                Utils.write(fileBaseWritePath + tfidx + ".assoc", swapLog);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    // create language folder
    void createLanguageFolder(String outputFolder, String lang) throws IOException {
        File outputFolderFile = new File(outputFolder);
        String [] outdirFiles = outputFolderFile.list();
        // create the language folder if not already there
        if(outdirFiles == null || !Arrays.asList(outdirFiles).contains(lang)) {
            Path langFolder = Paths.get(outputFolder + lang);
            Files.createDirectories(langFolder);
        }
    }


    // huehue
    public void doME(int num)
    {

        Pair<String,String> swapLog = new Pair<>();

        // write folder
        String outputFolder = Props.getProperty("output_folder","output");
        outputFolder+="ME";
        if(NumRunsPerSingleMethod[Options.methods.ME.idx] > 0)
            outputFolder+=Integer.toString(1+ NumRunsPerSingleMethod[Options.methods.ME.idx]);
        outputFolder = Utils.toFolderPath(outputFolder);
        File outputFolderFile = new File(outputFolder);

        for(int tfidx=CurrentProcessedIndex;tfidx<CurrentProcessedIndex+num;++tfidx)
        {
            Textfile tf = InputTexts.get(tfidx);

            String lang = tf.getLanguage();
            Textfile outTextfile = null;
            if(Verbosity)
                System.out.printf("Textfile %d/%d lang %s ",tfidx,num,lang);
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
