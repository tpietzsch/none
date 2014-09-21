package examples;

import neon.annotation.ByTypeOf;
import neon.annotation.Instantiate;
import net.imglib2.Cursor;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.basictypeaccess.array.FloatArray;
import net.imglib2.img.basictypeaccess.array.IntArray;
import net.imglib2.type.Type;
import net.imglib2.type.numeric.integer.IntType;
import net.imglib2.type.numeric.real.FloatType;


public class Test2
{
	@Instantiate
	public static < T extends Type< T > & Comparable< T > > T max(
			@ByTypeOf final T type,
			@ByTypeOf final Cursor< T > c )
	{
		final T max = type.createVariable();
		while ( c.hasNext() )
		{
			final T t = c.next();
			if ( t.compareTo( max ) > 0 )
				max.set( t );
		}
		return max;
	}

	public static void main( final String[] args )
	{
		final ArrayImg< FloatType, FloatArray > floats = ArrayImgs.floats( new float[] { 0, 1, 2, 3, 4123, 5, 6, 7, 8, 9 }, 10 );
		final ArrayImg< IntType, IntArray > ints = ArrayImgs.ints( new int[] { 0, 1, 2, 3, 4123, 5, 6, 7, 8, 9 }, 10 );

		final FloatType max = max( floats.firstElement(), floats.cursor() );
		final FloatType max2 = max( floats.firstElement(), floats.localizingCursor() );
		final IntType max3 = max( ints.firstElement(), ints.cursor() );

		System.out.println( max.get() );
		System.out.println( max2.get() );
		System.out.println( max3.get() );
	}
}
