package org.oakinger.concordion;

import org.agileinsider.concordion.ConcordionPlusExtension;
import org.agileinsider.concordion.MatrixExtension;
import org.agileinsider.concordion.ScenarioExtension;
import org.agileinsider.concordion.command.MatrixCommand;
import org.agileinsider.concordion.command.ScenarioResultRecorder;
import org.agileinsider.concordion.event.*;
import org.agileinsider.concordion.junit.NotifierFactory;
import org.agileinsider.concordion.render.ScenarioResultRenderer;
import org.concordion.Concordion;
import org.concordion.api.*;
import org.concordion.api.listener.*;
import org.concordion.internal.ConcordionBuilder;
import org.concordion.internal.OgnlEvaluator;
import org.concordion.internal.OgnlEvaluatorFactory;
import org.concordion.internal.extension.FixtureExtensionLoader;
import org.concordion.internal.listener.AssertResultRenderer;
import org.concordion.internal.util.Announcer;
import org.concordion.internal.util.IOUtil;
import org.junit.ComparisonFailure;
import org.junit.internal.runners.model.EachTestNotifier;
import org.junit.runner.Description;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.MultipleFailureException;
import org.junit.runners.model.Statement;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Base class used for Concordion and Spring unit integration. All concordion class should be run using this class in
 * the @RunWith annotation.
 */
public class ConcordionPlusSpringJUnit4ClassRunner extends SpringJUnit4ClassRunner
{
    private final Description fixtureDescription;
    private final Class<?> fixtureClass;

    public ConcordionPlusSpringJUnit4ClassRunner(Class<?> fixtureClass) throws InitializationError, NoSuchMethodException
    {
        super( fixtureClass );
        this.fixtureClass = fixtureClass;
//        String testDescription =  ( "[Concordion Specification for '" + fixtureClass.getSimpleName() ).replaceAll( "Test$", "']" );
        String testDescription =  ( fixtureClass.getSimpleName() ).replaceAll( "Test$", "" );
        fixtureDescription = Description.createTestDescription( fixtureClass, testDescription );
    }

    @Override public Description getDescription()
    {
        return fixtureDescription;
    }

    @SuppressWarnings("deprecation")
    @Override protected void validateInstanceMethods(List<Throwable> errors)
    {
        // suppress no-methods check
    }

    @Override
    protected List<FrameworkMethod> computeTestMethods()
    {
        throw new AssertionError("must never be reached");
    }

    @Override protected List<FrameworkMethod> getChildren()
    {
        throw new AssertionError("must never be reached");
    }

    @Override
    protected Description describeChild( FrameworkMethod method )
    {
        throw new AssertionError("must never be reached");
    }

    @Override
    protected void runChild(FrameworkMethod method, RunNotifier notifier) {
        throw new AssertionError("must never be reached");
    }

    private static class ScenarioFrameworkMethod extends FrameworkMethod
    {
        private final Class<?> fixtureClass;
        private final ScenarioRunnable scenario;

        private static Method assertNotNull(Method method)
        {
            if(method==null) throw new IllegalArgumentException("method"); return method;
        }

        private static Method resolveScenarioMethod(Class<?> fixtureClass, ScenarioRunnable scenario)
        {
            Method foundMethod = null;

            for(Method method : fixtureClass.getMethods()) {
                ScenarioAnnotations scenarioMetaData = method.getAnnotation(ScenarioAnnotations.class);
                if (null != scenarioMetaData) {
                    String name = scenarioMetaData.name();
                    String scenarioName = scenario.getName();
                    if (name.equalsIgnoreCase(scenarioName)) {
                        return method; // exact match
                    }
                    if (name.equals("")) {
                        foundMethod = method;
                    }
                }
            }
            return foundMethod;
        }

        private ScenarioFrameworkMethod(Class<?> fixtureClass, ScenarioRunnable scenario)
        {
            super(resolveScenarioMethod(fixtureClass, scenario));
            this.fixtureClass = fixtureClass;
            this.scenario = scenario;
        }

