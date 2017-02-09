package gr.demokritos.iit.datasetgen.scramble;

import java.util.Calendar;
import java.util.Properties;
import java.util.Random;

/**
 * Created by nik on 2/4/17.
 */
public class Randomizer {
    public Random getR() {
        return R;
    }

    Random R;
    public Randomizer(Properties props)
    {
        long seed;
        try {
            seed = Long.parseLong(props.getProperty("random_seed"));
        }
        catch (NumberFormatException ex)
        {
            System.err.println("Did not provide a random_seed field.");
            seed = Calendar.getInstance().getTimeInMillis();
            System.err.println("Using clock:" + Long.toString(seed));
            System.err.flush();
        }
        R = new Random(seed);
    }
    int getProb()
    {
        return R.nextInt(100);
    }
    int getInt(int lessThan)
    {
        return R.nextInt(lessThan);
    }
    public boolean coinToss(int probPcnt)
    {
        int prob = getProb();
        return  prob < probPcnt;
    }
}
