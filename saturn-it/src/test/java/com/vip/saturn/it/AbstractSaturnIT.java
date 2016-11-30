package com.vip.saturn.it;

import org.junit.AfterClass;
import org.junit.BeforeClass;

import java.io.IOException;

/**
 * IT flow
 * Created by xiaopeng.he on 2016/7/13.
 */
public class AbstractSaturnIT extends SaturnAutoBasic {

    @BeforeClass
    public static void beforeClass() throws Exception {
        initZK();
        initSysEnv();
    }

    @AfterClass
    public static void afterClass() throws IOException, InterruptedException {
        regCenter.close();
        nestedZkUtils.stopServer();
    }

}
