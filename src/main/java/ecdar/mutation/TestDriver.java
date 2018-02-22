package ecdar.mutation;
import ecdar.mutation.models.*;

import java.io.*;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

public class TestDriver {

    private String SUTPath;
    private String stringInput;
    private BufferedReader input;
    private InputStream inputStream;
    private BufferedWriter output;
    private enum Verdict {NONE, INCONCLUSIVE, PASS, FAIL}

    public TestDriver(List<MutationTestCase> mutationTestCases, String SUTPath, int timeUnit, int bound){
        int testcaseNumber = 1;
        for (MutationTestCase testCase  : mutationTestCases) {
            Verdict verdict = Verdict.NONE;
            System.out.println("testcase" + testcaseNumber);
            testcaseNumber++;

            NonRefinementStrategy strategy = testCase.getStrategy();
            ComponentSimulation testModelSimulation = new ComponentSimulation(testCase.getTestModel());
            ComponentSimulation mutantSimulation = new ComponentSimulation(testCase.getMutant());
            initializeAndRunProcess();
            int step = 0;

            int step = 1;

            try {
                Instant delayStart = Instant.now();
                //Check if any output is ready, if there is none, do delay
                System.out.println("a");
                if(inputStream.available() == 0){
                    System.out.println("b");
                    Thread.sleep(timeUnit);
                    System.out.println("ba");
                    //Do Delay
                    if(!testModelSimulation.delay(Duration.between(delayStart, Instant.now()).getSeconds())){
                        System.out.println("bb");
                        verdict = Verdict.FAIL;
                        break;
                    } else {
                        mutantSimulation.delay(Duration.between(delayStart, Instant.now()).getSeconds());
                        System.out.println("bc");
                    }
                }

                System.out.println("c");
                //Do output if any output happened when sleeping
                if(inputStream.available() != 0){
                    System.out.println("ca");
                    String outputFromSUT = readFromSUT();
                    System.out.println("cb");
                    if(!testModelSimulation.triggerOutput(outputFromSUT)){
                        verdict = Verdict.FAIL;
                        break;
                    } else if (!mutantSimulation.triggerOutput(outputFromSUT)) {
                        verdict = Verdict.PASS;
                        break;
                    }
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (MutationTestingException e) {
                e.printStackTrace();
            }


            System.out.println("d");
            while(verdict.equals(Verdict.NONE)) {
                StrategyRule rule = strategy.getRule(testModelSimulation.getCurrentLocation().getId(), mutantSimulation.getCurrentLocation().getId(), testModelSimulation.getValuations(), mutantSimulation.getValuations());
                if((rule) == null){
                    verdict = Verdict.INCONCLUSIVE;
                    break;
                }

                if(rule instanceof ActionRule){
                    testModelSimulation.runActionRule((ActionRule)rule);
                    mutantSimulation.runActionRule((ActionRule)rule);

                    String sync = (ActionRule)rule.getSync();

                    writeToSUT(sync);
                } else if(rule instanceof DelayRule){
                    Instant delayStart = Instant.now();
                    try {
                        //Check if any output is ready, if there is none, do delay
                        if(inputStream.available() == 0){
                            Thread.sleep(timeUnit);
                            //Do Delay
                            if(!testModelSimulation.delay(Duration.between(delayStart, Instant.now()).getSeconds())){
                                verdict = Verdict.FAIL;
                                break;
                            } else {
                                mutantSimulation.delay(Duration.between(delayStart, Instant.now()).getSeconds());
                            }
                        }

                        //Do output if any output happened when sleeping
                        if(inputStream.available() != 0){
                            System.out.println("BITCONNEEEEEECT");
                            String outputFromSUT = readFromSUT();
                            if(!testModelSimulation.triggerOutput(outputFromSUT)){
                                verdict = Verdict.FAIL;
                            } else if (!mutantSimulation.triggerOutput(outputFromSUT)) {
                                verdict = Verdict.PASS;
                            }
                        }
                    } catch (InterruptedException e) {
                                e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                if(step == bound){
                    verdict = Verdict.INCONCLUSIVE;
                }
                step++;
            }
        }
    }

    private void initializeAndRunProcess(){
        try {
            Process SUT = Runtime.getRuntime().exec("java -jar C:\\Users\\Chres\\Desktop\\SimpleMutationProgram\\out\\artifacts\\test\\test.jar");
            output = new BufferedWriter(new OutputStreamWriter(SUT.getOutputStream()));
            inputStream = SUT.getInputStream();
            input = new BufferedReader(new InputStreamReader(inputStream));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void writeToSUT(String outputBroadcast){
        try {
            output.write(outputBroadcast+"\n");
            output.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String readFromSUT(){
        try {
            return stringInput = input.readLine();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;

    }

    public String getSUTPath() {
        return SUTPath;
    }

    public void setSUTPath(String SUTPath) {
        this.SUTPath = SUTPath;
    }

    public String getStringInput() { return stringInput; }
}
