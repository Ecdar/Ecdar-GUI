package ecdar.mutation.models;

import ecdar.abstractions.Component;
import ecdar.abstractions.Location;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

public class ChangeInvariantOperatorTest {

    @Test
    public void generateOneInvariantPart() {
        final Component c = new Component();

        final Location l1 = new Location();
        l1.setInvariant("x < 2");
        c.addLocation(l1);

        final List<Component> mutants = new ChangeInvariantOperator().generate(c);

        Assert.assertEquals(1, mutants.size());
        Assert.assertEquals("x < 2 + 1", mutants.get(0).getLocations().get(0).getInvariant());
    }

    @Test
    public void generateTwoInvariantParts() {
        final Component c = new Component();

        final Location l1 = new Location();
        l1.setInvariant("x < 2 && y <= 3");
        c.addLocation(l1);

        final List<Component> mutants = new ChangeInvariantOperator().generate(c);

        Assert.assertEquals(2, mutants.size());

        Assert.assertEquals(1, mutants.stream().filter(m -> m.getLocations().get(0).getInvariant().equals("x < 2 + 1 && y <= 3")).count());
        Assert.assertEquals(1, mutants.stream().filter(m -> m.getLocations().get(0).getInvariant().equals("x < 2 && y <= 3 + 1")).count());
    }

    @Test
    public void NumberOfMutations() {
        final Component c = new Component();

        Location l = new Location();
        l.setInvariant("x < 2 && y <= 3");
        c.addLocation(l);

        l = new Location();
        l.setInvariant("x < 1");
        c.addLocation(l);

        final List<Component> mutants = new ChangeInvariantOperator().generate(c);

        Assert.assertEquals(3, mutants.size());
    }
}