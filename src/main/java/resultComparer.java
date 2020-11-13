
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


// assuming same amount fo runs for both and resultCollector
public class resultComparer {

    public static void main(String[] args) {

        String project = args[0];
        try{
            Files.createDirectories(Paths.get("results/comparison/"));
        } catch(IOException e){
            System.out.println("Failed to create results directory");
        }
        Path endPath = Paths.get("results/comparison/"+project.replace("/","")+"comparisonResults.txt");

        Double incTime = -1.0d;
        Integer incFlakyTests = -1;

        Double randTime = -1.0d;
        Integer randFlakyTests = -1;

        try{

            File file = new File("results/incremental/"+project.replace("/","")+"runTimeResults.txt");
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line;

            while ((line = br.readLine()) != null) {
                if (line.contains("Overall")) {
                    String[] words = line.split(": ");
                    incTime = Double.parseDouble(words[1]);
                    String incRunTime = "Incremental Overall Runtime: "+incTime.toString();
                    try {
                        Files.write(endPath, incRunTime.getBytes());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else if (line.contains("Number")){
                    String[] words = line.split(": ");
                    incFlakyTests = Integer.parseInt(words[1]);
                    String incFlakyTest = "Incremental Number of Flaky Tests: "+incFlakyTests.toString();
                    try {
                        Files.write(endPath, (System.lineSeparator() + incFlakyTest).getBytes(), StandardOpenOption.APPEND);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }






        } catch(Exception e){
            System.out.println("Failed to read incremental results");
        }

        try{

            File file = new File("results/random-class-method/"+project.replace("/","")+"runTimeResults.txt");
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line;

            while ((line = br.readLine()) != null) {
                if (line.contains("Overall")) {
                    String[] words = line.split(": ");
                    randTime = Double.parseDouble(words[1]);
                    String randRunTime = "Random Overall Runtime: "+randTime.toString();
                    try {
                        Files.write(endPath, (System.lineSeparator() + randRunTime).getBytes(), StandardOpenOption.APPEND);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else if (line.contains("Number")){
                    String[] words = line.split(": ");
                    randFlakyTests = Integer.parseInt(words[1]);
                    String randFlakyTest = "Random Number of Flaky Tests: "+randFlakyTests.toString();
                    try {
                        Files.write(endPath, (System.lineSeparator() + randFlakyTest).getBytes(), StandardOpenOption.APPEND);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

        } catch(Exception e){
            System.out.println("Failed to read random results");
        }


        String timeComp = "";
        if(incTime < randTime){
            timeComp = "Incremental ran faster than Random.";
        } else if(incTime == randTime){
            timeComp = "Both runners took the same amount of time.";
        } else{
            timeComp = "Random ran faster than Incremental.";
        }

        String flakyComp = "";
        if(incFlakyTests > randFlakyTests){
            flakyComp = "Incremental found more flaky tests than Random.";
        } else if(incFlakyTests == randFlakyTests){
            flakyComp = "Both runners found the same amount of flaky tests.";
        } else{
            flakyComp = "Random found more flaky tests than Incremental.";
        }


        try {
            Files.write(endPath, (System.lineSeparator() + System.lineSeparator() + timeComp + System.lineSeparator() + flakyComp).getBytes(), StandardOpenOption.APPEND);
        } catch (IOException e) {
            e.printStackTrace();
        }


    }

}
