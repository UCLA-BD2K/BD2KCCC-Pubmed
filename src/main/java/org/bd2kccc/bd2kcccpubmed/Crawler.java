/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.bd2kccc.bd2kcccpubmed;

import com.github.kevinsawicki.http.HttpRequest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import static org.apache.commons.lang3.StringEscapeUtils.unescapeHtml4;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 *
 * @author bleakley
 */
public class Crawler {
    
    public static final String[] BD2K_GRANTS = { "GM114833", "EB020406", "HG007990", "HG008540", "AI117925", "AI117924", "EB020403", "GM114838", "HL127624", "HL127366", "EB020404", "EB020405", "HG007963"};
    public static final String[] BD2K_CENTERS = { "HeartBD2K", "BDDS", "BDTG", "CCD", "CEDAR", "CPCP", "ENIGMA", "KnowEnG", "LINCS-DCIC", "LINCS-TG", "MD2K", "Mobilize", "PIC-SURE" };
    
    //ignoring bioCADDE, U24 grant AI117966
    
    public static void main(String[] args)
    {
        HashMap<Integer, ArrayList<String>> publications = new HashMap();
        
        for(int i = 0; i < BD2K_CENTERS.length; i++)
        {
            String searchUrl = "http://eutils.ncbi.nlm.nih.gov/entrez/eutils/esearch.fcgi?db=pubmed&term=" + BD2K_GRANTS[i] + "[Grant%20Number]&retmode=json&retmax=1000";
            
            System.out.println(searchUrl);
            HttpRequest request = HttpRequest.get(searchUrl);
            JSONObject req = new JSONObject(request.body());
            JSONArray idlist = req.getJSONObject("esearchresult").getJSONArray("idlist");
            System.out.println(BD2K_CENTERS[i] + ": " + idlist.length());
            for(int j = 0; j < idlist.length(); j++)
            {
                int pmid = idlist.optInt(j);
                if(!publications.containsKey(pmid))
                {
                    ArrayList<String> centerList = new ArrayList();
                    centerList.add(BD2K_CENTERS[i]);
                    publications.put(pmid, centerList);
                } else {
                    publications.get(pmid).add(BD2K_CENTERS[i]);
                }
            }
        }
        
        Integer[] pmids = publications.keySet().toArray(new Integer[0]);
        int collaborations = 0;
        for(int i = 0; i < pmids.length; i++)
            if(publications.get(pmids[i]).size() > 1)
                collaborations++;
        
        System.out.println(pmids.length + " publications found.");
        System.out.println(collaborations + " BD2K Center collaborations found.");
        String summaryUrl = "http://eutils.ncbi.nlm.nih.gov/entrez/eutils/esummary.fcgi?db=pubmed&retmode=json&rettype=abstract&id=" + pmids[0];
        for(int i = 1; i < pmids.length; i++)
            summaryUrl += "," + pmids[i];
        
        System.out.println(summaryUrl);
        HttpRequest request = HttpRequest.get(summaryUrl);
        JSONObject result = new JSONObject(request.body()).getJSONObject("result");
        
        ArrayList<Publication> publicationStructs = new ArrayList();
        
        for(int i = 0; i < pmids.length; i++) {
            JSONObject pub = result.getJSONObject("" + pmids[i]);
            Publication publication = new Publication();
            publication.pmid = pmids[i];
            publication.centers = publications.get(pmids[i]);
            publication.name = pub.optString("title", "NO_TITLE");
            //remove &lt;i&gt; and &lt;/i&gt;
            //replace &amp;amp; with &
            publication.name = unescapeHtml4(publication.name);
            publication.date = pub.optString("sortpubdate", "1066/01/01 00:00").substring(0, 10);
            publication.date = publication.date.replaceAll("/", "-");
            
            JSONArray articleIDs = pub.getJSONArray("articleids");
            for(int j = 0; j < articleIDs.length(); j++) {
                JSONObject id = articleIDs.optJSONObject(j);
                if(id.optString("idtype").equals("doi"))
                    publication.doi = id.optString("value");
            }

            String issue = pub.optString("issue");
            if(!issue.isEmpty())
                issue = "(" + issue + ")";
            
            publication.citation = pub.optString("source") + ". " + pub.optString("pubdate") + ";" + pub.optString("volume") + issue + ":" + pub.optString("pages") + ".";
            
            publication.authors = new ArrayList();
            JSONArray aut = pub.getJSONArray("authors");
            for(int j = 0; j < aut.length(); j++) {
                publication.authors.add(aut.getJSONObject(j).optString("name"));
            }
            
            publicationStructs.add(publication);
        }
        
        Collections.sort(publicationStructs);
        
        for(Publication p : publicationStructs) {
            if(p.isTool())
                System.out.println(p.name);
        }
        
        JSONArray outputData = new JSONArray();
        
        for(Publication p : publicationStructs) {
            JSONArray row = new JSONArray();
            row.put(0, p.getName());
            row.put(1, p.getType());
            row.put(2, p.getDate());
            row.put(3, p.getCenter());
            row.put(4, p.getInitiative());
            row.put(5, p.getDescription());
            outputData.put(row);
        }
        
        System.out.println(outputData.toString(1));
        
    }//end main method
    
}

class Publication implements Comparable {
    int pmid;
    ArrayList<String> centers;
    
    String name;
    String date;
    String doi;
    String citation;
    ArrayList<String> authors;
    
    public static final int UPPERCASE = 1, LOWERCASE = 2, TITLECASE = 3, MIXEDCASE = 4;
    
    String getName() {
        return name;
    }
    
    String getType() {
        if(isTool())
            return "Publications, Tools";
        return "Publications";
    }
    
