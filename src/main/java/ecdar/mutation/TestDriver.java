package ecdar.mutation;
import ecdar.abstractions.Component;
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

            while(verdict.equals(Verdict.NONE)) {
                System.out.println("c");
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
                    verdict = delay(testModelSimulation, mutantSimulation, timeUnit);
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

    private Verdict delay(ComponentSimulation testModelSimulation, ComponentSimulation mutantSimulation, int timeUnit){
        Instant delayStart = Instant.now();
        try {
            //Check if any output is ready, if there is none, do delay
            if(inputStream.available() == 0){
                Thread.sleep(timeUnit);
                //Do Delay
                if(!testModelSimulation.delay(Duration.between(delayStart, Instant.now()).getSeconds())){
                    return Verdict.FAIL;
                } else {
                    mutantSimulation.delay(Duration.between(delayStart, Instant.now()).getSeconds());
                }
            }

            //Do output if any output happened when sleeping
            if(inputStream.available() != 0){
                String outputFromSUT = readFromSUT();
                if(!testModelSimulation.triggerOutput(outputFromSUT)){
                    return Verdict.FAIL;
                } else if (!mutantSimulation.triggerOutput(outputFromSUT)) {
                    return Verdict.PASS;
                }
            }
            return Verdict.NONE;
        } catch (InterruptedException e) {
            e.printStackTrace();
            return Verdict.INCONCLUSIVE;
        } catch (IOException e) {
            e.printStackTrace();
            return Verdict.INCONCLUSIVE;
        } catch (MutationTestingException e) {
            e.printStackTrace();
            return Verdict.INCONCLUSIVE;
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
