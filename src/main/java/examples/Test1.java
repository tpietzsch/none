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
package examples;

import neon.annotation.ByTypeOf;
import neon.annotation.Instantiate;

import org.objectweb.asm.Opcodes;

public class Test1 implements Opcodes
{
	public static void main( final String[] args )
	{
		final Test1 t = new Test1();
		t.b = 42;
		t.doSomethingSimple();
		t.doSomethingHarder();
		t.doSomethingCrazy( t, 42 );
		t.doSomethingCrazy( t, 42 );
		t.doSomethingCrazy( "a string", 44 );
		t.doSomethingCrazy( new Object(), 43 );
		t.doSomethingCrazy( "a different string", 45 );
	}

	int b = 0;

	@Instantiate
	private void doSomethingSimple()
	{
		System.out.println( "Test1.doSomethingSimple()" );
	}

	@Instantiate
	public void doSomethingHarder()
	{
		System.out.println( "Test1.doSomethingHarder()" );
		System.out.println( "local variable b = " + b );
	}

	@Instantiate
	public static boolean doSomethingCrazy( @ByTypeOf final Object o, final int i )
	{
		System.out.println( "Test1.doSomethingCrazy( " + o + ", " + i + " )" );
		return o == null;
	}

	///////////////////////////////////////////////////////////////////////
	// unused stuff, just for looking at bytecode and reference.
	///////////////////////////////////////////////////////////////////////

	public interface dyn1I
	{
		public boolean doSomethingCrazy( final Object o, final int i );
	}

	public static class dyn1C implements dyn1I
	{
		@Override
		public boolean doSomethingCrazy( final Object o, final int i )
		{
			System.out.println( "Test1.doSomethingCrazy( " + o + ", " + i + " )" );
			return o == null;
		}
	}
}
