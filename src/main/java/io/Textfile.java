package io;

import javax.xml.soap.Text;

/**
 * Created by nik on 2/4/17.
 */
public class Textfile {
    String Text;

    public String getText() {
        return Text;
    }

    public String getLanguage() {
        return Language;
    }

    String Language;
    public Textfile(String text, String lang)
    {
        Text = text;
        Language = lang;
    }
}
