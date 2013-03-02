package testmodel;

import org.springframework.beans.factory.BeanNameAware;

public class DummyService implements BeanNameAware {

    private String beanName;

    public String getBeanName() {
        return beanName;
    }

    @Override
    public void setBeanName(String name) {
        this.beanName = name;
    }
}
