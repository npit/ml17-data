package io;

import javax.xml.soap.Text;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by nik on 2/4/17.
 */
public class Textfile {
    String Text;

    public void setSentences(List<String> sentences) {
        Sentences = sentences;
    }

    public List<String> getSentences() {
        return Sentences;
    }

    List<String> Sentences;

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
        Sentences = new ArrayList<>();
    }

    public Textfile(Textfile other)
    {
        Text = other.getText();
        Language = other.getLanguage();
        Sentences = other.getSentences();
    }

    public Textfile(String lang, List<String> sents)
    {
        Text = "";
        for(String s : sents) {
            Text += s + "\n";
        }
        Language = lang;
        Sentences = sents;
    }
}
