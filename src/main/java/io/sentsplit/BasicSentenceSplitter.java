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
    public BasicSentenceSplitter()
    {
        BreakIterators = new HashMap<>();
        AvailableLocales = new HashSet<>();
        for(Locale loc : DateFormat.getAvailableLocales())
        {
            String localeDescription = loc.getDisplayLanguage().toLowerCase();
            if(localeDescription.isEmpty())
            {
                continue;
            }
            AvailableLocales.add(localeDescription);
        }
    }

    @Override
    public List<String>  splitToSentences(String text, String locale)
    {
        ArrayList<String> result = new ArrayList<>();
        checkLocaleAvailable(locale);
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
    public void checkLocaleAvailable(String lang) {

            if(AvailableLocales.contains(lang.toLowerCase()))
            {
                if( ! BreakIterators.containsKey(lang))
                    BreakIterators.put(lang, BreakIterator.getSentenceInstance(Locale.forLanguageTag(lang)));
            }
        }


}
