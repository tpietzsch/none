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

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import java.util.Arrays;

import none.annotation.Instantiate;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

public class Agent
{
	public static final int ASM_API = Opcodes.ASM5;

	public static void premain( final String agentArgs, final Instrumentation instrumentation )
	{
		instrumentation.addTransformer( new ClassFileTransformer()
		{
			@Override
			public synchronized byte[] transform(
					final ClassLoader loader,
					final String className,
					final Class< ? > classBeingRedefined,
					final ProtectionDomain protectionDomain,
					final byte[] classfileBuffer )
			{
//				System.out.println( "Agent.premain(): transform " + className );

				if ( loader == null || className == null || className.startsWith( "dynameta.agent" ) )
					return null;

				try
				{
					final ClassReader reader = new ClassReader( classfileBuffer );

					final GetAnnotatedMethodsClassVisitor checker = new GetAnnotatedMethodsClassVisitor( ASM_API, Arrays.< Class< ? > >asList( Instantiate.class ) );
					reader.accept( checker, 0 );

//					System.out.println( "Agent.premain(): methods to transform: {" );
//					for ( final Method method : checker.getAnnotatedMethods() )
//					{
//						System.out.println( "  " + method );
//					}
//					System.out.println( "}" );

					if ( checker.getAnnotatedMethods().isEmpty() )
						return null;

					final ClassWriter writer = new ClassWriter( reader, 0 );

					ClassVisitor rewriter = writer;

					// wrap writer with adapters for rewriting none annotations
					// currently there is only @Instantiate
					if ( ! checker.getAnnotatedMethods( Instantiate.class ).isEmpty() )
						rewriter = new InstantiateTransformClassVisitor( ASM_API, writer, checker.getAnnotatedMethods( Instantiate.class ) );

					reader.accept( rewriter, 0 );

					final byte[] byteArray = writer.toByteArray();
//					new ClassReader( byteArray ).accept( new TraceClassVisitor( new PrintWriter( System.out ) ), 0 );
//					CheckClassAdapter.verify( new ClassReader( byteArray ), false, new PrintWriter( System.err ) );
					return byteArray;
				}
				catch ( final Throwable t )
				{
					t.printStackTrace();
					return null;
				}
			}
		}, true );
	}
}
