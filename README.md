![logo](logo-small.png)

**None** is a runtime Java code modification tool. It implements code transformations that enable JIT optimization for certain difficult-to-optimize constructs.

**None** comprises a set of Java annotations and a Java agent that transforms annotated code. The transformation idioms are designed to be nonintrusive. *Importantly, annotated code will run with identical results, whether the Java agent is employed or not, making the use of __None__ optional at runtime.*

Idioms
======
To get any benefit out of the library, it is important to understand the occurrence of certain patterns that make life hard for the JIT. In these cases certain pieces of the code can be broken out into idiomatically annotated constructs.
Currently there is only one idiom implemented:

@Instantiate @ByTypeOf
----------------------
This applies when

* a polymorphic call occurs within a hot inner loops over many elements, and
* the actual runtime target of the polymorphic call is the same for all elements.

### Problem
To illustrate the problem, consider the following example. (See also [examples/Example1](src/main/java/examples/Example1.java)).

```java
public class Example1 {
	final int[] values;

	static interface F {
		void apply( int i );
	}

	public void foreach( final F f ) {
		for ( final int i : values )
			f.apply( i );
	}

	...
}
```

`Example1` has a (large) array of `int`s. It defines a `foreach()` method that takes a `F` and applies it to each of the array elements.

As an example take
```java
class Sum implements F {
	private long sum = 0;

	@Override
	public void apply( final int i ) {
		sum += i;
	}

	public long get() {
		return sum;
	}
}
```
which computes the sum of all array elements. Applying it for 3 times to an array of 10000000 elements
```java
for ( int i = 0; i < 3; ++i ) {
	t0 = System.currentTimeMillis();
	final Sum sum = new Sum();
	example1.foreach( sum );
	t = System.currentTimeMillis() - t0;
	System.out.println( "Sum: " + t + "ms" );
}
```
we obtain the following running times:
```
Sum: 8ms
Sum: 4ms
Sum: 3ms
```

After (actually during) the first run, the JIT detects that this is a hot loop and compiles it. Noting that the only `F` that was ever used in `foreach()` is `Sum`, the JIT optimistically inlines that, assuming that this is all that will ever occur.

However, if we add different `F`s, the JIT will revise its overly optimistic assumption,
making `F.apply()` a polymorphic call.
```java
for ( int i = 0; i < 3; ++i ) {
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
```
We observe the following running times:
```
Sum: 8ms
Max: 11ms
Average: 31ms
Sum: 4ms
Max: 6ms
Average: 29ms
Sum: 39ms
Max: 39ms
Average: 39ms
```
Now, what happens here is that the polymorphic call is resolved in each iteration of the inner loop in `foreach()` although for a single invocation of `F` the runtime type of `F` never changes! This affects performance considerably, slowing down `Sum` by a factor of more than 10.


### Solution
Using the idiom **@Instantiate @ByTypeOf**, this problem can be resolved. Add annotations to `foreach` as follows:
```java
@Instantiate public void foreach( @ByTypeOf final F f ) {
	for ( final int i : values )
		f.apply( i );
}

```
When running the program, use the *none* Java agent to rewrite the annotated code by specifying
```
java -javaagent:/path/to/none-1.0.0-SNAPSHOT.jar ...
```
We obtain the following running times:
```
Sum: 7ms
Max: 8ms
Average: 9ms
Sum: 3ms
Max: 5ms
Average: 4ms
Sum: 3ms
Max: 4ms
Average: 3ms

```
All cases are optimized by the JIT.

### Implementation
What happened?
The **none** java agent replaces the annotated method `foreach()` by roughly the bytecode of the following:
```java
static interface __none__I0
{
	public void foreach( final F f );
}

public void foreach( final F f )
{
	final __none__I0 i0 = ( __none__I0 )
			RuntimeBuilder.getInnerClassInstance(
					this,
					__none__I0.class,
					new Object[] { f } );
	i0.foreach( f );
}
```
The `RuntimeBuilder.getInnerClassInstance()` returns an object implementing `__none__I0.foreach()`. The last argument, `new Object[] { f }` is an object array containing all parameters annotated with `@ByTypeOf`, in this case `f`. Depending on the classes of these objects, new `__none__I0` implementations are generated on the fly. These implementations are made using the original bytecode of the `@Instantiated` method, transformed to accomodate the fact that they lives in an inner class now. (Both `__none__I0` and its implementations are inner classes of the class containing `@Instantiated` method).

Here is again a run of the above example, printing when new classes are generated:
```
ClassLoaderUtil.loadClass( examples.Example1$__none__I0 )
InstantiateTransform.transformToInstantiatorMethod( foreach (Lexamples/Example1$F;)V --> examples/Example1$__none__I0 )
ClassLoaderUtil.loadClass( examples.Example1$__none__I0C0 )
Sum: 7ms
ClassLoaderUtil.loadClass( examples.Example1$__none__I0C1 )
Max: 8ms
ClassLoaderUtil.loadClass( examples.Example1$__none__I0C2 )
Average: 8ms
Sum: 3ms
Max: 4ms
Average: 3ms
Sum: 4ms
Max: 4ms
Average: 4ms
```
So, when `Class<Sum>`, `Class<Max>`, and `Class<Average>` are first encountered, new inner classes `__none__I0C0`, `__none__I0C`, `__none__I0C3` are made. On subsequent occurrences these classes are re-used.


# Resources

**@Instantiate @ByTypeOf** has been heavily influenced by [Cliff Click's blog post](http://www.azulsystems.com/blog/cliff/2011-04-04-fixing-the-inlining-problem) describing essentially the exact problem and [this reply by Remi Forax](https://weblogs.java.net/blog/forax/archive/2011/04/08/fixing-inlining-“problem”-prototype) which has a prototype for addressing a flavour of the problem related to lamda calls. Unfortunately I'm unfamiliar with the workings of `java.lang.invoke` so his code is a bit beyond me. But I understood enough to see that ASM plus JavaAgent might be a solution.

In general for Java-performance related issues I found these blogs useful:
[Mechanical Sympathy](http://mechanical-sympathy.blogspot.de)
[Insightful Logic](http://www.insightfullogic.com/blog/)
[Psychosomatic, Lobotomy, Saw](http://psy-lob-saw.blogspot.de)
[Headius](http://blog.headius.com)

For hunting down JIT-related performance problems I found the JVM flags
`-XX:+PrintCompilation`
`-XX:+UnlockDiagnosticVMOptions -XX:+PrintInlining` (only useful starting from java 7)
`-XX:+UnlockDiagnosticVMOptions -XX:+LogCompilation`
extremely useful.
