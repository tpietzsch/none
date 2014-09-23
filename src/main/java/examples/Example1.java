package examples;

import java.util.Random;

import neon.annotation.ByTypeOf;
import neon.annotation.Instantiate;

/**
 * Example illustrating @Instantiate @ByTypeOf.
 * Try running it with {@code -javaagent:/path/to/neon-1.0.0-SNAPSHOT.jar} and without.
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

	static interface F
	{
		void apply( int i );
	}

	@Instantiate
	public void foreach( @ByTypeOf final F f )
	{
		for ( final int i : values )
			f.apply( i );
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
