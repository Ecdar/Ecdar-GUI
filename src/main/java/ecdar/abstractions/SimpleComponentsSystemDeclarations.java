package ecdar.abstractions;

import ecdar.abstractions.Component;
import ecdar.abstractions.Declarations;

import java.util.ArrayList;
import java.util.List;

/**
 * Systems declarations for some components.
 */
public class SimpleComponentsSystemDeclarations extends Declarations {
    public SimpleComponentsSystemDeclarations(final Component ... components) {
        super("SimpleComponentsSystemDeclarations");

        if (components.length < 1) throw new IllegalArgumentException("Cannot contruct declarations without any components");

        final List<String> names = new ArrayList<>();
        for (final Component component : components) names.add(component.getName());

        final StringBuilder declarationsText = new StringBuilder("system " + String.join(", ", names) + ";\n");

        // Add inputs and outputs of components to declarations
        for (final Component component : components) {
            final List<String> comp1Io = new ArrayList<>();
            for (final String inputSync : component.getInputStrings()) comp1Io.add(inputSync + "?");
            for (final String outputSync : component.getOutputStrings()) comp1Io.add(outputSync + "!");
            declarationsText.append("IO ").append(component.getName()).append(" {").append(String.join(", ", comp1Io)).append("}\n");
        }

        setDeclarationsText(declarationsText.toString());
    }
}
