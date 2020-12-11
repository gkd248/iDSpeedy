package edu.illinois.cs.dt.tools.detection;

import com.google.common.math.IntMath;
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
import java.util.*;

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
    private long maxPermutations;
    private List<String> newTests = new ArrayList<>();

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

        List<String> jsonMap;

        try {
            JsonReader getLocalJsonFile = new JsonReader(new FileReader(DetectorPathManager.previousTestsPath().toString()));
            Type mapTokenType = new TypeToken<List<String>>() {
            }.getType();
            jsonMap = new Gson().fromJson(getLocalJsonFile, mapTokenType);
        }
        catch (IOException e) {
            jsonMap = new ArrayList<String>();
        }

        for(int j=0;j<tests.size();j++){
            String test = tests.get(j);

            if(!jsonMap.contains(test)){
                System.out.println("***Found a new test***");
                newTests.add(test);
            }
        }

        int numClasses = 0;
        long numPermutations = 1;
        Iterator<String> it = classToMethods.keySet().iterator();
        while(it.hasNext()){
            String className = it.next();
            int numTests = classToMethods.get(className).size();
            numPermutations *= IntMath.factorial(numTests);
            numClasses++;
        }

        // formula
        maxPermutations = IntMath.factorial(numClasses) * numPermutations;
        System.out.println(maxPermutations);

        //Load all previous orders into checkedOrders
        try {
            JsonReader getLocalJsonFile = new JsonReader(new FileReader(DetectorPathManager.newTestOrderPath().toString()));
            Type mapTokenType = new TypeToken<Set<String>>(){}.getType();
            Set<String> jsonMapOrders = new Gson().fromJson(getLocalJsonFile, mapTokenType);
            if(jsonMapOrders != null) {
                checkedOrders = jsonMapOrders;
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

        // if no new tests just sent new shuffled order
        // need to make this more sophisticated to account for multiple new tests at once
        if(newTests.isEmpty()){
            System.out.println("***No new tests found, just shuffling tests***");
            //Should add test order to checkedOrders set
            List<String> randOrder = generateShuffled();
            while(checkedOrders.contains(randOrder.toString()) && checkedOrders.size() < maxPermutations) {
                randOrder = generateShuffled();
                System.out.println("generating random order");
            }
            if(checkedOrders.size() < maxPermutations){
                checkedOrders.add(randOrder.toString());
                System.out.println("recording new order");
            } else{
                System.out.println("***All test orders run, running random order***");
            }

            returnList = randOrder;
        } else if(processedIndex >= newTests.size()){
            System.out.println("***No new tests left to process, just shuffling tests***");
            List<String> randOrder = generateShuffled();
            while(checkedOrders.contains(randOrder.toString()) && checkedOrders.size() < maxPermutations) {
                randOrder = generateShuffled();
            }
            if(checkedOrders.size() < maxPermutations){
                checkedOrders.add(randOrder.toString());
            } else{
                System.out.println("***All test orders run, running random order***");
            }

            returnList = randOrder;
        } else {
            System.out.println("***New tests were found***");
            // if there are new tests, run them at the front and back
            List<String> testOrder = new ArrayList<>();
            if(!newTestsRan.contains("Front")){

                //Put one of the tests at the front
//                    testOrder.add(newTests.get(processedIndex));
//                    newTests.remove(processedIndex);
//                    Collections.shuffle(newTests); //Should randomize the order in which new tests are added
//                    testOrder.addAll(newTests);

                System.out.println("ProcessedIdx: "+processedIndex);

                String testToAdd = newTests.get(processedIndex);
                System.out.println("Test to add to front: "+testToAdd);

                String[] testParts = testToAdd.split("\\.");
//                String className = testParts[testParts.length - 2];
                String className = TestShuffler.className(testToAdd);

                System.out.println("Class Name: "+className);

                List<String> testsInClass = classToMethods.get(className);



                testsInClass.remove(testToAdd);
                Collections.shuffle(testsInClass);
                testsInClass.add(0, testToAdd);

                testOrder.addAll(testsInClass);
                testOrder.addAll(generateExclusiveShuffled(className));

                newTestsRan.add("Front");
            } else if(newTestsRan.contains("Front") && !newTestsRan.contains("Back")){

                String testToAdd = newTests.remove(processedIndex);
                System.out.println("Test to add to back: "+testToAdd);

                String[] testParts = testToAdd.split("\\.");
                // String className = testParts[testParts.length - 2];
                String className = TestShuffler.className(testToAdd);
                System.out.println("Class Name: "+className);
                List<String> testsInClass = classToMethods.get(className);
                testsInClass.remove(testToAdd);
                Collections.shuffle(testsInClass);
                testsInClass.add(testToAdd);

                testOrder.addAll(generateExclusiveShuffled(className));
                testOrder.addAll(testsInClass);

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
                Files.write(DetectorPathManager.newTestOrderPath(), gsonString.getBytes(),
                        Files.exists(DetectorPathManager.newTestOrderPath()) ? StandardOpenOption.WRITE : StandardOpenOption.CREATE);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return returnList;
    }

    private List<String> generateShuffled() {
        return generateWithClassOrder(new RandomList<>(classToMethods.keySet()).shuffled());
    }

    //Generate shuffled list without the specified class
    private List<String> generateExclusiveShuffled(String className) {
        Iterator<String> classesIt = classToMethods.keySet().iterator();
        List<String> classes = new ArrayList<>();
        while (classesIt.hasNext()) {
            classes.add(classesIt.next());
        }
        classes.remove(className);

        return generateWithClassOrder(new RandomList<>(classes).shuffled());
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

    static int factorial(int n)
    {
        if (n == 0)
            return 1;

        return n*factorial(n-1);
    }
}
