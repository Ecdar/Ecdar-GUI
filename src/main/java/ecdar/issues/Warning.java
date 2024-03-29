package ecdar.issues;

import javafx.beans.Observable;
import javafx.scene.Node;
import org.kordamp.ikonli.material.Material;

import java.util.function.Predicate;

public class Warning<T extends Node> extends Issue<T> {

    @Override
    protected Material getIcon() {
        return Material.WARNING;
    }

    public Warning(final Predicate<T> presentPredicate, final T subject, final Observable... observables) {
        super(presentPredicate, subject, observables);
    }
}
