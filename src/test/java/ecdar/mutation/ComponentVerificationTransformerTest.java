package ecdar.mutation;

import ecdar.abstractions.Component;
import ecdar.abstractions.Location;
import ecdar.abstractions.Project;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static ecdar.mutation.ComponentVerificationTransformer.applyAngelicCompletionForComponent;
import static ecdar.mutation.ComponentVerificationTransformer.applyDemonicCompletionToComponent;

public class ComponentVerificationTransformerTest {
    @Test
    public void demonicCompletionAddsUniversalLocationAndMatchAllInputAndOutputEdges() {
        final Project project = new Project();
        final Component comp = new Component(false, "Comp");
        project.addComponent(comp);

        applyDemonicCompletionToComponent(comp);

        Assertions.assertTrue(comp.getLocations().stream().anyMatch(e -> e.getType().equals(Location.Type.UNIVERSAL)));
        Assertions.assertTrue(comp.getListOfEdgesFromDisplayableEdges(comp.getInputEdges()).stream().anyMatch(e -> e.getSync().equals("*")));
        Assertions.assertTrue(comp.getListOfEdgesFromDisplayableEdges(comp.getOutputEdges()).stream().anyMatch(e -> e.getSync().equals("*")));
    }

    @Test
    public void angelicCompletionAddsUniversalLocationAndMatchAllInputAndOutputEdges() {
        final Project project = new Project();
        final Component comp = new Component(false, "Comp");
        project.addComponent(comp);

        applyAngelicCompletionForComponent(comp);

        // ToDo NIELS
   }
}
