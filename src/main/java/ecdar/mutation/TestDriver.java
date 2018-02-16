package ecdar.mutation;
import java.io.*;

public class TestDriver {

    private String SUTPath;
    private String stringInput;

    public TestDriver(){
        final Process p;
        try {
            p = Runtime.getRuntime().exec("java -jar C:\\Users\\Chres\\Desktop\\SimpleMutationProgram\\out\\artifacts\\test\\test.jar");
        new Thread(() -> {
            try {
                BufferedWriter output = new BufferedWriter(new OutputStreamWriter(p.getOutputStream()));
                BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));
                stringInput = input.readLine();
                System.out.println(stringInput);
                output.write("Tobias");
                output.newLine();
                output.flush();
                stringInput = input.readLine();
                System.out.println(stringInput);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();

        p.waitFor();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getSUTPath() {
        return SUTPath;
    }

    public void setSUTPath(String SUTPath) {
        this.SUTPath = SUTPath;
    }

    public String getStringInput() { return stringInput; }
}
