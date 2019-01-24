package agents.MaastCTS2.libs.it.unimi.dsi.util;

/*		 
 * DSI utilities
 *
 * Copyright (C) 2011-2014 Sebastiano Vigna 
 *
 *  This library is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU Lesser General Public License as published by the Free
 *  Software Foundation; either version 3 of the License, or (at your option)
 *  any later version.
 *
 *  This library is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 *  for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses/>.
 *
 */

import java.util.Random;

import agents.MaastCTS2.libs.it.unimi.dsi.Util;
import agents.MaastCTS2.libs.it.unimi.dsi.fastutil.HashCommon;

/** A very fast, high-quality 64-bit {@linkplain Random pseudorandom number generator} 
 * that combines George Marsaglia's Xorshift
 * generators (described in <a href="http://www.jstatsoft.org/v08/i14/paper/">&ldquo;Xorshift RNGs&rdquo;</a>,
 * <i>Journal of Statistical Software</i>, 8:1&minus;6, 2003) with a multiplication.
 * 
 * <p>More details about <code>xorshift*</code> generators can be found in my paper &ldquo;<a href="http://vigna.di.unimi.it/papers.php#VigEEMXGS">An experimental exploration of Marsaglia's <code>xorshift</code> generators,
 * scrambled&rdquo;</a>, 2014.
 * 
 * <p>Note that this is <strong>not</strong> a cryptographic-strength
 * pseudorandom number generator, but its quality is preposterously higher than {@link Random}'s 
 * (and its period is 2<sup>64</sup>&nbsp;&minus;&nbsp;1, more than enough for 99% applications).
 * 
 * <p>On an Intel&reg; Core&trade; i7-4770 CPU @3.40GHz (Haswell), 
 * all methods of this class and of {@link XorShift128PlusRandom} except for {@link #nextInt()} are faster than 
 * those of <a href="http://docs.oracle.com/javase/7/docs/api/java/util/concurrent/ThreadLocalRandom.html"><code>ThreadLocalRandom</code></a> (available only from Java 7).
 * Timings are orders of magnitude faster than {@link Random}'s, but {@link Random} is slowed down by the
 * fact of being thread safe, so the comparison is not fair. 
 * 
 * <p>The following table reports timings
 * in nanoseconds for some type of calls for <a href="http://docs.oracle.com/javase/7/docs/api/java/util/concurrent/ThreadLocalRandom.html"><code>ThreadLocalRandom</code></a>, 
 * for {@link XorShift128PlusRandom} (the fastest), for this class, for {@link XorShift1024StarRandom} and for the <samp>xorgens</samp> generator
 * described by Richard P. Brent in &ldquo;Some long-period random number generators using shifts and xors&rdquo;, <i>ANZIAM Journal</i> 48 (CTAC2006), C188-C202, 2007:
 * 
 * <CENTER><TABLE BORDER=1>
 * <TR><TH>
 * <TH><a href="http://docs.oracle.com/javase/7/docs/api/java/util/concurrent/ThreadLocalRandom.html"><code>ThreadLocalRandom</code></a>
 * <TH>{@link XorShift128PlusRandom}
 * <TH>{@link XorShift64StarRandom}
 * <TH>{@link XorShift1024StarRandom}
 * <TH><samp>xorgens</samp>
 * <TR><TD>	nextInt()	<TD>1.33<TD>1.42<TD>1.70<TD>2.16<TD>2.57
 * <TR><TD>	nextLong()	<TD>2.72<TD>1.29<TD>1.62<TD>2.06<TD>2.32
 * <TR><TD>	nextDouble()	<TD>2.81<TD>2.06<TD>2.06<TD>2.13<TD>2.58
 * <TR><TD>	nextInt(1000000)	<TD>2.89<TD>2.74<TD>2.33<TD>3.25<TD>3.75
 * <TR><TD>	nextInt(2^29+2^28)	<TD>6.47<TD>2.85<TD>2.40<TD>3.49<TD>4.02
 * <TR><TD>	nextInt(2^30)	<TD>2.86<TD>2.34<TD>1.80<TD>2.68<TD>2.91
 * <TR><TD>	nextInt(2^30+1)	<TD>12.12<TD>2.82<TD>2.27<TD>3.38<TD>3.98
 * <TR><TD>	nextInt(2^30+2^29)	<TD>6.37<TD>2.85<TD>2.40<TD>3.49<TD>4.02
 * <TR><TD>	nextLong(1000000000000)	<TD>72.55<TD>2.59<TD>3.01<TD>3.18<TD>3.94
 * <TR><TD>	nextLong(2^62+1)	<TD>229.04<TD>12.59<TD>13.14<TD>15.06<TD>16.01
 * </TABLE></CENTER>
 * 
 * <p>Note that the relative differences between generators are actually more marked than these figures show,
 * as the timings include the execution of a loop and of a xor instruction that combines the results
 * to avoid excision. 
 * 
 * <p>The quality of this generator is high: for instance, it performs better than <samp>WELL1024a</samp> 
 * or <samp>MT19937</samp> in suites like  
 * <a href="http://www.iro.umontreal.ca/~simardr/testu01/tu01.html">TestU01</a> and
 * <a href="http://www.phy.duke.edu/~rgb/General/dieharder.php">Dieharder</a>. 
 * More details can be found on the <a href="http://xorshift.di.unimi.it/"><code>xorshift*</code> generators and the PRNG shootout</a> page.
 * 
 * <p>This class extends {@link Random}, overriding (as usual) the {@link Random#next(int)} method. Nonetheless,
 * since the generator is inherently 64-bit also {@link Random#nextInt()}, {@link Random#nextInt(int)},
 * {@link Random#nextLong()} and {@link Random#nextDouble()} have been overridden for speed (preserving, of course, {@link Random}'s semantics).
 * See in particular the comments in the documentation of {@link #nextInt(int)}, which is tailored for speed at the price of an essentially undetectable bias.
 * 
 * <p>If you do not need an instance of {@link Random}, or if you need a {@link RandomGenerator} to use
 * with <a href="http://commons.apache.org/math/">Commons Math</a>, you might be wanting
 * {@link XorShift64StarRandomGenerator} instead of this class.
 * 
 * <p>If you need a longer period, consider using {@link XorShift1024StarRandom} or {@link XorShift1024StarRandomGenerator}.
 * 
 * <h3>Notes</h3>
 * 
 * <p>The <em>lower bits</em> of this generator are of slightly better quality than the higher bits. Thus, masking the lower
 * bits is a safe and effective way to obtain small random numbers. The code in this class usually extracts
 * lower bits, rather than upper bits, whenever a subset of bits is necessary (when extracting 63 bits
 * we use a right shift for performance reasons, though).
 */