        @Override public String getName()
        {
            return scenario.getName();
        }


        @Override
        public Method getMethod() {
            Method m = super.getMethod();
            if (m == null) {
                m = fixtureClass.getMethods()[0];
            }
            return m;
        }

        @Override public <T extends Annotation> T getAnnotation(Class<T> annotationType)
        {
            if (super.getMethod() == null) {
                return fixtureClass.getAnnotation(annotationType);
            }
            return super.getAnnotation(annotationType);
        }

        @Override public Annotation[] getAnnotations()
        {
            if (super.getMethod() == null) {
                return fixtureClass.getAnnotations();
            }
            return super.getAnnotations();
        }

        @Override public Object invokeExplosively(Object target, Object... params) throws Throwable
        {
            scenario.run(target);
            return null;
        }
    }

    @Override protected Statement childrenInvoker(final RunNotifier notifier)
    {
        return new Statement() {
            @Override public void evaluate() throws Throwable
            {
                final Object fixture = createTest();
                ScenarioRunner scenarioRunner = new ScenarioRunner() {
                    @Override public void invoke(ScenarioRunnable scenarioRunnable) throws Throwable
                    {
                        methodBlock(new ScenarioFrameworkMethod(fixture.getClass(), scenarioRunnable)).evaluate();
                    }
                };
                Concordion concordion = new ConcordionPlusBuilder(fixture, fixtureClass, notifier, scenarioRunner).build();
                ResultSummary resultSummary = concordion.process(fixture);
                resultSummary.print(System.out, fixture);
                resultSummary.assertIsSatisfied(fixture);
            }
        };
    }

    public static interface ScenarioRunnable
    {
        String getName();
        void run(Object target) throws Throwable;
    }

    public static interface ScenarioRunner
    {
        void invoke(ScenarioRunnable scenarioRunnable) throws Throwable;
    }

    public static class ConcordionPlusBuilder extends ConcordionBuilder
    {
        private final FixtureExtensionLoader fixtureExtensionLoader = new FixtureExtensionLoader();
        private final Object fixture;
        private final Class<?> fixtureClass;
        private final RunNotifier notifier;
        private final ScenarioRunner scenarioRunner;

        public ConcordionPlusBuilder(Object fixture, Class<?> fixtureClass, RunNotifier notifier, ScenarioRunner scenarioRunner)
        {
            this.fixture = fixture;
            this.fixtureClass = fixtureClass;
            this.notifier = notifier;
            this.scenarioRunner = scenarioRunner;
        }

        @Override
        public Concordion build()
        {
            final Class<?> fixtureClass = this.fixtureClass;

            withEvaluatorFactory( new OgnlEvaluatorFactory() );

            fixtureExtensionLoader.addExtensions(fixture, this);

            ScenarioRunnerCommand scenarioCommand = new ScenarioRunnerCommand(scenarioRunner);
            scenarioCommand.addScenarioListener(new ScenarioResultRenderer());
            final NotifierFactory notifierFactory = new NotifierFactory( null, null )
            {
                @Override public EachTestNotifier createNotifier(ScenarioEvent event)
                {
                    Description testDescription = Description.createTestDescription( fixtureClass, event.getScenarioName() );
                    return new EachTestNotifier( notifier, testDescription );
                }
            };
            final ScenarioNotifier scenarioNotifier = new ScenarioNotifier( notifierFactory ) {
            };
            scenarioCommand.addScenarioListener( scenarioNotifier );

            withAssertEqualsListener(scenarioCommand);
            withAssertFalseListener(scenarioCommand);
            withAssertTrueListener(scenarioCommand);
            withThrowableListener( scenarioCommand );

            withCommand( ConcordionPlusExtension.CONCORDION_PLUS_NAMESPACE,
                    ScenarioExtension.SCENARIO_COMMAND,
                    scenarioCommand );

            MatrixCommand matrixCommand = new MatrixCommand();
            matrixCommand.addAssertEqualsListener(new AssertResultRenderer());
            withCommand( ConcordionPlusExtension.CONCORDION_PLUS_NAMESPACE,
                    MatrixExtension.MATRIX_COMMAND,
                    matrixCommand );

            String css = IOUtil.readResourceAsString( ConcordionPlusExtension.CONCORDION_PLUS_CSS );
            withEmbeddedCSS( css );

            return super.build();
        }
    }

