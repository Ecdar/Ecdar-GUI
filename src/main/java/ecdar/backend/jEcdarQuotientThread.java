package ecdar.backend;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import ecdar.Ecdar;
import ecdar.abstractions.Component;
import ecdar.abstractions.QueryState;
import ecdar.controllers.CanvasController;
import ecdar.utility.UndoRedoStack;
import javafx.application.Platform;
import javafx.collections.ObservableList;

import java.io.*;
import java.util.function.Consumer;

public class jEcdarQuotientThread extends BackendThread {
    public jEcdarQuotientThread(final String query,
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
                JsonParser parser = new JsonParser();
                StringBuilder combinedLines = new StringBuilder();
                QueryState result = QueryState.RUNNING;

                JsonObject returnedComponent = (JsonObject) parser.parse("{\n" +
                        "  \"name\": \"Administration\",\n" +
                        "  \"declarations\": \"clock z;\",\n" +
                        "  \"locations\": [\n" +
                        "    {\n" +
                        "      \"id\": \"L0\",\n" +
                        "      \"nickname\": \"\",\n" +
                        "      \"invariant\": \"\",\n" +
                        "      \"type\": \"INITIAL\",\n" +
                        "      \"urgency\": \"NORMAL\",\n" +
                        "      \"x\": 130.0,\n" +
                        "      \"y\": 100.0,\n" +
                        "      \"nicknameX\": 30.0,\n" +
                        "      \"nicknameY\": -10.0,\n" +
                        "      \"invariantX\": 30.0,\n" +
                        "      \"invariantY\": 10.0\n" +
                        "    },\n" +
                        "    {\n" +
                        "      \"id\": \"L1\",\n" +
                        "      \"nickname\": \"\",\n" +
                        "      \"invariant\": \"z \\u003c\\u003d 2\",\n" +
                        "      \"type\": \"NORMAL\",\n" +
                        "      \"urgency\": \"NORMAL\",\n" +
                        "      \"x\": 310.0,\n" +
                        "      \"y\": 100.0,\n" +
                        "      \"nicknameX\": 30.0,\n" +
                        "      \"nicknameY\": -10.0,\n" +
                        "      \"invariantX\": 50.0,\n" +
                        "      \"invariantY\": -40.0\n" +
                        "    },\n" +
                        "    {\n" +
                        "      \"id\": \"L2\",\n" +
                        "      \"nickname\": \"\",\n" +
                        "      \"invariant\": \"\",\n" +
                        "      \"type\": \"NORMAL\",\n" +
                        "      \"urgency\": \"NORMAL\",\n" +
                        "      \"x\": 310.0,\n" +
                        "      \"y\": 300.0,\n" +
                        "      \"nicknameX\": 30.0,\n" +
                        "      \"nicknameY\": -10.0,\n" +
                        "      \"invariantX\": 30.0,\n" +
                        "      \"invariantY\": 10.0\n" +
                        "    },\n" +
                        "    {\n" +
                        "      \"id\": \"L3\",\n" +
                        "      \"nickname\": \"\",\n" +
                        "      \"invariant\": \"z\\u003c\\u003d2\",\n" +
                        "      \"type\": \"NORMAL\",\n" +
                        "      \"urgency\": \"NORMAL\",\n" +
                        "      \"x\": 130.0,\n" +
                        "      \"y\": 300.0,\n" +
                        "      \"nicknameX\": 30.0,\n" +
                        "      \"nicknameY\": -10.0,\n" +
                        "      \"invariantX\": 20.0,\n" +
                        "      \"invariantY\": 20.0\n" +
                        "    }\n" +
                        "  ],\n" +
                        "  \"edges\": [\n" +
                        "    {\n" +
                        "      \"sourceLocation\": \"L0\",\n" +
                        "      \"targetLocation\": \"L1\",\n" +
                        "      \"status\": \"INPUT\",\n" +
                        "      \"select\": \"\",\n" +
                        "      \"guard\": \"\",\n" +
                        "      \"update\": \"z\\u003d0\",\n" +
                        "      \"sync\": \"grant\",\n" +
                        "      \"nails\": [\n" +
                        "        {\n" +
                        "          \"x\": 180.0,\n" +
                        "          \"y\": 100.0,\n" +
                        "          \"propertyType\": \"SYNCHRONIZATION\",\n" +
                        "          \"propertyX\": -20.0,\n" +
                        "          \"propertyY\": -30.0\n" +
                        "        },\n" +
                        "        {\n" +
                        "          \"x\": 230.0,\n" +
                        "          \"y\": 100.0,\n" +
                        "          \"propertyType\": \"UPDATE\",\n" +
                        "          \"propertyX\": -20.0,\n" +
                        "          \"propertyY\": -30.0\n" +
                        "        }\n" +
                        "      ]\n" +
                        "    },\n" +
                        "    {\n" +
                        "      \"sourceLocation\": \"L1\",\n" +
                        "      \"targetLocation\": \"L2\",\n" +
                        "      \"status\": \"OUTPUT\",\n" +
                        "      \"select\": \"\",\n" +
                        "      \"guard\": \"\",\n" +
                        "      \"update\": \"\",\n" +
                        "      \"sync\": \"coin\",\n" +
                        "      \"nails\": [\n" +
                        "        {\n" +
                        "          \"x\": 310.0,\n" +
                        "          \"y\": 190.0,\n" +
                        "          \"propertyType\": \"SYNCHRONIZATION\",\n" +
                        "          \"propertyX\": 10.0,\n" +
                        "          \"propertyY\": -10.0\n" +
                        "        }\n" +
                        "      ]\n" +
                        "    },\n" +
                        "    {\n" +
                        "      \"sourceLocation\": \"L2\",\n" +
                        "      \"targetLocation\": \"L3\",\n" +
                        "      \"status\": \"INPUT\",\n" +
                        "      \"select\": \"\",\n" +
                        "      \"guard\": \"\",\n" +
                        "      \"update\": \"z\\u003d0\",\n" +
                        "      \"sync\": \"pub\",\n" +
                        "      \"nails\": [\n" +
                        "        {\n" +
                        "          \"x\": 250.0,\n" +
                        "          \"y\": 300.0,\n" +
                        "          \"propertyType\": \"SYNCHRONIZATION\",\n" +
                        "          \"propertyX\": -20.0,\n" +
                        "          \"propertyY\": -40.0\n" +
                        "        },\n" +
                        "        {\n" +
                        "          \"x\": 200.0,\n" +
                        "          \"y\": 300.0,\n" +
                        "          \"propertyType\": \"UPDATE\",\n" +
                        "          \"propertyX\": -20.0,\n" +
                        "          \"propertyY\": -40.0\n" +
                        "        }\n" +
                        "      ]\n" +
                        "    },\n" +
                        "    {\n" +
                        "      \"sourceLocation\": \"L3\",\n" +
                        "      \"targetLocation\": \"L0\",\n" +
                        "      \"status\": \"OUTPUT\",\n" +
                        "      \"select\": \"\",\n" +
                        "      \"guard\": \"\",\n" +
                        "      \"update\": \"\",\n" +
                        "      \"sync\": \"patent\",\n" +
                        "      \"nails\": [\n" +
                        "        {\n" +
                        "          \"x\": 130.0,\n" +
                        "          \"y\": 200.0,\n" +
                        "          \"propertyType\": \"SYNCHRONIZATION\",\n" +
                        "          \"propertyX\": 10.0,\n" +
                        "          \"propertyY\": -10.0\n" +
                        "        }\n" +
                        "      ]\n" +
                        "    },\n" +
                        "    {\n" +
                        "      \"sourceLocation\": \"L1\",\n" +
                        "      \"targetLocation\": \"L1\",\n" +
                        "      \"status\": \"INPUT\",\n" +
                        "      \"select\": \"\",\n" +
                        "      \"guard\": \"\",\n" +
                        "      \"update\": \"\",\n" +
                        "      \"sync\": \"grant\",\n" +
                        "      \"nails\": [\n" +
                        "        {\n" +
                        "          \"x\": 310.0,\n" +
                        "          \"y\": 60.0,\n" +
                        "          \"propertyType\": \"SYNCHRONIZATION\",\n" +
                        "          \"propertyX\": 10.0,\n" +
                        "          \"propertyY\": -20.0\n" +
                        "        },\n" +
                        "        {\n" +
                        "          \"x\": 290.0,\n" +
                        "          \"y\": 60.0,\n" +
                        "          \"propertyType\": \"NONE\",\n" +
                        "          \"propertyX\": 0.0,\n" +
                        "          \"propertyY\": 0.0\n" +
                        "        }\n" +
                        "      ]\n" +
                        "    },\n" +
                        "    {\n" +
                        "      \"sourceLocation\": \"L1\",\n" +
                        "      \"targetLocation\": \"L1\",\n" +
                        "      \"status\": \"INPUT\",\n" +
                        "      \"select\": \"\",\n" +
                        "      \"guard\": \"\",\n" +
                        "      \"update\": \"\",\n" +
                        "      \"sync\": \"pub\",\n" +
                        "      \"nails\": [\n" +
                        "        {\n" +
                        "          \"x\": 350.0,\n" +
                        "          \"y\": 100.0,\n" +
                        "          \"propertyType\": \"SYNCHRONIZATION\",\n" +
                        "          \"propertyX\": 10.0,\n" +
                        "          \"propertyY\": -10.0\n" +
                        "        },\n" +
                        "        {\n" +
                        "          \"x\": 350.0,\n" +
                        "          \"y\": 120.0,\n" +
                        "          \"propertyType\": \"NONE\",\n" +
                        "          \"propertyX\": 0.0,\n" +
                        "          \"propertyY\": 0.0\n" +
                        "        }\n" +
                        "      ]\n" +
                        "    },\n" +
                        "    {\n" +
                        "      \"sourceLocation\": \"L2\",\n" +
                        "      \"targetLocation\": \"L2\",\n" +
                        "      \"status\": \"INPUT\",\n" +
                        "      \"select\": \"\",\n" +
                        "      \"guard\": \"\",\n" +
                        "      \"update\": \"\",\n" +
                        "      \"sync\": \"grant\",\n" +
                        "      \"nails\": [\n" +
                        "        {\n" +
                        "          \"x\": 350.0,\n" +
                        "          \"y\": 300.0,\n" +
                        "          \"propertyType\": \"SYNCHRONIZATION\",\n" +
                        "          \"propertyX\": -20.0,\n" +
                        "          \"propertyY\": -40.0\n" +
                        "        },\n" +
                        "        {\n" +
                        "          \"x\": 350.0,\n" +
                        "          \"y\": 320.0,\n" +
                        "          \"propertyType\": \"NONE\",\n" +
                        "          \"propertyX\": 0.0,\n" +
                        "          \"propertyY\": 0.0\n" +
                        "        }\n" +
                        "      ]\n" +
                        "    },\n" +
                        "    {\n" +
                        "      \"sourceLocation\": \"L3\",\n" +
                        "      \"targetLocation\": \"L3\",\n" +
                        "      \"status\": \"INPUT\",\n" +
                        "      \"select\": \"\",\n" +
                        "      \"guard\": \"\",\n" +
                        "      \"update\": \"\",\n" +
                        "      \"sync\": \"grant\",\n" +
                        "      \"nails\": [\n" +
                        "        {\n" +
                        "          \"x\": 130.0,\n" +
                        "          \"y\": 340.0,\n" +
                        "          \"propertyType\": \"SYNCHRONIZATION\",\n" +
                        "          \"propertyX\": -20.0,\n" +
                        "          \"propertyY\": 10.0\n" +
                        "        },\n" +
                        "        {\n" +
                        "          \"x\": 110.0,\n" +
                        "          \"y\": 340.0,\n" +
                        "          \"propertyType\": \"NONE\",\n" +
                        "          \"propertyX\": 0.0,\n" +
                        "          \"propertyY\": 0.0\n" +
                        "        }\n" +
                        "      ]\n" +
                        "    },\n" +
                        "    {\n" +
                        "      \"sourceLocation\": \"L3\",\n" +
                        "      \"targetLocation\": \"L3\",\n" +
                        "      \"status\": \"INPUT\",\n" +
                        "      \"select\": \"\",\n" +
                        "      \"guard\": \"\",\n" +
                        "      \"update\": \"\",\n" +
                        "      \"sync\": \"pub\",\n" +
                        "      \"nails\": [\n" +
                        "        {\n" +
                        "          \"x\": 90.0,\n" +
                        "          \"y\": 280.0,\n" +
                        "          \"propertyType\": \"SYNCHRONIZATION\",\n" +
                        "          \"propertyX\": -50.0,\n" +
                        "          \"propertyY\": -20.0\n" +
                        "        },\n" +
                        "        {\n" +
                        "          \"x\": 90.0,\n" +
                        "          \"y\": 300.0,\n" +
                        "          \"propertyType\": \"NONE\",\n" +
                        "          \"propertyX\": 0.0,\n" +
                        "          \"propertyY\": 0.0\n" +
                        "        }\n" +
                        "      ]\n" +
                        "    }\n" +
                        "  ],\n" +
                        "  \"description\": \"\",\n" +
                        "  \"x\": 5.0,\n" +
                        "  \"y\": 5.0,\n" +
                        "  \"width\": 450.0,\n" +
                        "  \"height\": 400.0,\n" +
                        "  \"includeInPeriodicCheck\": false\n" +
                        "}");

                addGeneratedComponent(new Component(returnedComponent));
                handleResult(QueryState.SUCCESSFUL, "");

                /*
                while ((line = jEcdarReader.readLine()) != null) {
                    if (hasBeenCanceled.get()) {
                        cancel(jEcdarEngineInstance);
                        return;
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
                    referenceToReturnedObject[0] = returnedObject;
                } catch (ParseException e) {
                    handleResult(QueryState.ERROR, e.getMessage());
                    throw new BackendException("The response from the backend could not be parsed as a JSON object");
                }*/
            }
        } catch (/*BackendException | */ IOException e) {
            handleResult(QueryState.UNKNOWN, e.getMessage());
            e.printStackTrace();
        }
    }

    private void cancel(Process jEcdarEngineInstance) {
        jEcdarEngineInstance.destroy();
        failure.accept(new BackendException.QueryErrorException("Canceled"));
    }

    private void addGeneratedComponent(Component newComponent){
        Platform.runLater(() -> {
            newComponent.setTemporary(true);

            ObservableList<Component> listOfGeneratedComponents = Ecdar.getProject().getTempComponents();
            Component matchedComponent = null;

            for(Component currentGeneratedComponent : listOfGeneratedComponents) {
                int comparisonOfNames = currentGeneratedComponent.getName().compareTo(newComponent.getName());

                if(comparisonOfNames == 0) {
                    matchedComponent = currentGeneratedComponent;
                    break;
                } else if(comparisonOfNames < 0) {
                    break;
                }
            }

            if(matchedComponent == null) {
                UndoRedoStack.pushAndPerform(() -> { // Perform
                    Ecdar.getProject().getTempComponents().add(newComponent);
                }, () -> { // Undo
                    Ecdar.getProject().getTempComponents().remove(newComponent);
                }, "Created new component: " + newComponent.getName(), "add-circle");
            } else {
                Component finalMatchedComponent = matchedComponent;
                UndoRedoStack.pushAndPerform(() -> { // Perform
                    Ecdar.getProject().getTempComponents().remove(finalMatchedComponent);
                    Ecdar.getProject().getTempComponents().add(newComponent);
                }, () -> { // Undo
                    Ecdar.getProject().getTempComponents().remove(newComponent);
                    Ecdar.getProject().getTempComponents().add(finalMatchedComponent);
                }, "Created new component: " + newComponent.getName(), "add-circle");
            }

            CanvasController.setActiveModel(newComponent);
        });
    }
}
