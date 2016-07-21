package io.anyway.galaxy.intercepter.support;

import io.anyway.galaxy.context.support.ServiceExcecutePayload;
import io.anyway.galaxy.intercepter.ServiceIntercepter;
import org.springframework.stereotype.Component;

import java.sql.Connection;

/**
 * Created by yangzz on 16/7/21.
 */
@Component
public class ServiceIntercepterSupport implements ServiceIntercepter {

    @Override
    public void tryService(Connection conn, ServiceExcecutePayload payload, long txId) {

    }

    @Override
    public void confirmService(Connection conn, long txId) {

    }

    @Override
    public void cancelService(Connection conn, long txId) {

    }
}
