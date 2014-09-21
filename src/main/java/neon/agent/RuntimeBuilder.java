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

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;

public class RuntimeBuilder
{
	public static Object getInnerClassInstance( final Object outerClassThisReference, final Class< ? > interfaceClass, final Object[] params )
	{
		return interfaceToBuilder.get( interfaceClass ).getInnerClassInstance( outerClassThisReference, params );
	}

	public static Object getInnerClassInstance( final Class< ? > interfaceClass, final Object[] params )
	{
		return interfaceToBuilder.get( interfaceClass ).getInnerClassInstance( null, params );
	}

	public static void add( final Class< ? > interfaceClass, final InnerClassTemplate template )
	{
		interfaceToBuilder.put( interfaceClass, new InnerClassBuilder( template ) );
	}

	private static final HashMap< Class< ? >, InnerClassBuilder > interfaceToBuilder = new HashMap< Class<?>, InnerClassBuilder >();

	private static class InnerClassBuilder
	{
		private final InnerClassTemplate innerClassTemplate;

		private int nextClassInstanceIndex;

		/**
		 * maps runtime signature to constructor.
		 */
		private final HashMap< ArrayList< Class< ? > >, Constructor< ? > > instanceCtors;

		public InnerClassBuilder( final InnerClassTemplate innerClassTemplate )
		{
			this.innerClassTemplate = innerClassTemplate;
			nextClassInstanceIndex = 0;
			instanceCtors = new HashMap< ArrayList< Class< ? > >, Constructor< ? > >();
		}

		public Object getInnerClassInstance( final Object outerClassThisReference, final Object[] params )
		{
			try
			{
				final ArrayList< Class< ? > > runtimeSignature = new ArrayList< Class<?> >( params.length );
				for ( final Object p : params )
				{
					final Class< ? > klass = p == null ? null : p.getClass();
					runtimeSignature.add( klass );
				}
				Constructor< ? > ctor = instanceCtors.get( runtimeSignature );
				if ( ctor == null )
				{
					final Class< ? > klass = innerClassTemplate.instantiate( nextClassInstanceIndex++ ).load();
					ctor = klass.getConstructors()[ 0 ];
					instanceCtors.put( runtimeSignature, ctor );
				}
				if ( outerClassThisReference == null )
					return ctor.newInstance();
				else
					return ctor.newInstance( outerClassThisReference );
			}
			catch ( final Exception e )
			{
				e.printStackTrace();
				throw new RuntimeException( e );
			}
		}
	}
}
