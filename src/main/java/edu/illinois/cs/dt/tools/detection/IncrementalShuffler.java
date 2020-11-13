package edu.illinois.cs.dt.tools.detection;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import com.google.gson.reflect.TypeToken;

import java.io.FileNotFoundException;
import java.lang.reflect.Type;
import com.reedoei.eunomia.collections.RandomList;
import edu.illinois.cs.dt.tools.utility.MD5;
import edu.illinois.cs.testrunner.configuration.Configuration;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.Collections;

public class IncrementalShuffler {
    public static String className(final String testName) {
        return testName.substring(0, testName.lastIndexOf('.'));
    }

    private final HashMap<String, List<String>> classToMethods;

    private final String type;
    private final List<String> tests;
    private final Set<String> alreadySeenOrders = new HashSet<>();
    private Set<String> checkedOrders = new HashSet<>();
    private final Set<String> newTestsRan = new HashSet<>();
    private boolean overwritten = false;
    private boolean accessedNewTests = false;

    //Variable to keep track of which new tests have been processed
    private int processedIndex = 0;
    private int roundsRemaining;

    public IncrementalShuffler(final String type, final int rounds, final List<String> tests) {
        this.type = type;
        this.tests = tests;
        roundsRemaining = rounds;
        classToMethods = new HashMap<>();

        for (final String test : tests) {
            final String className = className(test);

            if (!classToMethods.containsKey(className)) {
                classToMethods.put(className, new ArrayList<>());
            }

            classToMethods.get(className).add(test);
        }

        //Load all previous orders into checkedOrders
        try {
            JsonReader getLocalJsonFile = new JsonReader(new FileReader(DetectorPathManager.INCREMENTAL.resolve(DetectorPathManager.NEWTEST_TESTORDER).toString()));
            Type mapTokenType = new TypeToken<Set<String>>(){}.getType();
            Set<String> jsonMap = new Gson().fromJson(getLocalJsonFile, mapTokenType);
            if(jsonMap != null) {
                checkedOrders = jsonMap;
            }
        } catch (FileNotFoundException e) {
            System.out.println("Accessing newTest-testOrder.json for the first time.");
        }
    }

    private String historicalType() {
        if (type.equals("random")) {
            return Configuration.config().getProperty("detector.random.historical_type", "random-class");
        } else {

            return Configuration.config().getProperty("detector.random.historical_type", "random");
        }
    }

    public List<String> shuffledOrder(final int i) {
        return incrementalOrder();
    }

    public List<String> incrementalOrder() {

        // *** check if any new tests were run ***
        // if yes, run test at front and back
        // if no, run tests in new order
        System.out.println("***Using incremental detector and shuffling for a round***");
        System.out.println("***Rounds Remaining: "+roundsRemaining+"***");
        roundsRemaining--;

        List<String> returnList = new ArrayList<>();


        List<String> jsonMap;

        try {
            JsonReader getLocalJsonFile = new JsonReader(new FileReader(DetectorPathManager.INCREMENTAL.resolve(DetectorPathManager.PREVIOUS_TESTS).toString()));
            Type mapTokenType = new TypeToken<List<String>>() {
            }.getType();
            jsonMap = new Gson().fromJson(getLocalJsonFile, mapTokenType);
        }
        catch (IOException e) {
            jsonMap = new ArrayList<String>();
        }
        // check is there are any new tests
        List<String> newTests = new ArrayList<>();
        for(int j=0;j<tests.size();j++){
            String test = tests.get(j);

            if(!jsonMap.contains(test)){
                System.out.println("***Found a new test***");
                newTests.add(test);
            }
        }

        // if no new tests just sent new shuffled order
        // need to make this more sophisticated to account for multiple new tests at once
        if(newTests.isEmpty()){
            System.out.println("***No new tests found, just shuffling tests***");
            //Should add test order to checkedOrders set
            List<String> randOrder = generateShuffled();
            while(checkedOrders.contains(randOrder.toString())) {
                randOrder = generateShuffled();
            }
            checkedOrders.add(randOrder.toString());
            returnList = randOrder;
        } else if(processedIndex >= newTests.size()){
            System.out.println("***No new tests left to process, just shuffling tests***");
            List<String> randOrder = generateShuffled();
            while(checkedOrders.contains(randOrder.toString())) {
                randOrder = generateShuffled();
            }
            checkedOrders.add(randOrder.toString());

            returnList = randOrder;
        } else {
            System.out.println("***New tests were found***");
            // if there are new tests, run them at the front and back
            List<String> testOrder = new ArrayList<>();
            if(!newTestsRan.contains("Front")){
                if(newTests.size() > 1) {
                    //Put one of the tests at the front
                    testOrder.add(newTests.get(processedIndex));
                    newTests.remove(processedIndex);
                    Collections.shuffle(newTests); //Should randomize the order in which new tests are added
                    testOrder.addAll(newTests);
                }
                else {
                    testOrder.addAll(newTests);
                }

                testOrder.addAll(jsonMap);
                newTestsRan.add("Front");
            } else if(newTestsRan.contains("Front") && !newTestsRan.contains("Back")){
                testOrder.addAll(jsonMap);
                String newTest = newTests.remove(processedIndex);
                if (!(newTests.size()>0)) {
                    testOrder.add(newTest);
                } else{
                    Collections.shuffle(newTests);
                    testOrder.addAll(newTests);
                    testOrder.add(newTest);
                }
                processedIndex++;
                newTestsRan.remove("Front");
            } else{
                return generateShuffled();
            }

            if(!overwritten){
                checkedOrders.add(testOrder.toString());
                overwritten = true;
                accessedNewTests = true;
            } else{
                checkedOrders.add(testOrder.toString());}

            returnList = testOrder;
        }

        //Add all previous rounds to JSON file
        if(roundsRemaining <= 0) {
            Gson gson = new Gson();
            Type gsonType = new TypeToken<Set>(){}.getType();
            String gsonString = gson.toJson(checkedOrders,gsonType);
            try {
                Files.write(DetectorPathManager.INCREMENTAL.resolve(DetectorPathManager.NEWTEST_TESTORDER), gsonString.getBytes(), Files.exists(DetectorPathManager.INCREMENTAL.resolve(DetectorPathManager.NEWTEST_TESTORDER)) ? StandardOpenOption.WRITE : StandardOpenOption.CREATE);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return returnList;
    }

    private List<String> generateShuffled() {
        return generateWithClassOrder(new RandomList<>(classToMethods.keySet()).shuffled());
    }

    private List<String> generateWithClassOrder(final List<String> classOrder) {
        final List<String> fullTestOrder = new ArrayList<>();

        for (final String className : classOrder) {
            // random-class only shuffles classes, not methods
            if ("random-class".equals(type)) {
                fullTestOrder.addAll(classToMethods.get(className));
            } else {
                // the standard "random" type, will shuffle both
                fullTestOrder.addAll(new RandomList<>(classToMethods.get(className)).shuffled());
            }
        }

        alreadySeenOrders.add(MD5.md5(String.join("", fullTestOrder)));

        return fullTestOrder;
    }
}
