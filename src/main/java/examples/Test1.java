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