public class XorShift64StarRandom extends Random {
	private static final long serialVersionUID = 1L;

	/** 2<sup>53</sup> &minus; 1. */
	private static final long DOUBLE_MASK = ( 1L << 53 ) - 1;
	/** 2<sup>-53</sup>. */
	private static final double NORM_53 = 1. / ( 1L << 53 );
	/** 2<sup>24</sup> &minus; 1. */
	private static final long FLOAT_MASK = ( 1L << 24 ) - 1;
	/** 2<sup>-24</sup>. */
	private static final double NORM_24 = 1. / ( 1L << 24 );

	/** The internal state of the algorithm. */
	private long x;

	/** Creates a new generator seeded using {@link Util#randomSeed()}. */
	public XorShift64StarRandom() {
		this( Util.randomSeed() );
	}

	/** Creates a new generator using a given seed.
	 * 
	 * @param seed a nonzero seed for the generator (if zero, the generator will be seeded with -1).
	 */
	public XorShift64StarRandom( final long seed ) {
		super( seed );
	}

	@Override
	protected int next( int bits ) {
		return (int)( nextLong() & ( 1L << bits ) - 1 );
	}

	@Override
	public long nextLong() {
		x ^= x >>> 12;
		x ^= x << 25;
		return 2685821657736338717L * ( x ^= ( x >>> 27 ) );
	}

