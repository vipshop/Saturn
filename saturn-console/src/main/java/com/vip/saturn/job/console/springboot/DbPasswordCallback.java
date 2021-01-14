package com.vip.saturn.job.console.springboot;

import com.alibaba.druid.util.DruidPasswordCallback;
import com.vip.saturn.job.console.springboot.utils.RSAKeysUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

public class DbPasswordCallback extends DruidPasswordCallback {

    private static final Logger log = LoggerFactory.getLogger(DbPasswordCallback.class);

    /**
     * 私钥对数据进行解密
     */
    private static final String PRIVATE_KEY_STRING = "MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQCCrE/StGyQ/+YVtMA4G3YK2xsFuST4msH18nCgH+OzNkmbmR+9MQ5Pfx2dJXtSEwdcyNPp5UTGejQU+3bIUensX3OsRwm7lnhrWp16VGBgsPyUJd2GVcT7Glte25FMh0SZvQgtO2f5YMSl1GLNYS1pSAonQnOaMB8TxYnV60MMJ7Lf9KDN9dXL0yJpXu+8/v/UoAD+A4qLUKNRA2bwc/SIwUVzLQ1ezoDvy/2GbUZoL9bbt8Zt3elMFPK5XjyPZoRpR4O1I2+40YXzkGX7UWkvilKMLWiH9gc8bb1Lo67qzVbsrK+xX53Fp5uFXWapzVd88uQE1j67OTmmIyNM4jvXAgMBAAECggEBAIBmasUjLq7IFMDtbBd1vGbsZ0AXbiGuh7pxcIOW8jottORk8RAJPQOWZ3X715KNSiW83KYu94aDm9umQFCUVORD4ri80O5363mpf6YtdMpt/rJgBq2yZ61Jd9DctYhaPmlGmts/TWp40KIV8MPYojOaaweqv0cZ09+sskT2OFqezmwITynTFr+oUI8zfwvuXi+dXkosiaH2Po1trP6q3/sO3RVIn1WuFvzIueIRSg4/xm1OU6UQ3tI7aX5sM7GfKTBia0pjQgaDExrp8FbY2RGzfGcZ0J91B66IyLRTPIXnoGYVCkqU8QKeQy2RWZsuUhC5JtIEKYVGHrRe0anQt+kCgYEA8P9G90KQZFrWoERFLtMLYSYimrNdm7Ght0R75q4dobruEH8yl3/IoVnvqe+qRwC/Gtc8Iz1MPvmweP276GyaXds+19E8J+e4VukW239A9CbsRRuVIG6WdAZsOanaE8gMpFX/kgRsgUzkbbKE6XWji3D4YcUjApH9cL/LEio0+rsCgYEAis7SfC5uknxCms1KAVk1NWICkeQWzDQIBfcqN8yaohScTYgwLDsUBd9VgxpeX5TQybRMHtNB/O7YVXyH308vyxN6yvdcTQ2gYviIrI/lggbgwh/qGWZTTHOeLGeVtWJlt33H6YZL5u37cVt86ZXYewFdEDIVrJoSLtNYAdX4l5UCgYB3DEOMJS+aw1y+crsSNGKsrN/vN6eIH6lZFNV30I8Gs4aiU5M0T4VWX00Kzz94L6A3rBci+UbO3R0Vx5GpcOyYbJOQnOVweQEQvbfy2dvGP+v6/3MdQEPGMN4JZiQ6HVpRW8IR6WmIPemhXsxVXy1Y1od8FJwEywU9SyEPahYdvQKBgFtNDM/Xrq01hI3UCCB47/GsBOq3L3Ojqb9awu8u6ms/yUuKD72YImR2cQWp+3c9KFPz/rmr1VSsOamJHsn4iQQ3TOZh69lW5C8VMLjocVqkcYPegBmy34mC5wBoJeXH0gXueucUiapc0K3QMopmIJ1P2UnE19LPCLS6GMIkXr49AoGBAJ7JhexixTTnOyqvZokIaMAC4Ce74jUynJTim0u8yeNchqECeCsfmmrryrkdUFigNt9o00YGF66B6ow20aIBSXxan6/VpZ72ham9l0r9I7xn/OfJNDROr6UFr1N/l1LzlBKI4cdYXa9v9BWj+0gxiLJ3rQ9qbyARXFEzo/Ick2qF";

    @Override
    public void setProperties(Properties properties) {
        super.setProperties(properties);
        String pwd = properties.getProperty("password");
        log.info("get pw: " + pwd);
        if (StringUtils.isNotBlank(pwd)) {
            try {
                //对jdbc.properties配置的密码密文 进行解密
                String password = RSAKeysUtil.decryptByPrivateKey(PRIVATE_KEY_STRING, pwd);
                log.info("decryptByPrivateKey pw: " + password);
                setPassword(password.toCharArray());
            } catch (Exception e) {
                setPassword(pwd.toCharArray());
            }
        }
    }
}
