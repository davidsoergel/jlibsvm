= Efficient training of Support Vector Machines in Java =

 * Heavily refactored Java port of the venerable [http://www.csie.ntu.edu.tw/~cjlin/libsvm/ LIBSVM] (version 2.88).
 * Provides '''idiomatic Java''' class structure and APIs (unlike the Java version provided by LIBSVM, which is transliterated C code).
 * Easy to '''add new kernels''', in addition to the five standard ones provided by LIBSVM.
 * On the mathematical side, jlibsvm performs '''exactly the same computations as LIBSVM''', including shrinking and all the fancy stuff described in the [http://www.csie.ntu.edu.tw/~cjlin/papers/libsvm.pdf LIBSVM implementation docs].
 * '''Optimized kernel implementations''' run faster, particularly when input vectors are sparse.  For instance, on the [http://www.csie.ntu.edu.tw/~cjlin/libsvmtools/datasets/binary.html#mushrooms mushrooms] dataset, jlibsvm trained ~25% faster than LIBSVM (java version) with an RBF kernel and ~40% faster with a linear kernel.  (The C version of LIBSVM is still faster, though).
 * '''Multithreaded training''' to take advantage of modern multi-core machines (using [http://dev.davidsoergel.com/trac/conja/ Conja]).
 * '''Integrated scaling and normalization''' so you don't have to explicitly preprocess your data.
 * '''Integrated grid search''' for optimal kernel parameters.
 * Drop-in replacement if you use the command-line tools (e.g. svm-train, etc.), but not if you use LIBSVM programmatically.
 * Uses Java '''generics''' throughout, including for classification labels, so you can specify that the "label" of a class be of whatever Java type you like.  In an email-filtering application, for example, you could use objects of type {{{Mailbox}}} as the labels.  That would allow you to write something like {{{mySvmSolutionModel.predict(incomingEmail).addMessage(incomingEmail)}}}.  The {{{predict()}}} method returns a classification label, which in this case is an object of class {{{Mailbox}}}, which has an {{{addMessage()}}} method.
 

== Status ==

This is beta code.  While LIBSVM is stable, it's possible that I broke something in the process of refactoring it.  I've done ad-hoc testing primarily with the C_SVC machine and an RBF kernel, and got results that were identical to LIBSVM as far as I could tell.  There are not (yet?) any unit tests.  I'm running some automated verifications that jlibsvm behaves identically to LIBSVM for a number of input datasets and parameter choices; results will be available here soon.  Please [mailto:dev@davidsoergel.com let me know] if you find a situation in which the two packages give different results.

== Documentation ==

 * [http://dev.davidsoergel.com/maven/jlibsvm/apidocs API docs] (jlibsvm only)
 * [http://dev.davidsoergel.com/apidocs Aggregate API docs] (all projects hosted here; useful for navigating cross-package dependencies)

Sorry, I haven't really had a chance to write any docs.  Have a look at the sources for the command-line programs in the [http://dev.davidsoergel.com/trac/jlibsvm/browser/trunk/src/main/java/edu/berkeley/compbio/jlibsvm/legacyexec legacyexec] package to see how jlibsvm gets called.  Very briefly, you'll need to:

 1. instantiate the [http://dev.davidsoergel.com/maven/jlibsvm/apidocs/edu/berkeley/compbio/jlibsvm/kernel/KernelFunction.html KernelFunction] that you want
 2. set up some parameters in a new [http://dev.davidsoergel.com/maven/jlibsvm/apidocs/edu/berkeley/compbio/jlibsvm/SvmParameter.html SvmParameter] object
 3. instantiate a concrete subclass of [http://dev.davidsoergel.com/maven/jlibsvm/apidocs/edu/berkeley/compbio/jlibsvm/SvmProblem.html SvmProblem] (binary, multiclass, or regression), and populate it with training data
 4. instantiate a concrete subclass of [http://dev.davidsoergel.com/maven/jlibsvm/apidocs/edu/berkeley/compbio/jlibsvm/SVM.html SVM], choosing a type appropriate for your problem
 5. Call {{{SVM.train(problem)}}} to yield a [http://dev.davidsoergel.com/maven/jlibsvm/apidocs/edu/berkeley/compbio/jlibsvm/SolutionModel.html SolutionModel], which can be used to make predictions

== Download ==

[http://maven.apache.org/ Maven] is by far the easiest way to make use of jlibsvm.  Just add these to your pom.xml:
{{{
<repositories>
	<repository>
		<id>dev.davidsoergel.com releases</id>
		<url>http://dev.davidsoergel.com/artifactory/repo</url>
		<snapshots>
			<enabled>false</enabled>
		</snapshots>
	</repository>
	<repository>
		<id>dev.davidsoergel.com snapshots</id>
		<url>http://dev.davidsoergel.com/artifactory/repo</url>
		<releases>
			<enabled>false</enabled>
		</releases>
	</repository>
</repositories>

<dependencies>
	<dependency>
		<groupId>edu.berkeley.compbio</groupId>
		<artifactId>jlibsvm</artifactId>
		<version>0.902</version>
	</dependency>
</dependencies>
}}}


If you don't use Maven, you can get the distribution (including all external dependencies) here: [http://dev.davidsoergel.com:80/artifactory/libs-releases/edu/berkeley/compbio/jlibsvm/0.902/jlibsvm-0.902.tar.gz jlibsvm-0.902.tar.gz] (2.7 MB)  ''December 14, 2009''

Or get the [http://dev.davidsoergel.com/hudson/job/jlibsvm/lastStableBuild/ latest stable build] from the continuous integration server.  (You may need to pick up the latest versions of the dependencies listed there, too.)

You can also [source:/ browse the source], or get the source with Mercurial:
{{{
hg clone https://hg.davidsoergel.com/davidsoergel/jlibsvm
}}}

The project is also [http://hg.davidsoergel.com/jlibsvm on BitBucket], so please feel free to fork it there.


== Support ==

 * [http://dev.davidsoergel.com/trac/jlibsvm/discussion/1 Discussion Forum]
 * Feel free to [mailto:dev@davidsoergel.com email me] any questions or comments
 * For bug reports or feature requests, please [http://dev.davidsoergel.com/trac/jlibsvm/newticket create a ticket] (you'll need to [http://dev.davidsoergel.com/trac/jlibsvm/register create an account] and [http://dev.davidsoergel.com/trac/jlibsvm/login login] first).
 * [report:1 view active tickets]