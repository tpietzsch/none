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
