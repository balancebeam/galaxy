package io.anyway.galaxy.spring;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.sql.DataSource;

import io.anyway.galaxy.context.TXContext;
import io.anyway.galaxy.context.SerialNumberGenerator;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.Ordered;
import org.springframework.core.io.ResourceLoader;
import org.springframework.jdbc.datasource.ConnectionHolder;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

import io.anyway.galaxy.annotation.TXAction;
import io.anyway.galaxy.annotation.TXTry;
import io.anyway.galaxy.common.TransactionTypeEnum;
import io.anyway.galaxy.context.AbstractExecutePayload;
import io.anyway.galaxy.context.TXContextHolder;
import io.anyway.galaxy.context.support.ActionExecutePayload;
import io.anyway.galaxy.context.support.ServiceExecutePayload;
import io.anyway.galaxy.context.support.TXContextSupport;
import io.anyway.galaxy.exception.DistributedTransactionException;
import io.anyway.galaxy.infoBoard.TransactionServer;
import io.anyway.galaxy.intercepter.ActionIntercepter;
import io.anyway.galaxy.intercepter.ServiceIntercepter;

/**
 * Created by yangzz on 16/7/20.
 */
@Component
@Aspect
public class TXAnnotationAspect implements Ordered,ResourceLoaderAware{

    private static Log logger= LogFactory.getLog(TXAnnotationAspect.class);

    private ResourceLoader resourceLoader;

    @Autowired
    private DataSourceAdaptor dataSourceAdaptor;

    @Autowired
    private ActionIntercepter actionIntercepter;

    @Autowired
    private ServiceIntercepter serviceIntercepter;

    private ConcurrentHashMap<Method, AbstractExecutePayload> cache= new ConcurrentHashMap<Method, AbstractExecutePayload>();

    private TransactionServer transactionServer;
    
    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
    
    @PostConstruct
    public void init(){
//    	this.transactionServer = TransactionServer.instance();
//    	this.transactionServer.setDataSource(dataSourceAdaptor);
//    	this.transactionServer.start();
    }
    
    @PreDestroy
    public void destroy(){
//        this.transactionServer.shutdown();
    }

    //切面注解TXAction
    @Pointcut("@annotation(io.anyway.galaxy.annotation.TXAction)")
    public void pointcutTXAction(){}

