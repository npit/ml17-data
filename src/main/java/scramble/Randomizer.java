package scramble;

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
        long seed = Long.parseLong(props.getProperty("random_seed","35916589713248"));
        R = new Random(seed);
    }
    int getProb()
    {
        return R.nextInt(100);
    }
    public boolean coinToss(int probPcnt)
    {
        int prob = getProb();
        return  prob < probPcnt;
    }
}
