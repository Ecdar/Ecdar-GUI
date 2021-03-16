package ecdar.backend;

import ecdar.Ecdar;
import ecdar.abstractions.QueryState;

import java.io.*;
import java.util.function.Consumer;

public class jEcdarThread extends BackendThread {
    public jEcdarThread(final String query,
                        final Consumer<Boolean> success,
                        final Consumer<BackendException> failure,
                        final QueryListener queryListener) {
        super(query, success, failure, queryListener);
    }

    public void run() {
        ProcessBuilder pb = new ProcessBuilder("java", "-jar", "src/libs/j-Ecdar.jar");
        pb.redirectErrorStream(true);
        try {
            //Start the j-Ecdar process
            Process jEcdarEngineInstance = pb.start();

            //Communicate with the j-Ecdar process
            try (
                    var jEcdarReader = new BufferedReader(new InputStreamReader(jEcdarEngineInstance.getInputStream()));
                    var jEcdarWriter = new BufferedWriter(new OutputStreamWriter(jEcdarEngineInstance.getOutputStream()));
            ) {
                //Run the query with the j-Ecdar process
                jEcdarWriter.write("-inputFolder " + Ecdar.projectDirectory.get() + " -get " + query + "\n"); // Newline added to signal EOI
                jEcdarWriter.flush();

                //Read the result of the query from the j-Ecdar process
                String line;
                QueryState result = QueryState.RUNNING;
                while ((line = jEcdarReader.readLine()) != null) {
                    if (hasBeenCanceled.get()) {
                        cancel(jEcdarEngineInstance);
                        return;
                    }

                    System.out.println(line);

                    // Process the query result
                    if ((line.equals("true")) && (result.getStatusCode() <= QueryState.SUCCESSFUL.getStatusCode())) {
                        result = QueryState.SUCCESSFUL;
                    } else if (line.equals("false") && (result.getStatusCode() <= QueryState.ERROR.getStatusCode())){
                        result = QueryState.ERROR;
                    } else if (result.getStatusCode() <= QueryState.SYNTAX_ERROR.getStatusCode()) {
                        result = QueryState.SYNTAX_ERROR;
                    }

                    handleResult(result, line);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void cancel(Process jEcdarEngineInstance) {
        jEcdarEngineInstance.destroy();
        failure.accept(new BackendException.QueryErrorException("Canceled"));
    }
}
