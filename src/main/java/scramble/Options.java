package scramble;

import utils.Utils;

import java.util.Arrays;
import java.util.Properties;

/**
 * Created by nik on 2/9/17.
 */
class Options
{
    static final String ME = "ME";
    static final String SR = "SR";
    static final String SO = "SO";
    Properties Props;
    String MethodName;
    boolean Verbosity;

    int DecisionProb;
    int Percentage;
    String miscValues[];
    enum modifiers
    {
        KEEPF(0),KEEPS(1),REUSE_A(2),REUSE_S(3);
        modifiers(int idx)
        {
            this.idx=idx;
        }
        int idx;
    }
    enum fields
    {
        PROB(0),PCNT(1);
        fields(int idx)
        {
            this.idx=idx;
        }
        int idx;
    }
    final String[] modifiers_str ={"keep_first","keep_second","reuse_art","reuse_sent"};
    final String[] fields_str ={"prob","pcnt"};

    void tell()
    {
        System.out.println( "Name  : " + MethodName);
        System.out.println(fields_str[fields.PCNT.idx] + " : " + DecisionProb);
        System.out.println(fields_str[fields.PROB.idx] + " : " + Percentage);
        if(MethodName.equals(this.SO)) return;

        System.out.println(modifiers_str[modifiers.REUSE_A.idx] + ":" + hasMisc(modifiers_str[modifiers.REUSE_A.idx]));
        System.out.println(modifiers_str[modifiers.REUSE_S.idx] + ":" + hasMisc(modifiers_str[modifiers.REUSE_S.idx]));
        if(MethodName.equals(this.SR)) return;

        System.out.println(modifiers_str[modifiers.KEEPF.idx] + " : " + hasMisc(modifiers_str[modifiers.KEEPF.idx]));
    }


    void checkProb(int prob)
    {
        if(prob<0 || prob > 100) throw new NumberFormatException();
    }
    void setContent(String content, String field)
    {
        String raw_field = field.substring(3); // remove method tag
        try {
            if (raw_field.equals("prob"))
            {
                DecisionProb =  Integer.parseInt(content);
                checkProb(DecisionProb);
            }
            else if (raw_field.equals("pcnt")) Percentage = Integer.parseInt(content);
            else System.err.println("Ignoring undefined field: " + field + " for method " + MethodName);
        }
        catch(Exception ex)
        {
            System.err.println("Exception while trying to read " + content + " to field " + field);
            setDefault(field);
        }
    }
    void setDefault(String field)
    {
        String rawField = field.substring(3); // remove method tag
        if(Verbosity) System.out.println("Setting default for field " + field);
        if(rawField.equals("prob")) DecisionProb = 50;
        else if(rawField.equals("pcnt")) Percentage = 100;
        else System.err.println("Ignoring undefined field: " + field + " for method " + MethodName);

    }

    void readField(String field)
    {

        String content = getContent(field);
        if( content.isEmpty()) return;
        setContent(content,field);

    }

    String getContent(String field)
    {
        String res =  Props.getProperty(field,"");
        if(res.isEmpty()) setDefault(field);
        return res;
    }

    public Options(Properties props, String MethodName)
    {

        this.Props = props;
        this.MethodName = MethodName;
        String modifiers = Props.getProperty("modifiers");
        Verbosity = Utils.csvStringContains(modifiers,"verbose");
        if(Verbosity) System.out.println("Initializing method options for " + MethodName);
        for(String field : fields_str)
            readField(MethodName + "_" + field);
    }
    public boolean hasMisc(String miscVal)
    {
        if(miscValues == null)
        {
            String content = Props.getProperty(MethodName + "_modifiers","");
            if(content.isEmpty()) return false;
            miscValues = content.split(",");
        }
        return Arrays.asList(miscValues).contains(miscVal);
    }
    public boolean hasMisc(modifiers mod)
    {
        return hasMisc(modifiers_str[mod.idx]);
    }

}