    @Around("pointcutTXAction()")
    public Object processTXAction(ProceedingJoinPoint pjp) throws Throwable {
        if (TXContextHolder.getTXContext()!= null){
            return pjp.proceed();
        }
        //需要判断最外层是否开启了写事务
        assertTransactional();

        //获取方法上的注解内容
        final Method actionMethod = ((MethodSignature) pjp.getSignature()).getMethod();

        String serialNumber= "";
        //根据Action第一个入参获(类型必须是SerialNumberGenerator)取交易流水号
        if(pjp.getArgs().length==0){
            logger.warn("no any incoming parameter,you need input first value typeof SerialNumberGenerator, method: "+actionMethod);
        }
        else{
            Object firstValue= pjp.getArgs()[0];
            if(!(firstValue instanceof SerialNumberGenerator)){
                logger.warn("the first value is not typeof SerialNumberGenerator, method: "+actionMethod+", inArgs: "+pjp.getArgs());
            }
            else{
                serialNumber= ((SerialNumberGenerator)firstValue).getSerialNumber();
                if(StringUtils.isEmpty(serialNumber)){
                    logger.warn("incoming trade serial number is empty, method: "+actionMethod+", inArgs: "+pjp.getArgs());
                }
            }
        }
        //缓存actionMethod解析注解的内容
        ActionExecutePayload cachedPayload = (ActionExecutePayload)cache.get(actionMethod);
        for(;cachedPayload==null;){
            Class<?> target = pjp.getTarget().getClass();
            Method targetMethod= target.getDeclaredMethod(actionMethod.getName(),actionMethod.getParameterTypes());
            TXAction action = targetMethod.getAnnotation(TXAction.class);
            String bizType = action.bizType();
            if (StringUtils.isEmpty(bizType)) {
                logger.warn("miss business type, class: " + target + ",method: " + actionMethod);
            }
            cachedPayload = new ActionExecutePayload(bizType, target, actionMethod.getName(), actionMethod.getParameterTypes());
            cachedPayload.setTimeout(action.timeout());
            //设置分布式事务类型: TC | TCC
            cachedPayload.setTxType(action.value());
            cache.putIfAbsent(actionMethod, cachedPayload);
            cachedPayload = (ActionExecutePayload)cache.get(actionMethod);
        }
        final ActionExecutePayload payload= cachedPayload.clone();
        //设置运行时的入参
        payload.setArgs(pjp.getArgs());

        try {
            //获取新的连接开启新事务新增一条TX记录
            final TXContext ctx = actionIntercepter.addAction(serialNumber,payload);

            //绑定到ThreadLocal中
            TXContextHolder.setTXContext(ctx);
            //设置在Action操作里
            TXContextHolder.setAction(true);

            if (logger.isInfoEnabled()) {
                logger.info("generate TXContext: " + ctx+", actionExecutePayload: "+payload);
            }

            //获取外层业务开启事务的对应的数据库连接
            ConnectionHolder conHolder = (ConnectionHolder) TransactionSynchronizationManager.getResource(dataSourceAdaptor.getDataSource());
            final Connection conn = conHolder.getConnection();
            Method method= ReflectionUtils.findMethod(ConnectionHolder.class,"setConnection",Connection.class);
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
                                if(payload.getTxType()== TransactionTypeEnum.TCC){
                                    if (logger.isInfoEnabled()) {
                                        logger.info("will send \"confirm\" message, TXContext: " + ctx+", actionExecutePayload: "+payload);
                                    }
                                    actionIntercepter.confirmAction(ctx);
                                }
                                //确保在cancel之后执行通知方法
                            } else if ("rollback".equals(method.getName())) {
                                if (logger.isInfoEnabled()) {
                                    logger.info("will send \"cancel\" message, TXContext: " + ctx+", actionExecutePayload: "+payload);
                                }
                                actionIntercepter.cancelAction(ctx);
                            }
                        }
                    }
                }));

            //先执行业务操作
            Object result= pjp.proceed();
            //更改TX记录状态为TRIED
            actionIntercepter.tryAction(ctx);
            return result;

        }finally{
            //清空上下文内容
            TXContextHolder.setTXContext(null);
            TXContextHolder.setAction(null);
        }
    }

    //切面注解TXTry
    @Pointcut("@annotation(io.anyway.galaxy.annotation.TXTry)")
    public void pointcutTXTry(){ }

    @Around("pointcutTXTry()")
    public Object processTXTry(ProceedingJoinPoint pjp) throws Throwable {
        //如果调用关系在Action内不需要执行切面动作
        if(TXContextHolder.isAction()){
            return pjp.proceed();
        }
        //先验证事务
        assertTransactional();
        TXContext ctx= TXContextHolder.getTXContext();
        if(ctx ==null && pjp.getArgs().length> 0){
            //规定第一个参数为TXContext里面传递txId等信息
            Object firstValue= pjp.getArgs()[0];
            if(firstValue instanceof TXContext){
                ctx= (TXContext) firstValue;
            }
        }
        Assert.notNull(ctx);

        //获取方法上的注解内容
        final Method serviceMethod = ((MethodSignature) pjp.getSignature()).getMethod();
        //缓存serviceMethod解析注解的内容
        ServiceExecutePayload cachedPayload = (ServiceExecutePayload)cache.get(serviceMethod);
        for(;cachedPayload==null;){
            Class<?> target = pjp.getTarget().getClass();
            Method targetMethod= target.getDeclaredMethod(serviceMethod.getName(),serviceMethod.getParameterTypes());
            TXTry txTry= targetMethod.getAnnotation(TXTry.class);
            String bizType = txTry.bizType();
            if (StringUtils.isEmpty(bizType)) {
                logger.warn("miss business type, class: " + target + ",serviceExecutePayload: " + serviceMethod);
            }
            cachedPayload = new ServiceExecutePayload(bizType, target, serviceMethod.getName(), serviceMethod.getParameterTypes());
            cachedPayload.setConfirmMethod(txTry.confirm());
            cachedPayload.setCancelMethod(txTry.cancel());
            cache.putIfAbsent(serviceMethod, cachedPayload);
            cachedPayload = (ServiceExecutePayload)cache.get(serviceMethod);
        }
        ServiceExecutePayload payload= cachedPayload.clone();
        //设置运行时的入参
        payload.setArgs(pjp.getArgs());
        if (logger.isInfoEnabled()) {
            logger.info("found TXContext: " + ctx+", serviceExecutePayload: "+payload);
        }
        //先调用业务方法
        Object result= pjp.proceed();
        //更改TX状态为TRIED
        serviceIntercepter.tryService(ctx,payload);
        return result;
    }

    //切面注解TXConfirm
    @Pointcut("@annotation(io.anyway.galaxy.annotation.TXConfirm)")
    public void pointcutTXConfirm(){}

    @Around("pointcutTXConfirm()")
    public Object processTXConfirm(ProceedingJoinPoint pjp) throws Throwable {
        //如果调用关系在Action内不需要执行切面动作
        if(TXContextHolder.isAction()){
            return pjp.proceed();
        }
        //先验证事务
        assertTransactional();
        TXContext ctx= TXContextHolder.getTXContext();
        if(ctx ==null && pjp.getArgs().length> 0){
            //规定第一个参数为TXContext里面传递txId等信息
            Object firstValue= pjp.getArgs()[0];
            if(firstValue instanceof TXContext){
                ctx= (TXContext) firstValue;
            }
        }
        Assert.notNull(ctx);

        if (logger.isInfoEnabled()) {
            logger.info("found TXContext: " + ctx);
        }

        Object result= pjp.proceed();
        //更改TX表的状态为CONFIRMED
        serviceIntercepter.confirmService(ctx);
        return result;
    }

    //切面注解TXCancel
    @Pointcut("@annotation(io.anyway.galaxy.annotation.TXCancel)")
    public void pointcutTXCancel(){}

    @Around("pointcutTXCancel()")
    public Object processTXCancel(ProceedingJoinPoint pjp) throws Throwable {
        //如果调用关系在Action内不需要执行切面动作
        if(TXContextHolder.isAction()){
            return pjp.proceed();
        }
        //先验证事务
        assertTransactional();
        TXContext ctx= TXContextHolder.getTXContext();
        if(ctx ==null && pjp.getArgs().length> 0){
            //规定第一个参数为TXContext里面传递txId等信息
            Object firstValue= pjp.getArgs()[0];
            if(firstValue instanceof TXContext){
                ctx= (TXContext) firstValue;
            }
        }
        Assert.notNull(ctx);

        if (logger.isInfoEnabled()) {
            logger.info("found TXContext: " + ctx);
        }

        Object result= pjp.proceed();
        //更改TX表的状态为CANCELLED
        serviceIntercepter.cancelService(ctx);
        return result;
    }

    private void assertTransactional()throws Throwable{
        DataSource dataSource= dataSourceAdaptor.getDataSource();
        Assert.notNull(dataSource,"datasource can not empty");
        ConnectionHolder conHolder = (ConnectionHolder) TransactionSynchronizationManager.getResource(dataSource);
        if (conHolder == null || conHolder.getConnectionHandle()==null || !conHolder.isSynchronizedWithTransaction()) {
            throw new DistributedTransactionException("transaction connection is null");
        }
    }

    @Override
    public void setResourceLoader(ResourceLoader resourceLoader) {
        this.resourceLoader= resourceLoader;
    }
}

