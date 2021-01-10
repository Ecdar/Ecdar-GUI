package ecdar.backend;

import ecdar.Ecdar;
import ecdar.abstractions.QueryState;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class jEcdarQuotionThread {
    public AtomicBoolean hasBeenCanceled = new AtomicBoolean();
    final String query;
    final Consumer<Boolean> success;
    final Consumer<BackendException> failure;
    final QueryListener queryListener;

    public jEcdarQuotionThread(final String query,
                               final Consumer<Boolean> success,
                               final Consumer<BackendException> failure,
                               final QueryListener queryListener) {
        this.query = query;
        this.success = success;
        this.failure = failure;
        this.queryListener = queryListener;
    }

    public JSONObject run() {
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
                JSONParser parser = new JSONParser();
                StringBuilder combinedLines = new StringBuilder();
                QueryState result = QueryState.RUNNING;

                while ((line = jEcdarReader.readLine()) != null) {
                    if (hasBeenCanceled.get()) {
                        cancel(jEcdarEngineInstance);
                        return null;
                    }

                    //ToDo: Insert case for syntax error, when the response indicates an incorrect query
                    if (line.equals("false") && (result.getStatusCode() <= QueryState.ERROR.getStatusCode())) {
                        result = QueryState.ERROR;
                    } else {
                        combinedLines.append(line);
                    }
                }

                try{
                    JSONObject returnedObject = (JSONObject) parser.parse(combinedLines.toString());
                    handleResult(QueryState.SUCCESSFUL, "");
                    return returnedObject;
                } catch (ParseException e) {
                    handleResult(QueryState.ERROR, e.getMessage());
                    throw new BackendException("The response from the backend could not be parsed as a JSON object");
                }
            }
        } catch (BackendException | IOException e) {
            handleResult(QueryState.UNKNOWN, e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    void handleResult(QueryState result, String line) {
        if (result.getStatusCode() == QueryState.SUCCESSFUL.getStatusCode()) {
            success.accept(true);
        } else if (result.getStatusCode() == QueryState.ERROR.getStatusCode()){
            success.accept(false);
        } else if (result.getStatusCode() == QueryState.SYNTAX_ERROR.getStatusCode()) {
            failure.accept(new BackendException.QueryErrorException(line));
        } else {
            failure.accept(new BackendException.BadBackendQueryException(line));
        }
    }

    private void cancel(Process jEcdarEngineInstance) {
        jEcdarEngineInstance.destroy();
        failure.accept(new BackendException.QueryErrorException("Canceled"));
    }
}
