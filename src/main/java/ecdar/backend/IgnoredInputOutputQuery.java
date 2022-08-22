package ecdar.backend;

import ecdar.abstractions.Query;
import ecdar.presentations.QueryPresentation;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.HashMap;

public class IgnoredInputOutputQuery {
    private final Query query;
    private final QueryPresentation queryPresentation;
    private final HashMap<String, Boolean> ignoredInputs;
    private final VBox ignoredInputsVBox;
    private final HashMap<String, Boolean> ignoredOutputs;
    private final VBox ignoredOutputsVBox;
    public int tries = 0;

    public IgnoredInputOutputQuery(Query query, QueryPresentation queryPresentation, HashMap<String, Boolean> ignoredInputs, VBox ignoredInputsVBox, HashMap<String, Boolean> ignoredOutputs, VBox ignoredOutputsVBox) {
        this.query = query;
        this.queryPresentation = queryPresentation;
        this.ignoredInputs = ignoredInputs;
        this.ignoredInputsVBox = ignoredInputsVBox;
        this.ignoredOutputs = ignoredOutputs;
        this.ignoredOutputsVBox = ignoredOutputsVBox;
    }

    public Query getQuery() {
        return query;
    }

    public void addNewElementsToMap(ArrayList<String> inputs, ArrayList<String> outputs) {
        // Add inputs to list and as checkboxes in UI
        for (String key : inputs) {
            if (!this.ignoredInputs.containsKey(key)) {
                this.queryPresentation.addInputOrOutput(key, false, this.ignoredInputs, this.ignoredInputsVBox);
                this.ignoredInputs.put(key, false);
            }
        }

        // Add inputs to list and as checkboxes in UI
        for (String key : outputs) {
            if (!this.ignoredInputs.containsKey(key)) {
                this.queryPresentation.addInputOrOutput(key, false, this.ignoredInputs, this.ignoredInputsVBox);
                this.ignoredInputs.put(key, false);
            }
        }
    }
}
