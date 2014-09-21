package neon.agent;

import java.util.ArrayList;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.LocalVariableNode;

public class InnerClassTemplate
{
	private final ClassNode cn;

	private final ArrayList< FieldInsnNode > templateFieldInsnNodes;

	private final ArrayList< LocalVariableNode > templateLocalVariableNodes;

	private final String impClassNameFormat;

	public InnerClassTemplate(
			final ClassNode cn,
			final ArrayList<FieldInsnNode> templateFieldInsnNodes,
			final ArrayList<LocalVariableNode> templateLocalVariableNodes,
			final String impClassNameFormat )
	{
		this.cn = cn;
		this.templateFieldInsnNodes = templateFieldInsnNodes;
		this.templateLocalVariableNodes = templateLocalVariableNodes;
		this.impClassNameFormat = impClassNameFormat;
	}

	public String getImpClassName( final int instanceIndex )
	{
		return String.format( impClassNameFormat, instanceIndex );
	}

	public String getInterfaceName()
	{
		return ( String ) cn.interfaces.get( 0 );
	}

	public ClassBytes instantiate( final int instanceIndex )
	{
		final String impClassName = String.format( impClassNameFormat, instanceIndex );
		final Type impClassType = Type.getObjectType( impClassName );
		final String impClassDescriptor = impClassType.getDescriptor();
		final String impClassBinaryName = impClassType.getClassName();

		cn.name = impClassName;

		if ( templateLocalVariableNodes != null )
			for ( final LocalVariableNode n : templateLocalVariableNodes )
				n.desc = impClassDescriptor;

		if (templateFieldInsnNodes != null )
			for ( final FieldInsnNode n : templateFieldInsnNodes )
				n.owner = impClassName;

		final ClassWriter cw = new ClassWriter( 0 );
		cn.accept( cw );
		cw.visitEnd();
		return new ClassBytes( impClassBinaryName, cw.toByteArray() );
	}
}
