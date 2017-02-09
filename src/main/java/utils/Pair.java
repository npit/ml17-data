package utils;

import java.util.ArrayList;

/**
 * Created by nik on 2/9/17.
 */
public class  Pair <T1,T2>{
    T1 Value1;
    T2 Value2;
    public Pair(T1 t1, T2 t2)
    {
        Value1 = t1;
        Value2 = t2;
    }
    public Pair()
    {

    }
    public void set(T1 t1, T2 t2)
    {
        Value1 = t1;
        Value2 = t2;
    }
    public T1 first()
    {
        return Value1;
    }
    public T2 second()
    {
        return Value2;
    }
    @Override
    public String toString()
    {
        return Value1.toString() + " " + Value2.toString();
    }


}
