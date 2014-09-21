package neon.agent;

import java.lang.reflect.InvocationTargetException;

public class ClassLoaderUtil
{
	private static ClassLoader loader;

	private static Class< ? > cls;

	private static java.lang.reflect.Method method;

	static
	{
		try
		{
			loader = ClassLoaderUtil.class.getClassLoader();
			cls = Class.forName( "java.lang.ClassLoader" );
			method = cls.getDeclaredMethod( "defineClass", new Class[] { String.class, byte[].class, int.class, int.class } );
		}
		catch ( final Exception e )
		{
			e.printStackTrace();
			System.exit( 1 ); // TODO: be a bit more relaxed
		}
	}

	/**
	 * Load a byte array as a class in the {@link ClassLoader} that was used to
	 * load this ({@link ClassLoaderUtil}) class.
	 *
	 * @param className
	 *            the name of the class to load.
	 * @param b
	 *            the bytes of the class to load.
	 * @return the loaded class.
	 * @throws IllegalAccessException
	 * @throws IllegalArgumentException
	 * @throws InvocationTargetException
	 */
	public static Class< ? > loadClass( final String className, final byte[] b ) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException
	{
		System.out.println( ClassLoaderUtil.class.getSimpleName() + ".loadClass( " + className + " )" );

		method.setAccessible( true );
		try
		{
			final Object[] args = new Object[] { className, b, new Integer( 0 ), new Integer( b.length ) };
			return ( Class< ? > ) method.invoke( loader, args );
		}
		finally
		{
			method.setAccessible( false );
		}
	}
}