	@Override
	public int nextInt() {
		return (int)nextLong();
	}
	
	/** Returns a pseudorandom, approximately uniformly distributed {@code int} value
     * between 0 (inclusive) and the specified value (exclusive), drawn from
     * this random number generator's sequence.
     * 
     * <p>The hedge &ldquo;approximately&rdquo; is due to the fact that to be always
     * faster than <a href="http://docs.oracle.com/javase/7/docs/api/java/util/concurrent/ThreadLocalRandom.html"><code>ThreadLocalRandom</code></a>
     * we return
     * the upper 63 bits of {@link #nextLong()} modulo {@code n} instead of using
     * {@link Random}'s fancy algorithm (which {@link #nextLong(long)} uses though).
     * This choice introduces a bias: the numbers from 0 to 2<sup>63</sup> mod {@code n}
     * are slightly more likely than the other ones. In the worst case, &ldquo;more likely&rdquo;
     * means 1.00000000023 times more likely, which is in practice undetectable (actually,
     * due to the abysmally low quality of {@link Random}'s generator, the result is statistically  
     * better in any case than {@link Random#nextInt(int)}'s) . 
     * 
     * <p>If for some reason you need truly uniform generation, just use {@link #nextLong(long)}.
     * 
     * @param n the positive bound on the random number to be returned.
     * @return the next pseudorandom {@code int} value between {@code 0} (inclusive) and {@code n} (exclusive).
     */
	@Override
	public int nextInt( final int n ) {
        if ( n <= 0 ) throw new IllegalArgumentException();
		// No special provision for n power of two: all our bits are good.
        return (int)( ( nextLong() >>> 1 ) % n );
	}
	
	/** Returns a pseudorandom uniformly distributed {@code long} value
     * between 0 (inclusive) and the specified value (exclusive), drawn from
     * this random number generator's sequence. The algorithm used to generate
     * the value guarantees that the result is uniform, provided that the
     * sequence of 64-bit values produced by this generator is. 
     * 
     * @param n the positive bound on the random number to be returned.
     * @return the next pseudorandom {@code long} value between {@code 0} (inclusive) and {@code n} (exclusive).
     */
	public long nextLong( final long n ) {
        if ( n <= 0 ) throw new IllegalArgumentException();
		// No special provision for n power of two: all our bits are good.
		for(;;) {
			final long bits = nextLong() >>> 1;
			final long value = bits % n;
			if ( bits - value + ( n - 1 ) >= 0 ) return value;
		}
	}
	
	@Override
	 public double nextDouble() {
		return ( nextLong() & DOUBLE_MASK ) * NORM_53;
	}
	
	@Override
	public float nextFloat() {
		return (float)( ( nextLong() & FLOAT_MASK ) * NORM_24 );
	}

	@Override
	public boolean nextBoolean() {
		return ( nextLong() & 1 ) != 0;
	}
	
	@Override
	public void nextBytes( final byte[] bytes ) {
		int i = bytes.length, n = 0;
		while( i != 0 ) {
			n = Math.min( i, 8 );
			for ( long bits = nextLong(); n-- != 0; bits >>= 8 ) bytes[ --i ] = (byte)bits;
		}
	}


	/** Sets the seed of this generator.
	 * 
	 * <p>The seed will be passed through {@link HashCommon#murmurHash3(long)}. In this way, if the
	 * user passes a small value we will avoid the short irregular transient associated 
	 * with states with a very small number of bits set. 
	 * 
	 * @param seed a nonzero seed for this generator (if zero, the generator will be seeded with {@link Long#MIN_VALUE}).
	 */
	@Override
	public void setSeed( final long seed ) {
		x = HashCommon.murmurHash3( seed == 0 ? Long.MIN_VALUE : seed );
	}


	/** Sets the state of this generator.
	 * 
	 * @param state the new state for this generator (must be nonzero).
	 */
	public void setState( final long state ) {
		x = ( state == 0 ? -1 : state );
	}
}