    String getDate() {
        return date;
    }
    
    String getCenter() {
        String list = centers.get(0);
        
        for(int i = 1; i < centers.size(); i++)
            list += ", " + centers.get(i);
        
        return list;
    }
    
    String getInitiative() {
        return "";
    }
    
    
    
    //algorithm app server tool platform database interface knowledgebase framework visualization data program programming
    
    //test bioinformatics, nature methods, computational biology, nature biotechnology
    
    //abstract features:
    //keywords in abstract
    //presence of url
    //presense of github/sourceforge/bitbucket url
    @Deprecated
    int getSentenceCase(String sentence) {
        String[] words = sentence.split("[- ,]+");//split by hyphens, spaces, and commas
        boolean uppercase = true;
        boolean lowercase = true;
        boolean titlecase = true;
        boolean mixedcase = true;
        for(String word : words) {
            int wcase = getCase(word);
            if(wcase == MIXEDCASE)
                return MIXEDCASE;
            if(wcase == UPPERCASE && word.length() > 1)
                return UPPERCASE;
        }
        
        if(uppercase)
            return UPPERCASE;
        if(lowercase)
            return LOWERCASE;
        if(titlecase)
            return TITLECASE;
        if(mixedcase)
            return MIXEDCASE;
        
        return 0;
    }
    
    int getCase(String word) {
        
        if(word.isEmpty())
            return 0;
        
        boolean uppercase = true;
        boolean lowercase = true;
        boolean titlecase = true;
        boolean mixedcase = true;
        char[] letters = word.toCharArray();

        if(Character.isLetter(letters[0])) {
            if(Character.isLowerCase(letters[0])){
                //titlecase = false;
                //let's not require every word to be titlecase
            }
        } else {
            uppercase = false;
            lowercase = false;
            titlecase = false;
        }

        for(int i = 1; i < letters.length; i++) {
            char letter = letters[i];
            
            

            if(!Character.isLetter(letter)) {
                uppercase = false;
                lowercase = false;
                titlecase = false;
                break;
            }

            if(Character.isLowerCase(letter))
                uppercase = false;
            else {
                lowercase = false;
                titlecase = false;
            }
        }
        
        if(uppercase && word.length() > 1)
            return UPPERCASE;
        if(lowercase)
            return LOWERCASE;
        if(titlecase)
            return TITLECASE;
        if(mixedcase)
            return MIXEDCASE;
        
        return 0;
    }
    
    boolean isGene(String word) {
        
        if(true)return false;
        //this threshold varies quite a lot, from < 1 to > 400
        double similarityThreshod = 1;
        
        String mygeneUrl = "http://mygene.info/v2/query?q=";
        HttpRequest request = HttpRequest.get(mygeneUrl + word);
        JSONObject requestJson = new JSONObject(request.body());
        double geneSimilarity = requestJson.optDouble("max_score", 0);
        
        return geneSimilarity > similarityThreshod;
    }
    
    //consider any single word left of colon that isn't a gene
    //if it's uppercase or mixed case, count it
    
    boolean isTool() {
        //title features:
        //number of words in in title
        int wordsInTitle = 0;
        //number of words left of colon
        int wordsLeftOfColon = 0;
        //number of words in mixedcase (excluding genes)
        int mixedCase = 0;
        //number of words in uppercase (excluding genes)
        int upperCase = 0;
        //number of words in mixedcase left of colon (excluding genes)
        int mixedCaseLeftOfColon = 0;
        //number of words in uppercase left of colon (excluding genes)
        int upperCaseLeftOfColon = 0;
        
        //keywords in title
        
        String title = name;
        String firstHalf = title.split(": ")[0];
        for(String word : firstHalf.split("[-.,] |[, ]")) {
            
            //temporary measure
            if(word.equals("Brainhack"))
                return false;
            
            wordsInTitle++;
            wordsLeftOfColon++;
            if(getCase(word) == MIXEDCASE) {
                System.out.println("mixed: " + word);
                if(isGene(word))
                    continue;
                mixedCase++;
                mixedCaseLeftOfColon++;
            }
            if(getCase(word) == UPPERCASE) {
                System.out.println("upper: " + word);
                if(isGene(word))
                    continue;
                upperCase++;
                upperCaseLeftOfColon++;
            }
        }
        
        try{
        String secondHalf = title.split(": ")[1];
        for(String word : secondHalf.split("[-.,] |[, ]")) {
            wordsInTitle++;
            if(getCase(word) == MIXEDCASE) {
                System.out.println("mixed: " + word);
                if(isGene(word))
                    continue;
                mixedCase++;
            }
            if(getCase(word) == UPPERCASE) {
                System.out.println("upper: " + word);
                if(isGene(word))
                    continue;
                upperCase++;
            }
        }
        }catch(Exception e){}
        
        if(wordsLeftOfColon == 0)
            return false;
        if(wordsLeftOfColon > 1)
            return false;
        

        return true;
    }
    
    String getDescription() {
        String desc = name + "\n\n";
        
        desc += authors.get(0);
        for(int i = 1; i < authors.size(); i++)
            desc += ", " + authors.get(i);
        desc += ".";
        
        String link;
        if(doi != null)
            link = "doi: <a href=\"https://doi.org/" + doi + "\">" + doi + ".</a>";
        else link = "pmid: <a href=\"http://www.ncbi.nlm.nih.gov/pubmed/" + pmid + "\">" + pmid + ".</a>";
        
        desc += "\n\n" + citation + " " + link;
        
        return desc;
    }
    
    @Override
    public String toString() {
        return getDescription();
    }

    @Override
    public int compareTo(Object o) {
        return date.compareTo(((Publication)o).date);
    }
}
