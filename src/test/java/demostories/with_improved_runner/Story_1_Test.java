package demostories.with_improved_runner;

import org.junit.*;
import org.junit.runner.RunWith;
import org.oakinger.concordion.ScenarioAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ContextConfiguration;
import testmodel.DummyService;
import org.oakinger.concordion.ConcordionPlusSpringJUnit4ClassRunner;

@RunWith(ConcordionPlusSpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:/test-context.xml"})
public class Story_1_Test {

    @Autowired
    @Qualifier("dummyBean")
    DummyService dummyService;

    // use @ScenarioAnnotations annotation to mark the method where the runner should obtain standard junit
    // annotations to apply to scenarios
    @ScenarioAnnotations
    @Test(timeout = 1000)
    public void apply_annotations_to_all_scenarios() {}

    private static int beforeClassCalled;

    public static int getBeforeClassCalled() {
        return beforeClassCalled;
    }

    private int beforeCalled;

    public Story_1_Test() {
        beforeCalled = 0;
    }

    public int isBeforeCalled() {
        return beforeCalled;
    }

    @Before
    public void before() {
        System.out.println("before scenario");
        beforeCalled += 1;
    }

    @After
    public void after() {
        System.out.println("after scenario");
    }

    @BeforeClass
    public static void beforeStory() {
        System.out.println("before story");
        beforeClassCalled += 1;
    }

    @AfterClass
    public static void afterClass() {
        System.out.println("after story");
    }

    public String execute() {
        return dummyService.getBeanName();
    }

    public String executeWithDelay(long delay) throws Exception {
        Thread.sleep(delay);
        return dummyService.getBeanName();
    }
}
