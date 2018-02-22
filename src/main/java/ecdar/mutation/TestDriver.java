package ecdar.mutation;
import ecdar.mutation.models.MutationTestCase;
import ecdar.mutation.models.NonRefinementStrategy;

import java.io.*;
import java.util.List;

public class TestDriver {

    private String SUTPath;
    private String stringInput;
    private BufferedReader input;
    private BufferedWriter output;

    public TestDriver(List<MutationTestCase> mutationTestCases, String SUTPath){

        for (MutationTestCase testCase  : mutationTestCases) {
            NonRefinementStrategy strategy = testCase.getStrategy();

            initializeAndRunProcess();

            
        }
    }

    private void initializeAndRunProcess(){
        try {
            Process SUT = Runtime.getRuntime().exec("java -jar C:\\Users\\Chres\\Desktop\\SimpleMutationProgram\\out\\artifacts\\test\\test.jar");
            output = new BufferedWriter(new OutputStreamWriter(SUT.getOutputStream()));
            input = new BufferedReader(new InputStreamReader(SUT.getInputStream()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void write(String outputBroadcast){
        try {
            output.write(outputBroadcast);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String read(){
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
