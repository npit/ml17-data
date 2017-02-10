package gr.demokritos.iit.datasetgen.scramble;

import gr.demokritos.iit.datasetgen.utils.Utils;

import java.util.Arrays;
import java.util.Properties;

/**
 * Created by nik on 2/9/17.
 */
class Options
{
    static boolean checkRunOrder(String [] runs)
    {
        for(String s : runs)
        {
            if(methods.contains(s)) continue;
            System.err.println("Unspecified method : " + s); System.err.flush();
            return false;
        }
        return true;

    }

    Properties Props;
    String MethodName;
    boolean Verbosity;

    double DecisionProb;
    double Percentage;
    String miscValues[];

    enum methods
    {
        ME("ME",2),SR("SR",1),SO("SO",0);
        methods(String name,int idx)
        {
            this.idx=idx;
            this.name = name;
        }
        public static boolean contains(String name)
        {
            for(methods m : methods.values())
            {
                if(m.name.equals(name)) return true;
            }
            return false;
        }
        @Override
        public String toString(){return name;}
        String name;
        int idx;
    }
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
        System.out.println(fields_str[fields.PCNT.idx] + " : " + Percentage);
        System.out.println(fields_str[fields.PROB.idx] + " : " + DecisionProb);
        if(MethodName.equals(methods.SO.toString())) return;

        System.out.println(modifiers_str[modifiers.REUSE_A.idx] + ":" + hasMisc(modifiers_str[modifiers.REUSE_A.idx]));
        System.out.println(modifiers_str[modifiers.REUSE_S.idx] + ":" + hasMisc(modifiers_str[modifiers.REUSE_S.idx]));
        if(MethodName.equals(methods.SR.toString())) return;

        System.out.println(modifiers_str[modifiers.KEEPF.idx] + " : " + hasMisc(modifiers_str[modifiers.KEEPF.idx]));
    }


    void checkProb(double prob)
    {
        if(prob<0 || prob > 100) throw new NumberFormatException();
    }
    void setContent(String content, String field)
    {
        String raw_field = field.substring(3); // remove method tag
        try {
            if (raw_field.equals("prob"))
            {
                DecisionProb =  Double.parseDouble(content);
                checkProb(DecisionProb);
            }
            else if (raw_field.equals("pcnt"))
            {
                Percentage = Double.parseDouble(content);
                checkProb(Percentage);
            }
            else System.err.println("Ignoring undefined field: " + field + " for method " + MethodName);
        }
        catch(Exception ex)
        {
            System.err.println("Exception while trying to read " + content + " to field " + field + " , setting default.");
            System.err.flush();
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

    public Options(Properties props, methods MethodName)
    {

        this.Props = props;
        this.MethodName = MethodName.toString();
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
