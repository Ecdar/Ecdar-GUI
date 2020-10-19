package ecdar.backend;

import com.uppaal.engine.Engine;
import com.uppaal.engine.Problem;
import com.uppaal.model.system.UppaalSystem;
import ecdar.code_analysis.CodeAnalysis;
import javafx.application.Platform;

import java.io.*;
import java.util.Vector;
import java.util.function.Consumer;

public class jECDARDriver {
    public Thread runQuery(final String query,
                           final Consumer<Boolean> success,
                           final Consumer<BackendException> failure,
                           final Consumer<Engine> engineConsumer,
                           final QueryListener queryListener) {
        return new Thread(() -> {
            try {
                /*ProcessBuilder pb = new ProcessBuilder("src/libs/j-Ecdar.jar", "-help");
                Process process = pb.start();
                try (var reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream()))) {

                    String line;

                    while ((line = reader.readLine()) != null) {
                        System.out.println(line);
                    }

                }*/
                // Create a list to store the problems of the query
                final Vector<Problem> problems = new Vector<>();

                // Get the system, and fill the problems list if any
                //final UppaalSystem system = engin.getSystem(ecdarDocument.toXmlDocument(), problems);

                // Run on UI thread
                Platform.runLater(() -> {
                    // Clear the UI for backend-errors
                    CodeAnalysis.clearBackendErrors();

                    // Check if there is any problems
                    if (!problems.isEmpty()) {
                        problems.forEach(problem -> {
                            System.out.println("problem: " + problem);
                            CodeAnalysis.addBackendError(new CodeAnalysis.Message(problem.getMessage(), CodeAnalysis.MessageType.ERROR));
                        });
                    }
                });

                ProcessBuilder pb = new ProcessBuilder("src/libs/j-Ecdar.jar", "-help\n");
                pb.inheritIO();
                //-rq -json samples/json/CarAlarm/Model/ Specification: Alarm
                var process = pb.start();
                var reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println(line);
                    /*final char result = line.charAt(0);

                    // Process the query result
                    if (result == 'T') {
                        success.accept(true);
                    } else if (result == 'F') {
                        success.accept(false);
                    } else if (result == 'M') {
                        failure.accept(new BackendException.QueryErrorException("UPPAAL Engine was uncertain on the result"));
                    } else {
                        failure.accept(new BackendException.BadUPPAALQueryException("Unable to run query"));
                    }*/
                }

                reader.close();
                process.destroy();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }
}
