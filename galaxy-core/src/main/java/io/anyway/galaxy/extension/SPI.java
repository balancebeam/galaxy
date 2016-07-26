package io.anyway.galaxy.extension;

import java.lang.annotation.*;

/**
 * 扩展点接口的标识。
 * <p />
 * 扩展点声明配置文件，格式修改。<br />
 * 以Protocol示例，配置文件META-INF/dubbo/com.xxx.Protocol内容：<br />
 * 由<br/>
 * <pre><code>com.foo.XxxProtocol
com.foo.YyyProtocol</code></pre><br/>
 * 改成使用KV格式<br/>
 * <pre><code>xxx=com.foo.XxxProtocol
yyy=com.foo.YyyProtocol
 * </code></pre>
 * <br/>
 * 原因：<br/>
 * 当扩展点的static字段或方法签名上引用了三方库，
 * 如果三方库不存在，会导致类初始化失败，
 * Extension标识Dubbo就拿不到了，异常信息就和配置对应不起来。
 * <br/>
 * 比如:
 * Extension("mina")加载失败，
 * 当用户配置使用mina时，就会报找不到扩展点，
 * 而不是报加载扩展点失败，以及失败原因。
 *
 * @author william.liangf
 * @author ding.lid
 * @export
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface SPI {

    /**
     * 缺省扩展点名。
     */
	String value() default "";

}