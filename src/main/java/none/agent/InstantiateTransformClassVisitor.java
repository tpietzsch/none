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

import java.util.Set;

import none.annotation.Instantiate;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.Method;
import org.objectweb.asm.tree.MethodNode;

/**
 * Rewrite methods with the {@link Instantiate @Instantiate} annotation.
 *
 * @author Tobias Pietzsch <tobias.pietzsch@gmail.com>
 */
public class InstantiateTransformClassVisitor extends ClassVisitor
{
	/**
	 * The methods that need to be transformed. This is set by the constructor.
	 */
	private final Set< Method > methodsToTransform;

	private int version;

	private String debug;

	private String source;

	private Type type;

	/**
	 * @param api
	 *            the ASM API version implemented by this visitor. Must be one
	 *            of {@link Opcodes#ASM4} or {@link Opcodes#ASM5}.
	 * @param cv
	 *            the visitor to which we will pass everything that is passed to
	 *            us (either as is, or transformed). Usually this is a
	 *            {@link ClassWriter}.
	 * @param methodsToTransform
	 *            methods that need to be transformed by this visitor.
	 */
	public InstantiateTransformClassVisitor( final int api, final ClassVisitor cv, final Set< Method > methodsToTransform )
	{
		super( api, cv );
		this.methodsToTransform = methodsToTransform;
	}

	@Override
	public void visit( final int version, final int access, final String name, final String signature, final String superName, final String[] interfaces )
	{
//		System.out.println( getClass().getSimpleName() + ".visit(" + version + ", " + access + ", " + name + ", " + signature + ", " + superName + ", " + interfaces + " )" );
		this.version = version;
		this.type = Type.getObjectType( name );
		super.visit( version, access, name, signature, superName, interfaces );
	}

	@Override
	public void visitSource( final String source, final String debug )
	{
//		System.out.println( getClass().getSimpleName() + ".visitSource(" + source + ", " + debug + " )" );
		this.source = source;
		this.debug = debug;
		super.visitSource( source, debug );
	}

	@Override
	public MethodVisitor visitMethod( final int access, final String name, final String desc, final String signature, final String[] exceptions )
	{
		/*
		 * mv is a MethodVisitor obtained from the ClassWriter cv.
		 *
		 * If we would simply return it, the mv.visitCode() etc. methods would
		 * be called, and the method would be written to the ClassWriter.
		 */
		final MethodVisitor mv = super.visitMethod( access, name, desc, signature, exceptions );

		if ( mv != null && methodsToTransform.contains( new Method( name, desc ) ) )
			/*
			 * For methods annotated with @Instantiate, we return a
			 * InstantiateMethodVisitor instead, that transforms the method and
			 * then passes it on to mv.
			 */
			return new InstantiateMethodVisitor( mv, access, name, desc, signature, exceptions );
		else
			return mv;
	}

	/**
	 * Running index for generated interfaces (multiple methods may need to be transformed.)
	 */
	private int interfaceIndex = 0;

	private class InstantiateMethodVisitor extends MethodVisitor
	{
		private final MethodVisitor next;

		public InstantiateMethodVisitor(
				final MethodVisitor next,
				final int access,
				final String name,
				final String desc,
				final String signature,
				final String[] exceptions )
		{
			super( InstantiateTransformClassVisitor.this.api, new MethodNode( access, name, desc, signature, exceptions ) );
			this.next = next;
		}

		@Override
		public void visitEnd()
		{
			super.visitEnd();
			final MethodNode mn = ( MethodNode ) mv;
//			System.out.println( getClass().getSimpleName() + ": transforming " + new Method( mn.name, mn.desc ) );
			try
			{
				InstantiateTransform.transform( mn, type, version, source, debug, interfaceIndex++ ).accept( next );;
			}
			catch ( final Exception e )
			{
				e.printStackTrace();
				throw new RuntimeException( e );
			}
		}
	}
}
