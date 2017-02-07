package dataset;

import io.MultilingFileReader;
import io.Textfile;
import io.TextfileCollection;
import scramble.Scrambler;

import java.io.*;
import java.util.Properties;
import java.util.Set;

/**
 * Created by nik on 2/3/17.
 */

/**
 * Project to generate data for the multiling summarization task
 */
public class Launcher {
    public static void main(String[] args) {
        if(args.length<1)
        {
            System.err.println("Need config. file.");
            return ;
        }
        String configurationFile = args[0];
        Properties props = new Properties();
        try {
            props.load( new FileInputStream(configurationFile));
        } catch (IOException e) {
            e.printStackTrace();
        }

        MultilingFileReader reader = new MultilingFileReader(props);
        // read all docs
        reader.read();

        TextfileCollection TFColl= reader.getTextfileCollection();
        // scramble new  data
        Scrambler scrambler = new Scrambler(props);
       scrambler.setTextfileColl(TFColl);
        scrambler.run();

    }
}
