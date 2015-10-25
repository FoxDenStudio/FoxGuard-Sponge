package tk.elektrofuchse.fox.foxguard.commands.util;

import java.util.HashMap;
import java.util.function.Consumer;

/**
 * Created by Fox on 10/23/2015.
 */
public class CallbackHashMap<K,V> extends HashMap<K,V> {
    final private Consumer<Object> callback;

    public CallbackHashMap(Consumer<Object> callback) {
        this.callback = callback;
    }

    @Override
    public V get(Object key) {
        V value = super.get(key);
        if (value != null) {
            return value;
        } else {
            callback.accept(key);
            return super.get(key);
        }
    }
}
