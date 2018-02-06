package SW9.mutation;

import SW9.abstractions.Component;
import SW9.abstractions.Declarations;
import com.sun.deploy.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Systems declarations for two components.
 */
class TwoComponentSystemDeclarations extends Declarations {
    public TwoComponentSystemDeclarations(final Component component1, final Component component2) {
        super("TwoComponentSystemDeclarations");

        String declarationsText = "system " + component1.getName() + ", " + component2.getName() + ";\n";

        // Add inputs and outputs of component 1 to declarations
        final List<String> comp1Io = new ArrayList<>();
        for (final String inputSync : component1.getInputStrings()) comp1Io.add(inputSync + "?");
        for (final String outputSync : component1.getOutputStrings()) comp1Io.add(outputSync + "!");
        declarationsText += "IO " + component1.getName() + " {" + StringUtils.join(comp1Io, ", ") + "}\n";

        // Add inputs and outputs of component 2 to declarations
        final List<String> comp2Io = new ArrayList<>();
        for (final String inputSync : component2.getInputStrings()) comp2Io.add(inputSync + "?");
        for (final String outputSync : component2.getOutputStrings()) comp2Io.add(outputSync + "!");
        declarationsText += "IO " + component2.getName() + " {" + StringUtils.join(comp2Io, ", ") + "}";

        setDeclarationsText(declarationsText);
    }
}
