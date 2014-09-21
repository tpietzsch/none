package neon.agent;

import java.util.Iterator;
import java.util.List;

import neon.annotation.Instantiate;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.MethodNode;

public class Util
{
	/**
	 * Make a copy of the given {@link MethodNode}.
	 *
	 * @param m
	 *            {@link MethodNode} to copy.
	 * @return a copy of {@code m}
	 */
	public static MethodNode copy( final MethodNode m )
	{
		final int access = m.access;
		final String name = m.name;
		final String desc = m.desc;
		final String signature = m.signature;
		final String[] exceptions = ( ( List< String > ) m.exceptions ).toArray( new String[ 0 ] );

		final MethodNode c = new MethodNode( Agent.ASM_API, access, name, desc, signature, exceptions );
		m.accept( c );
		return c;
	}

	/**
	 * Remove a method annotation from the given {@link MethodNode} (if
	 * present).
	 *
	 * @param m
	 * @param annotation
	 * @param visible
	 */
	public static void removeMethodAnnotation( final MethodNode m, final Class< ? > annotation, final boolean visible )
	{
		final List< AnnotationNode > annotations = visible ? m.visibleAnnotations : m.invisibleAnnotations;
		if ( annotations == null )
			return;
		final String adesc = Type.getDescriptor( Instantiate.class );
		final Iterator< AnnotationNode > iter = annotations.iterator();
		while ( iter.hasNext() )
			if ( iter.next().desc.equals( adesc ) )
				iter.remove();
	}
}