    public static class ScenarioRunnerCommand extends AbstractCommand implements AssertTrueListener, AssertFalseListener,AssertEqualsListener,ThrowableCaughtListener
    {
        private final Announcer<ScenarioListener> listeners = Announcer.to(ScenarioListener.class);
        private final ScenarioRunner scenarioRunner;

        public ScenarioRunnerCommand(ScenarioRunner scenarioRunner) {
            this.scenarioRunner = scenarioRunner;
        }

        public void addScenarioListener(ScenarioListener listener) {
            listeners.addListener(listener);
        }

        @Override
        public void execute(final CommandCall commandCall, final Evaluator evaluator, final ResultRecorder resultRecorder) {
            try {
                invokeScenario(commandCall, evaluator, resultRecorder);
            } catch (RuntimeException throwable) {
                throw throwable;
            } catch (Throwable throwable) {
                throw new RuntimeException(throwable);
            }
        }

        private void invokeScenario(final CommandCall commandCall, final Evaluator evaluator, final ResultRecorder resultRecorder) throws Throwable
        {
            Element element = commandCall.getElement();
            String ignoreValue = element.getAttributeValue( ScenarioExtension.IGNORE_COMMAND, ConcordionPlusExtension.CONCORDION_PLUS_NAMESPACE);
            if (ignoreValue != null && !ignoreValue.isEmpty())
            {
                resultRecorder.record( Result.IGNORED);
                listeners.announce().ignoredReported(new ScenarioIgnoredEvent(commandCall.getExpression(), commandCall.getElement()));
                return;
            }

            final String scenarioName = commandCall.getExpression();
            listeners.announce().scenarioStarted(new ScenarioStartEvent(scenarioName));

            final ScenarioResultRecorder scenarioResultRecorder = new ScenarioResultRecorder();

            final ScenarioContext scenarioContext = new ScenarioContext();

            try {
                scenarioRunner.invoke(new ScenarioRunnable() {
                    @Override public String getName() { return scenarioName; }
                    @Override public void run(Object target) throws Exception {
                        // might execute on a different thread due to e.g. @Test(timeout=xxxx)
                        currentScenarioAnnouncerHolder.set( scenarioContext );
                        try {
                            OgnlEvaluator scenarioEvaluator = new OgnlEvaluator(target);
                            commandCall.getChildren().processSequentially( scenarioEvaluator, scenarioResultRecorder );
                        } finally {
                            currentScenarioAnnouncerHolder.remove();
                        }
                    }
                });
            } catch (Throwable throwable) {
                if (throwable instanceof MultipleFailureException) {
                    throwable = ((MultipleFailureException)throwable).getFailures().get(0);
                }
                scenarioContext.recordThrowable(new ThrowableCaughtEvent(throwable, new Element("span").appendText("error in scenario '" + scenarioName + "'"), "<unavailable>"));
                scenarioResultRecorder.record( Result.EXCEPTION );
            }

            if (scenarioResultRecorder.getExceptionCount() > 0) {
                listeners.announce().scenarioError(new ScenarioErrorEventEx(scenarioName, scenarioContext.getError())); // new RuntimeException("Scenario '"+scenarioName+"' has errors.")
                resultRecorder.record(Result.EXCEPTION);
            } else if (scenarioResultRecorder.getFailureCount() > 0) {
                listeners.announce().failureReported(new ScenarioFailureEventEx(scenarioName, scenarioContext.getFailure()));
                resultRecorder.record(Result.FAILURE);
            } else {
                listeners.announce().successReported(new ScenarioSuccessEvent(scenarioName, element));
                resultRecorder.record(Result.SUCCESS);
            }

            listeners.announce().scenarioFinished(new ScenarioFinishEvent(scenarioName));
        }

