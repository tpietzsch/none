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

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import none.annotation.ByTypeOf;
import none.annotation.Instantiate;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.commons.Method;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

public class InstantiateTransform implements Opcodes
{
	public static MethodNode transform( final MethodNode mn, final Type type, final int version, final String source, final String debug, final int interfaceIndex )
			throws IllegalAccessException, IllegalArgumentException, InvocationTargetException
	{
		Util.removeMethodAnnotation( mn, Instantiate.class, false );
		final Class< ? > interfaceClass = createInstatiateInterfaceBytes( mn, type, version, interfaceIndex ).load();
		final InnerClassTemplate template = InstantiateTransform.createInstantiatePrototypeTemplate( mn, type, version, interfaceIndex, source, debug );
		RuntimeBuilder.add( interfaceClass, template );
		return InstantiateTransform.transformToInstantiatorMethod( mn, template );
	}

	static final String interfaceNameFormat = "%s$__none__I%d";

	static final String impClassNameFormatFormat = "%s$__none__I%dC%%d";

	/**
	 * Create an interface containing one method of the same name, signature,
	 * and return type as m.
	 *
	 * The interface is made an inner class of <em>className</em>, named "
	 * <em>className</em>$__none__I<em>interfaceIndex</em>".
	 *
	 * @param m
	 *            the method to be rewritten.
	 * @param outerClassName
	 *            internal name of the outer class
	 * @param classVersion
	 *            bytecode version of the outer class. We will use the same
	 *            version for generated code.
	 * @param interfaceIndex
	 *            The outer class might have several methods that need to be
	 *            rewritten. This index is used to make different names for the
	 *            generated interface.
	 */
	public static ClassBytes createInstatiateInterfaceBytes( final MethodNode m, final Type outerClassType, final int classVersion, final int interfaceIndex )
	{
		final String interfaceName = String.format( interfaceNameFormat, outerClassType.getInternalName(), interfaceIndex );

		final String name = m.name;
		final String desc = m.desc;
		final String signature = m.signature;
		final String[] exceptions = ( String[] ) m.exceptions.toArray( new String[ 0 ] );

		final ClassWriter cw = new ClassWriter( 0 );
		cw.visit( classVersion, ACC_PUBLIC + ACC_ABSTRACT + ACC_INTERFACE, interfaceName, null, "java/lang/Object", null );
		cw.visitMethod( ACC_PUBLIC + ACC_ABSTRACT, name, desc, signature, exceptions ).visitEnd();
		cw.visitEnd();

//		System.out.println( "createInstatiateInterfaceBytes: interfaceName = " + interfaceName );
//		System.out.println( "createInstatiateInterfaceBytes: Type.getObjectType( interfaceName ).getClassName() = " + Type.getObjectType( interfaceName ).getClassName() );
		return new ClassBytes( Type.getObjectType( interfaceName ).getClassName(), cw.toByteArray() );
	}

