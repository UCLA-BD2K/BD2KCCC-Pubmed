/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.bd2kccc.bd2kcccpubmed;

import java.util.ArrayList;
import java.util.HashMap;

/**
 *
 * @author bleakley
 */
public class Thesaurus {
    
    //ArrayList<ArrayList<String>> synonymSet = new ArrayList();
    
    HashMap<String, String> synonymSet = new HashMap();
    ArrayList<String> multiWordTerms = new ArrayList();
    
    public Thesaurus() {
        
    }
    
    public String processText(String text) {
        
        for(String term : multiWordTerms)
            while(text.contains(term))
                text = text.replaceAll(term, synonymSet.get(term));
        
        return text;
    }
    
    void addSynonym (String word, String synonym) {
        synonymSet.put(synonym, word);
        if(synonym.split(" ").length > 1)
            multiWordTerms.add(synonym);
    }
    
    void addSynonyms (String word, String[] synonyms) {
        for(String s : synonyms)
            addSynonym(word, s);
    }
    
}
