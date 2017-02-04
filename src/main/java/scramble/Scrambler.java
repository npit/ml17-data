package scramble;

import io.Textfile;


import java.util.Properties;
import java.util.Set;

/**
 * Created by nik on 2/3/17.
 */
public class Scrambler {
    OpenNLPSentenceSplitter ONLPSplitter;
    Randomizer Rand;
    Properties Props;
    public Scrambler(Properties props)
    {
        Props = props;
        Rand = new Randomizer(Props);
        ONLPSplitter = new OpenNLPSentenceSplitter(Props.getProperty("sentenceSplitter_model_paths"));
    }
    // do the scramblin'
    public void scramble(Set<Textfile> textfiles)
    {
        // for each text file
        for(Textfile tf: textfiles)
        {
            // split to sentences
            String [] sentences = splitSentences(tf.getText());
        }
    }
    private String [] splitSentences(String text)
    {
        // split by dot and / or newline
        String splitMode = Props.getProperty("split_mode");
        if( splitMode.equals("openNLP"))
        {
            return ONLPSplitter.splitToSentences(text);
        }
        else
            return text.split("\\.\\r?\\n");
    }
}
