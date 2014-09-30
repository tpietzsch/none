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
