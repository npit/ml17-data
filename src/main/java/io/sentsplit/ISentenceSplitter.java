package io.sentsplit;

import java.util.List;
import java.util.Set;

/**
 * Created by nik on 2/7/17.
 */
public interface ISentenceSplitter {

    List<String> splitToSentences(String text, String locale);
    boolean checkLocaleAvailable(String lang);
}
