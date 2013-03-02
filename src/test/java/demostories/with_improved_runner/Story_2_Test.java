package demostories.with_improved_runner;

import org.junit.runner.RunWith;
import org.oakinger.concordion.ConcordionPlusSpringJUnit4ClassRunner;
import org.springframework.test.context.ContextConfiguration;

@RunWith(ConcordionPlusSpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:/test-context.xml"})
public class Story_2_Test {
}
