package org.litetokens.common.application;

import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.litetokens.core.db.Manager;

public class LitetokensApplicationContext extends AnnotationConfigApplicationContext {

    public LitetokensApplicationContext() {
    }

    public LitetokensApplicationContext(DefaultListableBeanFactory beanFactory) {
        super(beanFactory);
    }

    public LitetokensApplicationContext(Class<?>... annotatedClasses) {
        super(annotatedClasses);
    }

    public LitetokensApplicationContext(String... basePackages) {
        super(basePackages);
    }

    @Override
    public void destroy() {

        Manager dbManager = getBean(Manager.class);
        dbManager.stopRepushThread();

        super.destroy();
    }
}
