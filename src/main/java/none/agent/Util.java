/*
 * #%L
 * none: runtime code modification  by annotation idioms.
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
package none.agent;

import java.util.Iterator;
import java.util.List;

import none.annotation.Instantiate;

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
