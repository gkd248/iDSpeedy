import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.nio.charset.Charset;
import java.util.HashSet;

public class incrementalResultCollector {
    public static void main(String args[]) {
        File dir = new File("../incrementalResults");

        HashSet<String> foundFlakyTests = new HashSet<String>();

        for(File file: dir.listFiles()) {
            //System.out.println(file.getName());
            if(file.isFile() && file.getName().endsWith(".json")) {
                BufferedReader br = null;
                try {
                    br = new BufferedReader(new FileReader(file));
                    String results = br.readLine();
                    JSONObject json = new JSONObject(results);
                    JSONArray arr = (JSONArray) json.get("dts");
                    String[] tests = new String[arr.length()];
                    for(int i=0; i<tests.length; i++) {
                       // tests[i]=arr.optString(i);
                        tests[i] = (String) arr.getJSONObject(i).get("name");
                    }

                    for(String test : tests) {
                        if(!foundFlakyTests.contains(test))
                            foundFlakyTests.add(test);
                    }
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        System.out.println("Found " + foundFlakyTests.size() + " flaky tests overall");
    }
}
