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