	/**
	 * Create a {@link InnerClassTemplate} containing a default constructor and
	 * implementing the interface generated by
	 * {@link #createInstatiateInterfaceBytes(MethodNode, Type, int, int)}. The
	 * interface is implemented using the code from the method {@code m}. If
	 * {@code m} is a non-static method, field and method accesses are rewritten
	 * to account for the fact that the code has moved to an inner class.
	 *
	 * @param m
	 *            the code, name, and description of the method to implement.
	 * @param outerClassType
	 *            the type of the outer class.
	 * @param classVersion
	 *            the class version of the outer class. This will be used for
	 *            the inner class as well.
	 * @param interfaceIndex
	 *            the index of the generated interface (see
	 *            {@link #createInstatiateInterfaceBytes(MethodNode, Type, int, int)}
	 *            ).
	 * @param sourceFile
	 *            Source file of the outer class. This is set as the source file
	 *            for the inner class as well, such that debugging into the
	 *            rewritten method works.
	 * @param sourceDebug
	 * @return a template that can be
	 *         {@link InnerClassTemplate#instantiate(int) instantiated} to
	 *         generate new implementations of the
	 *         {@link #createInstatiateInterfaceBytes(MethodNode, Type, int, int)
	 *         generated interface.}
	 */
	public static InnerClassTemplate createInstantiatePrototypeTemplate(
					MethodNode m,
					final Type outerClassType,
					final int classVersion,
					final int interfaceIndex,
					final String sourceFile,
					final String sourceDebug )
	{
		m = Util.copy( m );

		final boolean isStatic = ( ( m.access & ACC_STATIC ) != 0 );
		final String interfaceName = String.format( interfaceNameFormat, outerClassType.getInternalName(), interfaceIndex );

		final ArrayList<FieldInsnNode> templateFieldInsnNodes;
		final ArrayList<LocalVariableNode> templateLocalVariableNodes = new ArrayList< LocalVariableNode >();
		if ( isStatic )
		{
			templateFieldInsnNodes = null;
			transformStaticToInnerClassMethod( m, outerClassType, templateLocalVariableNodes );
		}
		else
		{
			templateFieldInsnNodes = new ArrayList< FieldInsnNode >();
			transformToInnerClassMethod( m, outerClassType, templateFieldInsnNodes );
		}

		final ClassNode cn = new ClassNode( Agent.ASM_API );
		cn.visit( classVersion, ACC_PUBLIC + ACC_SUPER, "", null, "java/lang/Object", new String[] { interfaceName } );
		cn.visitSource( sourceFile, sourceDebug );
		cn.visitField( ACC_FINAL + ACC_SYNTHETIC, "this$0", outerClassType.getDescriptor(), null, null ).visitEnd();

		final MethodNode mConstructor;
		if ( isStatic )
		{
	        mConstructor = new MethodNode( ACC_PUBLIC, "<init>", Type.getMethodDescriptor( Type.VOID_TYPE ), null, null );
			final LabelNode l0 = new LabelNode();
			mConstructor.instructions.add( l0 );
			mConstructor.visitVarInsn( ALOAD, 0 );
			mConstructor.visitMethodInsn( INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false );
			mConstructor.visitInsn( RETURN );
			final LabelNode l1 = new LabelNode();
			mConstructor.instructions.add( l1 );

			mConstructor.visitMaxs( 1, 1 );
		}
		else
		{
	        mConstructor = new MethodNode( ACC_PUBLIC, "<init>", Type.getMethodDescriptor( Type.VOID_TYPE, outerClassType ), null, null );
			final LabelNode l0 = new LabelNode();
			mConstructor.instructions.add( l0 );
			mConstructor.visitVarInsn( ALOAD, 0 );
			mConstructor.visitVarInsn( ALOAD, 1 );

			final FieldInsnNode n = new FieldInsnNode( PUTFIELD, "", "this$0", outerClassType.getDescriptor() );
			templateFieldInsnNodes.add( n );
			mConstructor.instructions.add( n );

			mConstructor.visitVarInsn( ALOAD, 0 );
			mConstructor.visitMethodInsn( INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false );
			mConstructor.visitInsn( RETURN );
			final LabelNode l1 = new LabelNode();
			mConstructor.instructions.add( l1 );

			final LocalVariableNode lv = new LocalVariableNode( "this", "", null, l0, l1, 0 );
			templateLocalVariableNodes.add( lv );
			mConstructor.localVariables.add( lv );

			mConstructor.visitMaxs( 2, 2 );
		}

		cn.methods.add( mConstructor );
		cn.methods.add( m );

		final String impClassNameFormat = String.format( impClassNameFormatFormat, outerClassType.getInternalName(), interfaceIndex );
		return new InnerClassTemplate( cn, templateFieldInsnNodes, templateLocalVariableNodes, impClassNameFormat );
	}

	/**
	 * Transform a non-static {@link MethodNode} to make the method a method of an inner
	 * class. All references to {@code this} in the method are rewritten to @{code this$0}.
	 * The method access is set to public.
	 *
	 * @param m
	 *            method to transform.
	 * @param outerClassType
	 *            the type of the outer class.
	 * @param templateFieldInsnNodes
	 *            a list of {@link FieldInsnNode} in the code. When putting the
	 *            method into a concrete inner class, the
	 *            {@link FieldInsnNode#owner} field of these nodes must be set
	 *            to the concrete inner class.
	 */
	private static void transformToInnerClassMethod(
			final MethodNode m,
			final Type outerClassType,
			final ArrayList<FieldInsnNode> templateFieldInsnNodes )
	{
		m.access |= ACC_PUBLIC;
		m.access &= ~( ACC_PROTECTED | ACC_PRIVATE );

		// after each "ALOAD 0", append a "GETFIELD this$0".
		// This replaces the "this" on the stack by "this$0" which is required because we are in a inner class.
		final ListIterator< AbstractInsnNode > iter = m.instructions.iterator();
		while( iter.hasNext() )
		{
			final AbstractInsnNode insn = iter.next();
			if ( insn instanceof VarInsnNode )
			{
				final VarInsnNode varinsn = ( VarInsnNode ) insn;
				if( varinsn.getOpcode() == ALOAD && varinsn.var == 0 )
				{
					final FieldInsnNode n = new FieldInsnNode( GETFIELD, "", "this$0", outerClassType.getDescriptor() );
					templateFieldInsnNodes.add( n );
					iter.add( n );
				}
			}
		}
	}

