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
