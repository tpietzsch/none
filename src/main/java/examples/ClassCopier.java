package examples;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;

/**
 * @author Matthias Arzt
 */
public class ClassCopier {

	public static <T> Class<? extends T> copy(Class<? extends T> original) {
		// NB: for each copying a new ClassLoader has to be created
		return new ClassCopyLoader().copyClass(original);
	}

	private static class ClassCopyLoader extends ClassLoader {

		public <T> Class<T> copyClass(Class<? extends T> aClass) {
			byte[] bytes = classToBytes(aClass);
			@SuppressWarnings("unchecked")
			Class<T> copy = (Class<T>) bytesToClass(aClass.getName(), bytes);
			return copy;
		}

		private byte[] classToBytes(Class<?> aClass) {
			String className = aClass.getName();
			String classAsPath = className.replace('.', '/') + ".class";
			InputStream stream = aClass.getClassLoader().getResourceAsStream(classAsPath);
			try {
				return IOUtils.toByteArray(stream);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		private Class<?> bytesToClass(String className, byte[] bytes) {
			Class<?> copiedClass = super.defineClass(className, bytes, 0, bytes.length);
			super.resolveClass(copiedClass);
			return copiedClass;
		}
	}
}