        static class ScenarioContext {
            List<ThrowableCaughtEvent> throwableCaughtEvents = new ArrayList<ThrowableCaughtEvent>();
            List<AssertFailureEvent> assertFailureEvents = new ArrayList<AssertFailureEvent>();

            void recordThrowable(ThrowableCaughtEvent ev) { throwableCaughtEvents.add(ev); }
            void recordAssertFailure(AssertFailureEvent ev) { assertFailureEvents.add(ev); }

            ThrowableCaughtEvent getError() {
                return throwableCaughtEvents.get( 0 );
            }

            AssertFailureEvent getFailure() {
                return assertFailureEvents.get(0);
            }
        }

        ThreadLocal<ScenarioContext> currentScenarioAnnouncerHolder = new ThreadLocal<ScenarioContext>();

        @Override public void successReported(AssertSuccessEvent event)
        {
        }

        @Override public void throwableCaught(ThrowableCaughtEvent event)
        {
            ScenarioContext ctx = currentScenarioAnnouncerHolder.get();
            ctx.recordThrowable(event);
        }

        @Override public void failureReported(AssertFailureEvent event)
        {
            ScenarioContext ctx = currentScenarioAnnouncerHolder.get();
            ctx.recordAssertFailure( event );
        }
    }

    public static class ScenarioFailureEventEx extends ScenarioFailureEvent  {
        private AssertFailureEvent event;

        public ScenarioFailureEventEx(String name, AssertFailureEvent event) {
            super(name, event.getElement());
            this.event = event;
        }

        public AssertFailureEvent getEvent()
        {
            return event;
        }
    }

    public static class ScenarioErrorEventEx extends ScenarioErrorEvent  {
        private ThrowableCaughtEvent event;

        public ScenarioErrorEventEx(String name, ThrowableCaughtEvent event) {
            super(name, event.getElement(), event.getThrowable());
            this.event = event;
        }

        public ThrowableCaughtEvent getEvent()
        {
            return event;
        }
    }

    public static class ScenarioNotifier implements ScenarioListener {
        private EachTestNotifier scenarioEventNotifier;
        private final NotifierFactory notifierFactory;

        public ScenarioNotifier(NotifierFactory notifierFactory) {
            this.notifierFactory = notifierFactory;
        }

        public void successReported(ScenarioSuccessEvent event) {
            scenarioEventNotifier.fireTestFinished();
        }

        public void scenarioError(ScenarioErrorEvent event) {
            ScenarioErrorEventEx evex = (ScenarioErrorEventEx) event;
            String msg = "Error in element \n\t" + event.getElement().toXML();
            scenarioEventNotifier.addFailure(new Error(msg, evex.getThrowable()));
        }

        public void failureReported(ScenarioFailureEvent event) {
            ScenarioFailureEventEx evex = (ScenarioFailureEventEx) event;
            String msg = "Error in element \n\t" + event.getElement().toXML();
            scenarioEventNotifier.addFailure(new ComparisonFailure(msg, String.valueOf(evex.getEvent().getExpected()), String.valueOf( evex.getEvent().getActual() )));
        }

        public void scenarioStarted(ScenarioStartEvent event) {
            scenarioEventNotifier = notifierFactory.createNotifier(event);
            scenarioEventNotifier.fireTestStarted();
        }

        public void scenarioFinished(ScenarioFinishEvent scenarioFinishEvent) {
            scenarioEventNotifier = null;
        }

        public void ignoredReported(ScenarioIgnoredEvent event) {
            EachTestNotifier testNotifier = notifierFactory.createNotifier(event);
            testNotifier.fireTestIgnored();
        }

        public void throwableCaught(ThrowableCaughtEvent event) {
            scenarioEventNotifier.addFailure(event.getThrowable());
        }
    }
}