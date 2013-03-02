
ConcordionPlusSpringJUnit4ClassRunner
=====================================

Implements and demonstrates the use of a junit runner that integrates
Concordion+ (http://www.agileinsider.org/concordion-plus/) and
Spring-Test (http://static.springsource.org/spring/docs/current/spring-framework-reference/html/testing.html#integration-testing)

The runner comes with following features:

1) integrates Concordion+ with Spring-Test

you can annotate test classes the same way as normal spring-test classes


2) apply standard junit annotations to scenarios

to apply annotations like Spring's @Timed or junit's own @Test(timeout=1000) you can use @ScenarioAnnotations to mark
a method where the runner should obtain those annotations from:

    @ScenarioAnnotations
    @Test(timeout = 1000)
    @Transactional
    public void apply_annotations_to_all_scenarios() {}

this allows to use any annotations understood by JUnit or Spring


3) improves failure output

the original Concordion and Concordion+ runners don't output any useful stacktraces or error information (e.g. which
element in the concordion specification triggered the error). This runner improves that and instead of just

java.lang.AssertionError: Specification has failure(s). See output HTML for details.
	at org.concordion.internal.FixtureState$3.assertIsSatisfied(FixtureState.java:60)

you get

org.junit.ComparisonFailure: Error in element
	<span concordion:assertEquals="execute()" class="failure fail"><del class="expected">unexpected</del>
<ins class="actual">dummyBean</ins></span>
Expected :unexpected
Actual   :dummyBean
 <Click to see difference>
	at org.oakinger.concordion.ConcordionPlusSpringJUnit4ClassRunner$ScenarioNotifier.failureReported(ConcordionPlusSpringJUnit4ClassRunner.java:416)
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
	at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:39)
	at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:25)

4) Summary reporter

although there's a maven plugin http://bassman5.github.com/maven-concordion-reporting-plugin/ that generates
an index file of all individual concordion html reports, this plugin requires to run mvn:site.
"ConcordionSummaryReporter" can instead be used standalone, this project's pom demonstrates it's use with the ant plugin.


