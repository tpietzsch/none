package neon.agent;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.Method;

public class GetAnnotatedMethodsClassVisitor extends ClassVisitor
{
	private final Map< String, Class< ? > > annotationDescriptorToAnnotation;

	private final Set< Method > annotatedMethods;

	private final Map< Class< ? >, Set< Method > > annotationToAnnotatedMethods;

	public GetAnnotatedMethodsClassVisitor( final int api, final Collection< Class< ? > > annotations )
	{
		super( api );
		annotationDescriptorToAnnotation = new HashMap< String, Class< ? > >();
		annotatedMethods = new HashSet< Method >();
		annotationToAnnotatedMethods = new HashMap< Class< ? >, Set< Method > >();
		for ( final Class< ? > annotation : annotations )
		{
			annotationDescriptorToAnnotation.put( Type.getDescriptor( annotation ), annotation );
			annotationToAnnotatedMethods.put( annotation, new HashSet< Method >() );
		}
	}

	@Override
	public MethodVisitor visitMethod( final int access, final String mname, final String mdesc, final String signature, final String[] exceptions )
	{
		return new MethodVisitor( api )
		{
			@Override
			public AnnotationVisitor visitAnnotation( final String desc, final boolean visible )
			{
				final Class< ? > a = annotationDescriptorToAnnotation.get( desc );
				if ( a != null )
				{
					final Method m = new Method( mname, mdesc );
					annotatedMethods.add( m );
					annotationToAnnotatedMethods.get( a ).add( m );
				}
				return null;
			}
		};
	}

	public Set< Method > getAnnotatedMethods()
	{
		return annotatedMethods;
	}

	public Set< Method > getAnnotatedMethods( final Class< ? > annotation )
	{
		return annotationToAnnotatedMethods.get( annotation );
	}
}
