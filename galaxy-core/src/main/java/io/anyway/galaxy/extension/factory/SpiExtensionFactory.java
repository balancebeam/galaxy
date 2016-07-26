package io.anyway.galaxy.extension.factory;

import com.google.common.base.Strings;
import io.anyway.galaxy.extension.ExtensionFactory;
import io.anyway.galaxy.extension.ExtensionLoader;
import io.anyway.galaxy.extension.SPI;
import org.springframework.stereotype.Component;

/**
 * SpiExtensionFactory
 * 
 * Created by xiong.j on 2016/7/26.
 */
@Component
public class SpiExtensionFactory implements ExtensionFactory {

    public <T> T getExtension(Class<T> type, String name) {
        if (type.isInterface() && type.isAnnotationPresent(SPI.class)) {
            ExtensionLoader<T> loader = ExtensionLoader.getExtensionLoader(type);
            if (Strings.isNullOrEmpty(name)) {
                SPI spi = type.getAnnotation(SPI.class);
                name = spi.value();
            }
            return loader.getExtension(name);
        }
        return null;
    }

}
