
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.StandardOpenOption;
import java.nio.file.Files;

import edu.emory.mathcs.backport.java.util.Arrays;
import org.json.JSONArray;
import org.json.JSONObject;


public class resultCollector{

    private static final String resultsPath = "/.dtfixingtools/detection-results/incremental/";

    public static void main(String[] args) {

        String project = args[0];
        Path endPath = Paths.get(project.replace("/","")+"runTimeResults.txt");
        int idx = 0;
        Double overallTime = 0.0d;

        System.out.println(System.getProperty("user.dir"));

        try{
            while(true){
                File file = new File("../"+project+resultsPath+"/round"+idx+".json");
                BufferedReader br = new BufferedReader(new FileReader(file));
                String results = br.readLine();

                JSONObject json = new JSONObject(results);
                Double roundTime = (Double) json.get("roundTime");
                overallTime += roundTime;
                String formatted = "Round "+idx+": "+roundTime.toString();
                System.out.println(formatted);

                if(idx == 0){
                    Files.write(endPath, formatted.getBytes());
                } else{
                    Files.write(endPath, (System.lineSeparator() + formatted).getBytes(), StandardOpenOption.APPEND);
                }

                idx++;
            }
        } catch(Exception e){
            System.out.println("Reached end of results files");
        }

        String formattedOverall = "Overall Runtime for "+idx+" rounds: "+overallTime.toString();
        System.out.println(formattedOverall);
        try {
            Files.write(endPath, (System.lineSeparator() + formattedOverall).getBytes(), StandardOpenOption.APPEND);
        } catch (IOException e) {
            e.printStackTrace();
        }

        String[] flakyTests = getFlakyResults(project);
        String flakyResult = System.lineSeparator() + "Number of flaky tests found: " + flakyTests.length + System.lineSeparator() + "Flaky Tests: " + Arrays.toString(flakyTests);
        try {
            Files.write(endPath, flakyResult.getBytes(), StandardOpenOption.APPEND);
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("Compiled all results from "+project);
    }

    /*
        getFlakyResults()
        Also writes to file with the number of flaky tests found and a list of them
     */
    private static String[] getFlakyResults(String project) {
        File flakyList = new File("../"+project+"/.dtfixingtools/detection-results/flaky-lists.json");
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(flakyList));
            String results = br.readLine();
            JSONObject json = new JSONObject(results);
            JSONArray arr = (JSONArray) json.get("dts");
            String[] tests = new String[arr.length()];
            for(int i=0; i<tests.length; i++) {
                tests[i]=arr.optString(i);
            }
            return tests;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}