package com.vip.saturn.demo.utils;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.AbstractXmlApplicationContext;
import org.springframework.core.io.Resource;

public class MySpringApplicationContext extends
        AbstractXmlApplicationContext {

    private Resource[] configResources;

    public MySpringApplicationContext(Resource[] configResources)
            throws BeansException {
        this(configResources, true, null, null);
    }

    public MySpringApplicationContext(Resource[] configResources, ClassLoader classLoader) throws BeansException {
        this(configResources, true, null, classLoader);
    }

    public MySpringApplicationContext(Resource[] configResources,
                                      boolean refresh, ApplicationContext parent, ClassLoader classLoader) throws BeansException {
        super(parent);
        if (classLoader != null) {
            this.setClassLoader(classLoader);
        }
        this.configResources = configResources;
        if (refresh) {
            refresh();
        }
        //注册关闭钩子
        registerShutdownHook();
    }

    protected Resource[] getConfigResources() {
        return this.configResources;
    }

}  