	/**
	 * Transform a static {@link MethodNode} to make the method a method of an
	 * inner class. The static method is made into a non-static method. A
	 * {@code this} field is inserted as local variable 0. The the local
	 * variable index of all variables and all variable access instructions is
	 * incemented by 1 to account for the {@code this} field. The method access
	 * is set to public.
	 *
	 * @param m
	 *            method to transform.
	 * @param outerClassType
	 *            the type of the outer class.
	 * @param templateLocalVariableNodes
	 *            a list of {@link LocalVariableNode} in the code. When putting
	 *            the method into a concrete inner class, the
	 *            {@link LocalVariableNode#desc} field of these nodes must be
	 *            set to the descriptor of the concrete inner class.
	 */
	private static void transformStaticToInnerClassMethod(
			final MethodNode m,
			final Type outerClassType,
			final ArrayList<LocalVariableNode> templateLocalVariableNodes )
	{
		m.access |= ACC_PUBLIC;
		m.access &= ~( ACC_PROTECTED | ACC_PRIVATE | ACC_STATIC );

		// shift the local variable index of all variable access instructions by +1.
		// this accounts for the "this" field we need to insert.
		final ListIterator< AbstractInsnNode > iter = m.instructions.iterator();
		while( iter.hasNext() )
		{
			final AbstractInsnNode insn = iter.next();
			if ( insn instanceof VarInsnNode )
			{
				final VarInsnNode varinsn = ( VarInsnNode ) insn;
				varinsn.var += 1;
			}
		}

		// shift the local variable index of all local variables by +1.
		// this accounts for the "this" field we need to insert.
		final Iterator< LocalVariableNode > viter = m.localVariables.iterator();
		while( viter.hasNext() )
			viter.next().index++;

		// insert "this" local variable at index 0
		final LocalVariableNode arg0 = ( LocalVariableNode ) m.localVariables.get( 0 );
		final LocalVariableNode vn = new LocalVariableNode( "this", "", null, arg0.start, arg0.end, 0 );
		templateLocalVariableNodes.add( vn );
		m.localVariables.add( 0, vn );

		m.maxLocals++;
	}

