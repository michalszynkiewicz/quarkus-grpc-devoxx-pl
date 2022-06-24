package org.acme.riddle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Riddle {
    public String text;
    public String answer;
    public List<String> options;

    public Riddle(String text, String answer, String... otherOptions) {
        this.text = text;
        this.answer = answer;
        this.options = new ArrayList<>();
        Collections.addAll(options, otherOptions);
        options.add(answer);
        options.sort(String::compareTo);
    }

}
