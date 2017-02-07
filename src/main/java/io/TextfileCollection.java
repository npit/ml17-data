package io;

import javax.xml.soap.Text;
import java.util.*;

/**
 * Created by nik on 2/7/17.
 */
public class TextfileCollection {

    public TextfileCollection()
    {
        InputFiles = new HashMap<>();
        IdxNameToLangToFilesToSentences = new HashMap<>();
        IdxNameToLangToFiles = new HashMap<>();
        OutputTextfileLists = new ArrayList<>();
        AllLanguages = new HashSet<>();
    }
    public ArrayList<Textfile> getAllFilesInCategory( String cat)
    {
        ArrayList<Textfile> res = new ArrayList<>();
        for(String lang : InputFiles.get(cat).keySet())
        {
            res.addAll(InputFiles.get(cat).get(lang));
        }
        return res;
    }
    // text file roles
    public static final String INPUT="input";
    public static final String REPL="repl";
    public int CurrentProcessedIndex=0;

    Set<String> AllLanguages;
    public Set<String> getAllLanguages() {
        return AllLanguages;
    }

    public Map<String, Map<String, ArrayList<Textfile>>> getInputFiles() {
        return InputFiles;
    }

    Map<String,Map<String,ArrayList<Textfile>>> InputFiles;


    List<Textfile> OutputTextfileLists;

    public void addInputTextfileList(List<Textfile> tflist, String name)
    {
        if(!InputFiles.containsKey(name)) InputFiles.put(name,new HashMap<>());
        for(Textfile tf : tflist)
        {
            String lang = tf.getLanguage();
            if( ! InputFiles.get(name).containsKey(lang)) InputFiles.get(name).put(lang,new ArrayList<>());
            InputFiles.get(name).get(lang).add(tf);
        }

        // populate index maps
        // put name
        if(!IdxNameToLangToFiles.containsKey(name)) IdxNameToLangToFiles.put(name,new HashMap<>());
        if(!IdxNameToLangToFilesToSentences.containsKey(name)) IdxNameToLangToFilesToSentences.put(name,new HashMap<>());
        for(Textfile tf : tflist) {
            String lang = tf.getLanguage();
            if (!IdxNameToLangToFiles.get(name).containsKey(lang))
                IdxNameToLangToFiles.get(name).put(lang, new ArrayList<>()); // put lang
            int idx = IdxNameToLangToFiles.get(name).get(lang).size();
            IdxNameToLangToFiles.get(name).get(lang).add(
                    idx
            );

            if (!IdxNameToLangToFilesToSentences.get(name).containsKey(lang))
                IdxNameToLangToFilesToSentences.get(name).put(lang, new HashMap<>()); // put lang
            IdxNameToLangToFilesToSentences.get(name).get(lang).put(idx,new ArrayList<>());
            for (int s = 0 ; s < tf.getSentences().size(); ++s)
                IdxNameToLangToFilesToSentences.get(name).get(lang).get(idx).add(s);
        }
    }
    public void addOutputTextfile(Textfile tf)
    {
        OutputTextfileLists.add(tf);
    }



    public Map<String, Map<String, Map<Integer, List<Integer>>>> getGlobalIndex() {
        return IdxNameToLangToFilesToSentences;
    }

    // index lists for randomized selections
    // type - lang - fileidxs
    Map<String,Map<String,ArrayList<Integer>>> IdxNameToLangToFiles;
    Map<String,Map<String,Map<Integer,List<Integer>>>> IdxNameToLangToFilesToSentences;

    public Map<String, Map<String, ArrayList<Integer>>> getIdxNameToLangToFiles() {
        return IdxNameToLangToFiles;
    }

    public Map<String, Map<String, Map<Integer, List<Integer>>>> getIdxNameToLangToFilesToSentences() {
        return IdxNameToLangToFilesToSentences;
    }

    public int getRandomSentence(String name, String lang, int artidx, Random R)
    {
        int num=0;
        int idx=0;

        num = R.nextInt(IdxNameToLangToFilesToSentences.get(name).get(lang).get(artidx).size());
        idx = IdxNameToLangToFilesToSentences.get(name).get(lang).get(artidx).get(num);

//        System.out.printf("Chosen position %d/%d, sentence idx %d\n",num,IdxNameToLangToFilesToSentences.get(name).get(lang).get(artidx).size(),idx);
        IdxNameToLangToFilesToSentences.get(name).get(lang).get(artidx).remove(num);

        if(IdxNameToLangToFilesToSentences.get(name).get(lang).get(artidx).isEmpty())
        {
            IdxNameToLangToFilesToSentences.get(name).get(lang).remove(new Integer(artidx));
            IdxNameToLangToFiles.get(name).get(lang).remove(new Integer(artidx));
        }

        return idx;
    }

    public int getRandomArticle(String name, String lang, Random R)
    {
        if(IdxNameToLangToFiles.get(name).get(lang).isEmpty())
        {
            buildIndex(name,lang);
        }
        int indexOfArrlist =  R.nextInt(IdxNameToLangToFiles.get(name).get(lang).size());
        int articleIndex = IdxNameToLangToFiles.get(name).get(lang).get(indexOfArrlist);
//        System.out.printf("Chosen position %d/%d, article idx %d\n",indexOfArrlist,IdxNameToLangToFiles.get(name).get(lang).size(),articleIndex);
        if(IdxNameToLangToFiles.get(name).get(lang).isEmpty())
            IdxNameToLangToFiles.get(name).remove(lang);
        return articleIndex;
    }
    private void buildIndex(String name, String lang)
    {
//        System.out.println("Building " + lang + " random index...");
        for(int i=0;i<InputFiles.get(name).get(lang).size(); ++i)
        {
            IdxNameToLangToFiles.get(name).get(lang).add(i);
            IdxNameToLangToFilesToSentences.get(name).get(lang).put(i,new ArrayList<>());
            for(int s=0;s<InputFiles.get(name).get(lang).get(i).getSentences().size();++s)
                IdxNameToLangToFilesToSentences.get(name).get(lang).get(i).add(s);
        }
    }
}