	/**
	 * Replace the code in {@code m} by a call to
	 * {@link RuntimeBuilder#getInnerClassInstance(Object, Object[])}, casting
	 * the result to the
	 * {@link #createInstatiateInterfaceBytes(MethodNode, Type, int, int)
	 * generated interface}, and calling the generated method.
	 *
	 * @param m
	 *            node to transform.
	 * @param template
	 *            use the {@link InnerClassTemplate#getInterfaceName()
	 *            interface} implemented by this {@link InnerClassTemplate}.
	 * @return the transformed node {@code m}.
	 */
	public static MethodNode transformToInstantiatorMethod( final MethodNode m, final InnerClassTemplate template )
	{
		System.out.println( InstantiateTransform.class.getSimpleName() + ".transformToInstantiatorMethod( " + m.name + " " + m.desc + " --> " + template.getInterfaceName() + " )" );

		final String interfaceName = template.getInterfaceName();
		final Type interfaceType = Type.getType( interfaceName );
		final Method interfaceMethod = new Method( m.name, m.desc );

		final boolean isStatic = ( ( m.access & ACC_STATIC ) != 0 );
		final Type[] argumentTypes = interfaceMethod.getArgumentTypes();
		final int numArguments = argumentTypes.length;
		final int argSize = ( Type.getArgumentsAndReturnSizes( interfaceMethod.getDescriptor() ) >> 2 ) - ( isStatic ? 1 : 0 );

		// find parameters annotated with @ByTypeOf
		final int[] byTypeOfArguments = new int[ numArguments ];
		int numByTypeOfArguments = 0;
		final List< AnnotationNode >[] invisibleParameterAnnotations = m.invisibleParameterAnnotations;
		if ( invisibleParameterAnnotations != null )
		{
			for ( int pi = 0; pi < invisibleParameterAnnotations.length; ++pi )
			{
				final List< AnnotationNode > alist = invisibleParameterAnnotations[ pi ];
				if ( alist != null )
				{
					final Iterator< AnnotationNode > iter = alist.iterator();
					while ( iter.hasNext() )
						if ( iter.next().desc.equals( Type.getDescriptor( ByTypeOf.class ) ) )
						{
							if ( argumentTypes[ pi ].getSort() == Type.OBJECT )
								byTypeOfArguments[ numByTypeOfArguments++ ] = pi;
							else
								System.err.println( "Ignored @ByTypeOf annotation. (Only supported on Object parameters)." );
						}
				}
			}
		}

		m.instructions.clear();
		final GeneratorAdapter g = new GeneratorAdapter( m, m.access, m.name, m.desc );

		final int __none__P__localVariableIndex = g.newLocal( Type.getType( Object[].class ) );
		final int __none__O__localVariableIndex = g.newLocal( interfaceType );

		final Label l0 = g.mark();

		// this creates Object[] __none__P which will contain parameters for switching implementation
		g.push( numByTypeOfArguments );
		g.newArray( Type.getType( Object.class ) );
		g.storeLocal( __none__P__localVariableIndex );
		final Label l1 = g.mark();

		// put the @ByTypeOf annotated parameters into Object[] __none__P array
		for ( int i = 0; i < numByTypeOfArguments; ++i )
		{
			g.loadLocal( __none__P__localVariableIndex );
			g.push( i );
			g.loadArg( byTypeOfArguments[ i ] );
			g.visitInsn( AASTORE );
		}

		// get a generated class instance appropriate for the concrete types in __none__P
		if ( isStatic )
		{
			// if m is a static method, invoke RuntimeBuilder.getInnerClassInstance(__none__P);
			g.push( Type.getObjectType( interfaceName ) );
			g.loadLocal( __none__P__localVariableIndex );
			g.invokeStatic( Type.getType( RuntimeBuilder.class ), new Method( "getInnerClassInstance", "(Ljava/lang/Class;[Ljava/lang/Object;)Ljava/lang/Object;" ) );
		}
		else
		{
			// if m is a non-static method, invoke RuntimeBuilder.getInnerClassInstance(this, __none__P);
			g.loadThis();
			g.push( Type.getObjectType( interfaceName ) );
			g.loadLocal( __none__P__localVariableIndex );
			g.invokeStatic( Type.getType( RuntimeBuilder.class ), new Method( "getInnerClassInstance", "(Ljava/lang/Object;Ljava/lang/Class;[Ljava/lang/Object;)Ljava/lang/Object;" ) );
		}

		// cast to generated interface type and store in local variable __none__O
		g.checkCast( interfaceType );
		g.storeLocal( __none__O__localVariableIndex );
		final Label l2 = g.mark();

		// invoke __none__O.<methodName>(...), which is basically a copy of the
		// code that was originally contained in m.
		g.loadLocal( __none__O__localVariableIndex );
		g.loadArgs();
		g.invokeInterface( interfaceType, interfaceMethod );
		g.returnValue();
		final Label l3 = g.mark();

		g.endMethod();
		g.visitMaxs( Math.max( 3, argSize + ( isStatic ? 1 : 0 ) ), 0 );
		g.visitEnd();

		// for existing local variables, set begin and end to l0 and l3
		final Iterator< LocalVariableNode > vi = m.localVariables.iterator();
		m.localVariables = new ArrayList< LocalVariableNode >();
		for ( int i = 0; i < __none__P__localVariableIndex; ++i )
		{
			final LocalVariableNode v = vi.next();
			m.visitLocalVariable( v.name, v.desc, v.signature, l0, l3, i );
		}
		// create new local variables __none__P and __none__O
		m.visitLocalVariable( "__none__P", "[Ljava/lang/Object;", null, l1, l3, __none__P__localVariableIndex );
		m.visitLocalVariable( "__none__O", Type.getObjectType( interfaceName ).getDescriptor(), null, l2, l3, __none__O__localVariableIndex );

		return m;
	}
}
