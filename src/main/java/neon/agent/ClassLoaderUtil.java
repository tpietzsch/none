/*
 * #%L
 * neon: runtime code modification for imglib2.
 * %%
 * Copyright (C) 2014 Tobias Pietzsch.
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
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
