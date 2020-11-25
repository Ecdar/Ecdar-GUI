package ecdar.backend;

import com.uppaal.engine.Problem;
import ecdar.Ecdar;
import ecdar.abstractions.QueryState;
import ecdar.code_analysis.CodeAnalysis;
import javafx.application.Platform;

import java.io.*;
import java.util.Vector;
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
                jEcdarWriter.write("-rq -json " + Ecdar.projectDirectory.get() + " " + query.replaceAll("\\s", "") + "\n"); // Newline added to signal EOI
                jEcdarWriter.flush();

                //Read the result of the query from the j-Ecdar process
                String line;
                QueryState result = QueryState.RUNNING;
                while ((line = jEcdarReader.readLine()) != null) {
                    if (hasBeenCanceled.get()) {
                        cancel(jEcdarEngineInstance);
                        return;
                    }

                    // Process the query result
                    if ((line.equals("true") || line.equals("")) && (result.getStatusCode() <= QueryState.SUCCESSFUL.getStatusCode())) {
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
