package neon.agent;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import java.util.Arrays;

import neon.annotation.Instantiate;

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

					// wrap writer with adapters for rewriting neon annotations
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
