package io.sentsplit;

import java.text.BreakIterator;
import java.text.DateFormat;
import java.util.*;

/**
 * Created by npittaras on 6/2/2017.
 */
public class BasicSentenceSplitter implements ISentenceSplitter{
    private Map<String,BreakIterator> BreakIterators;
    Set<String> AvailableLocales;
    public BasicSentenceSplitter(Properties props)
    {
        BreakIterators = new HashMap<>();
        AvailableLocales = new HashSet<>();

        String localeSetting = props.getProperty("locale");
        if(localeSetting == null) localeSetting="EN,us";

        String [] locParts = localeSetting.split(",");
        Locale outputLocale;
        if(locParts.length > 1) outputLocale = new Locale(locParts[0],locParts[1]);
        else outputLocale = new Locale(locParts[0]);


        for(Locale loc : DateFormat.getAvailableLocales())
        {
            String localeDescription = loc.getDisplayLanguage().toLowerCase();
            if(localeDescription.isEmpty())
            {
                continue;
            }
            localeDescription = loc.getDisplayLanguage(outputLocale);
            AvailableLocales.add(localeDescription);
        }
    }

    @Override
    public List<String>  splitToSentences(String text, String locale)
    {
        ArrayList<String> result = new ArrayList<>();
        if(! checkLocaleAvailable(locale)) return null;
        BreakIterator iterator = BreakIterators.get(locale);

        iterator.setText(text);
        int start = iterator.first();
        for (int end = iterator.next(); end != BreakIterator.DONE; start = end, end = iterator.next()) {
            String sentence = text.substring(start,end).trim();
            if(sentence == null || sentence.isEmpty())
                continue;
            result.add(sentence);
        }
        return result;
    }

    @Override
    public boolean checkLocaleAvailable(String lang) {

            if(AvailableLocales.contains(lang.toLowerCase()))
            {
                if( ! BreakIterators.containsKey(lang))
                    BreakIterators.put(lang, BreakIterator.getSentenceInstance(Locale.forLanguageTag(lang)));
            }
            else
            {
                System.err.println("Unable to find locale for language:" + lang);
                return false;
            }
            return true;
        }


}
