package scramble;

import java.util.Properties;
import java.util.Random;

/**
 * Created by nik on 2/4/17.
 */
public class Randomizer {
    Random R;
    public Randomizer(Properties props)
    {
        long seed = Long.parseLong(props.getProperty("random_seem","35916589713248"));
        R = new Random(seed);
    }
    int getInt()
    {
        return R.nextInt();
    }
}
