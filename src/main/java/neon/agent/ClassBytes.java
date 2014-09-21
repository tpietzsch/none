package neon.agent;

import java.lang.reflect.InvocationTargetException;

public class ClassBytes
{
	public final String name;

	public final byte[] bytes;

	private Class< ? > loadedClass;

	public ClassBytes( final String name, final byte[] bytes )
	{
		this.name = name;
		this.bytes = bytes;
		loadedClass = null;
	}

	public Class< ? > load() throws IllegalAccessException, IllegalArgumentException, InvocationTargetException
	{
		if ( loadedClass == null )
			loadedClass = ClassLoaderUtil.loadClass( name, bytes );
		return loadedClass;
	}
}