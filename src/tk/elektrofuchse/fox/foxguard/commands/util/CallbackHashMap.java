package tk.elektrofuchse.fox.foxguard.commands.util;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Created by Fox on 10/23/2015.
 * Project: foxguard
 */
public class CallbackHashMap<K,V> extends HashMap<K,V> {
    final private BiConsumer<Object, Map<K, V>> callback;

    public CallbackHashMap(BiConsumer<Object,Map<K,V>> callback) {
        this.callback = callback;
    }

    @Override
    public V get(Object key) {
        V value = super.get(key);
        if (value != null) {
            return value;
        } else {
            callback.accept(key, this);
            return super.get(key);
        }
    }
}
