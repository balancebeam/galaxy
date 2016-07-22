package io.anyway.galaxy.spring;

import io.anyway.galaxy.annotation.TXAction;
import io.anyway.galaxy.annotation.TXTry;
import io.anyway.galaxy.context.TXContextHolder;
import io.anyway.galaxy.context.support.ActionExecutePayload;
import io.anyway.galaxy.context.support.ServiceExcecutePayload;
import io.anyway.galaxy.context.support.TXContextSupport;
import io.anyway.galaxy.exception.TXException;
import io.anyway.galaxy.intercepter.ActionIntercepter;
import io.anyway.galaxy.intercepter.ServiceIntercepter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.Ordered;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.io.ResourceLoader;
import org.springframework.jdbc.datasource.ConnectionHolder;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

import javax.sql.DataSource;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;

/**
 * Created by yangzz on 16/7/20.
 */
@Component
@Aspect
public class TXAnnotationAspect implements Ordered,ResourceLoaderAware{

    private Log logger= LogFactory.getLog(TXAnnotationAspect.class);

    private ResourceLoader resourceLoader;

    @Autowired
    private DataSourceAdaptor dataSourceAdaptor;

    @Autowired
    private ActionIntercepter actionIntercepter;

    @Autowired
    private ServiceIntercepter serviceIntercepter;

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }

    //切面注解TXAction
    @Pointcut("@annotation(io.anyway.galaxy.annotation.TXAction)")
    public void pointcutTXAction(){}

    @Around("pointcutTXAction()")
    public Object processTXAction(ProceedingJoinPoint pjp) throws Throwable {
        //需要判断最外层是否开启了写事务
        assertTransactional();

        //获取方法上的注解内容
        MethodSignature signature = (MethodSignature) pjp.getSignature();
        Method method= signature.getMethod();
        TXAction action= method.getAnnotation(TXAction.class);
        Object target= pjp.getTarget();
        Class<?>[] types= method.getParameterTypes();
        Object[] args= pjp.getArgs();
        final TXAction.TXType type= action.value();
        int timeout= action.timeout();
        final ActionExecutePayload payload= new ActionExecutePayload(target,method.getName(),types,args);
        try {
            //获取新的连接开启新事务新增一条TransactionAction记录
            final long txId = actionIntercepter.addAction(payload, type, timeout);
            TXContextHolder.setTXContext(new TXContextSupport(txId));

            //获取外出业务开启事务的对应的数据库连接
            final Connection conn = DataSourceUtils.getConnection(dataSourceAdaptor.getDataSource());
            ConnectionHolder conHolder = (ConnectionHolder) TransactionSynchronizationManager.getResource(dataSourceAdaptor.getDataSource());
            method= ReflectionUtils.findMethod(ConnectionHolder.class,"setConnection",Connection.class);
            ReflectionUtils.makeAccessible(method);
            ReflectionUtils.invokeMethod(method,conHolder,
                Proxy.newProxyInstance(resourceLoader.getClassLoader(),
                        //重载Connection复写commit和rollback方法
                        new Class<?>[]{Connection.class}, new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                        try {
                            return method.invoke(conn,args);
                        }finally {
                            //确保在commit执行之后执行通知方法
                            if ("commit".equals(method.getName())) {
                                //如果是TCC类型事务才发送confirm消息
                                if(type== TXAction.TXType.TCC){
                                    if (logger.isInfoEnabled()) {
                                        logger.info("will send \"confirm\" message, txId=" + txId+", payload="+payload);
                                    }
                                    actionIntercepter.confirmAction(txId);
                                }
                                //确保在cancel之后执行通知方法
                            } else if ("rollback".equals(method.getName())) {
                                if (logger.isInfoEnabled()) {
                                    logger.info("will send \"cancel\" message, txId=" + txId+", payload="+payload);
                                }
                                actionIntercepter.cancelAction(txId);
                            }
                        }
                    }
                }));

            //先执行业务操作
            Object result= pjp.proceed();
            //更新TX表状态为成功态,在根据TX类型确定是否要发送Confirm消息
            actionIntercepter.tryAction(conn, txId);
            return result;

        }finally{
            TXContextHolder.setTXContext(null);
        }
    }

    //切面注解TXTry
    @Pointcut("@annotation(io.anyway.galaxy.annotation.TXTry)")
    public void pointcutTXTry(){}

    @Around("pointcutTXTry()")
    public Object processTXTry(ProceedingJoinPoint pjp) throws Throwable {
        //先验证事务
        assertTransactional();
        //交易txid
        Assert.notNull(TXContextHolder.getTXContext());
        long txId= TXContextHolder.getTXContext().getTxId();

        //获取方法上的注解内容
        MethodSignature signature = (MethodSignature) pjp.getSignature();
        Method method= signature.getMethod();
        Object target= pjp.getTarget();
        Class<?>[] types= method.getParameterTypes();
        Object[] args= pjp.getArgs();
        ServiceExcecutePayload payload= new ServiceExcecutePayload(target,method.getName(),types,args);
        TXTry txTry= method.getAnnotation(TXTry.class);
        if(!"".equals(txTry.confirm())){
            payload.setConfirmMethod(txTry.confirm());
        }
        if(!"".equals(txTry.cancel())){
            payload.setCancelMethod(txTry.cancel());
        }
        //先调用业务方法
        Object result= pjp.proceed();
        //获取外出业务开启事务的对应的数据库连接
        Connection conn = DataSourceUtils.getConnection(dataSourceAdaptor.getDataSource());
        serviceIntercepter.tryService(conn,payload,txId);
        return result;
    }

    //切面注解TXConfirm
    @Pointcut("@annotation(io.anyway.galaxy.annotation.TXConfirm)")
    public void pointcutTXConfirm(){}

    @Around("pointcutTXConfirm()")
    public Object processTXConfirm(ProceedingJoinPoint pjp) throws Throwable {
        //先验证事务
        assertTransactional();
        //交易txid
        Assert.notNull(TXContextHolder.getTXContext());
        long txId= TXContextHolder.getTXContext().getTxId();

        Object result= pjp.proceed();
        //获取外出业务开启事务的对应的数据库连接
        Connection conn = DataSourceUtils.getConnection(dataSourceAdaptor.getDataSource());
        //提交TX表的confirm状态
        serviceIntercepter.confirmService(conn,txId);
        return result;
    }

    //切面注解TXCancel
    @Pointcut("@annotation(io.anyway.galaxy.annotation.TXCancel)")
    public void pointcutTXCancel(){}

    @Around("pointcutTXCancel()")
    public Object processTXCancel(ProceedingJoinPoint pjp) throws Throwable {
        //先验证事务
        assertTransactional();
        //交易txid
        Assert.notNull(TXContextHolder.getTXContext());
        long txId= TXContextHolder.getTXContext().getTxId();

        Object result= pjp.proceed();
        //获取外出业务开启事务的对应的数据库连接
        Connection conn = DataSourceUtils.getConnection(dataSourceAdaptor.getDataSource());
        //提交TX表的confirm状态
        serviceIntercepter.cancelService(conn,txId);
        return result;
    }

    private void assertTransactional()throws Throwable{
        DataSource dataSource= dataSourceAdaptor.getDataSource();
        Assert.notNull(dataSource,"datasource can not empty");
        ConnectionHolder conHolder = (ConnectionHolder) TransactionSynchronizationManager.getResource(dataSource);
        if (conHolder == null || conHolder.getConnectionHandle()==null || !conHolder.isSynchronizedWithTransaction()) {
            throw new TXException("transaction connection is null");
        }
    }

    @Override
    public void setResourceLoader(ResourceLoader resourceLoader) {
        this.resourceLoader= resourceLoader;
    }
}

