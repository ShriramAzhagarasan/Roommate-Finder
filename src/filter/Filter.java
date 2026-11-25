package filter;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** A single search criterion over items of type T. */
@FunctionalInterface
public interface Filter<T> {

    boolean matches(T item);

    /** How many individual criteria this filter represents. Composite filters count their children. */
    default int criteriaCount() {
        return 1;
    }

    /** Returns the items that pass the filter, preserving their order. */
    static <T> List<T> applyTo(Iterable<T> items, Filter<? super T> filter) {
        List<T> result = new ArrayList<>();
        Iterator<T> iterator = items.iterator();
        while (iterator.hasNext()) {
            T item = iterator.next();
            if (filter.matches(item)) {
                result.add(item);
            }
        }
        return result;
    }
}
