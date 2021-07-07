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
        ProcessBuilder pb = new ProcessBuilder("java", "-jar", "src/libs/j-Ecdar.jar", "-inputFolder " + Ecdar.projectDirectory.get(), "-getNewComponent " + query);
        pb.redirectErrorStream(true);
        try {
            Process jEcdarEngineInstance = pb.start();

            try (
                    var jEcdarReader = new BufferedReader(new InputStreamReader(jEcdarEngineInstance.getInputStream()));
            ) {
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
