package io;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by nik on 2/4/17.
 */
public class Textfile {
    String Text;

    public void setFilePath(String filePath) {
        FilePath = filePath;
    }

    public String getFilePath() {
        return FilePath;
    }

    String FilePath;

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
    @Override
    public String toString()
    {
        String res="";
        res += "[" + getLanguage() + "]\n";
        res +="[" + getFilePath() + "]\n";
        res+="[" + getText() +"]";
        return res;
    }
}
