package examples;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Matthias Arzt
 */
public class ClassCopyProvider<T> {

	private final Map<Object, Class<? extends T>> map = new HashMap<Object, Class<? extends T>>();

	private final Class<? extends T> clazz;

	public ClassCopyProvider(Class<? extends T> clazz) {
		this.clazz = clazz;
	}

	Class<? extends T> getKeySpecific(Object key) {
		Class<? extends T> result = map.get(key);
		if (result == null) {
			result = ClassCopier.copy(clazz);
			map.put(key, result);
		}
		return result;
	}

	T createKeySpecific(Object key) {
		try {
			return getKeySpecific(key).getConstructor().newInstance();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
