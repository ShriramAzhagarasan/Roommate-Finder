package filter;

import java.util.ArrayList;
import java.util.List;

/**
 * A group of filters combined with AND ({@link Mode#ALL}) or OR ({@link Mode#ANY}).
 * Groups can contain other groups, so both matching and counting walk the tree recursively.
 * An empty group matches everything.
 */
public class CompositeFilter<T> implements Filter<T> {

    public enum Mode { ALL, ANY }

    private final Mode mode;
    private final List<Filter<T>> children = new ArrayList<>();

    public CompositeFilter(Mode mode) {
        this.mode = mode;
    }

    public CompositeFilter<T> add(Filter<T> filter) {
        children.add(filter);
        return this;
    }

    public boolean isEmpty() {
        return children.isEmpty();
    }

    @Override
    public boolean matches(T item) {
        return children.isEmpty() || evaluate(item, 0);
    }

    private boolean evaluate(T item, int index) {
        if (index >= children.size()) {
            // ALL: every child passed. ANY: no child passed.
            return mode == Mode.ALL;
        }
        boolean passed = children.get(index).matches(item); // may itself recurse into a nested group
        if (mode == Mode.ALL && !passed) {
            return false;
        }
        if (mode == Mode.ANY && passed) {
            return true;
        }
        return evaluate(item, index + 1);
    }

    @Override
    public int criteriaCount() {
        return countFrom(0);
    }

    private int countFrom(int index) {
        if (index >= children.size()) {
            return 0;
        }
        return children.get(index).criteriaCount() + countFrom(index + 1);
    }
}
