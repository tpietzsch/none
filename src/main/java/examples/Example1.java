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
package examples;

import java.util.Random;

/**
 * Example illustrating @Instantiate @ByTypeOf.
 * Try running it with {@code -javaagent:/path/to/none-1.0.0-SNAPSHOT.jar} and without.
 *
 * @author Tobias Pietzsch <tobias.pietzsch@gmail.com>
 */
public class Example1
{
	final int[] values;

	public Example1( final int size )
	{
		values = new int[ size ];

		final Random random = new Random();
		for ( int i = 0; i < size; ++i )
			values[ i ] = random.nextInt( Integer.MAX_VALUE );
	}

	public void foreach(final F f)
	{
		LoopInterface loop = Loop.createKeySpecific(f.getClass());
		loop.foreach(f, values);
	}


	public interface  LoopInterface {

		void foreach(final F f, int[] values);
	}

	public static class Loop implements LoopInterface {

		private static final ClassCopyProvider<LoopInterface> loopProvider =
				new ClassCopyProvider<LoopInterface>(Loop.class);

		public static LoopInterface createKeySpecific(Object key) {
			return loopProvider.createKeySpecific(key);
		}

		public void foreach(final F f, int[] values) {
			for ( final int i : values )
				f.apply( i );
		}
	}

	static class Sum implements F
	{
		private long sum = 0;

		@Override
		public void apply( final int i )
		{
			sum += i;
		}

		public long get()
		{
			return sum;
		}
	}

	static class Max implements F
	{
		private int max = 0;

		@Override
		public void apply( final int i )
		{
			if ( i > max )
				max = i;
		}

		public long get()
		{
			return max;
		}
	}

	static class Average implements F
	{
		private long sum = 0;

		private int n = 0;

		@Override
		public void apply( final int i )
		{
			sum += i;
			++n;
		}

		public long get()
		{
			return sum / n;
		}
	}

	public static void main( final String[] args )
	{
		final Example1 example1 = new Example1( 10000000 );

		long t0, t;

		for ( int i = 0; i < 3; ++i )
		{
			t0 = System.currentTimeMillis();
			final Sum sum = new Sum();
			example1.foreach( sum );
			t = System.currentTimeMillis() - t0;
			System.out.println( "Sum: " + t + "ms" );

			t0 = System.currentTimeMillis();
			final Max sum2 = new Max();
			example1.foreach( sum2 );
			t = System.currentTimeMillis() - t0;
			System.out.println( "Max: " + t + "ms" );

			t0 = System.currentTimeMillis();
			final Average sum3 = new Average();
			example1.foreach( sum3 );
			t = System.currentTimeMillis() - t0;
			System.out.println( "Average: " + t + "ms" );
		}
	}

}
