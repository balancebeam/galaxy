package io.anyway.galaxy.spring;

import io.anyway.galaxy.annotation.TXAction;
import io.anyway.galaxy.annotation.TXTry;
import io.anyway.galaxy.context.TXContextHolder;
import io.anyway.galaxy.context.support.ActionExecutePayload;
import io.anyway.galaxy.context.support.ServiceExcecutePayload;
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
import org.springframework.core.Ordered;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.jdbc.datasource.ConnectionHolder;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.Assert;

import javax.sql.DataSource;
import java.lang.reflect.Method;
import java.sql.Connection;

/**
 * Created by yangzz on 16/7/20.
 */
@Component
@Aspect
public class TXAnnotationAspect implements Ordered {

    private Log logger= LogFactory.getLog(TXAnnotationAspect.class);

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
    public void processTXAction(ProceedingJoinPoint pjp) throws Throwable {
        //需要判断最外层是否开启了写事务
        assertTransactional();

        //获取方法上的注解内容
        MethodSignature signature = (MethodSignature) pjp.getSignature();
        Method method= signature.getMethod();
        TXAction action= method.getAnnotation(TXAction.class);
        Object target= pjp.getTarget();
        Class<?>[] types= method.getParameterTypes();
        Object[] args= pjp.getArgs();
        TXAction.TXType type= action.value();
        int timeout= action.timeout();
        ActionExecutePayload payload= new ActionExecutePayload(target,method.getName(),types,args);

        //获取新的连接开启新事务新增一条TransactionAction记录
        DataSource dataSource= dataSourceAdaptor.getDataSource();
        Connection newConnection= dataSource.getConnection();
        String txid;
        try{
            txid= actionIntercepter.addAction(newConnection,payload,type,timeout);
            if(logger.isInfoEnabled()){
                logger.info("will send \"cancel\" message, xid="+ txid);
            }
        }
        catch (Throwable e){
            logger.error("record transaction action error",e);
            throw e;
        }
        finally {
            DataSourceUtils.doCloseConnection(newConnection,dataSource);
        }
        //获取外出业务开启事务的对应的数据库连接
        Connection tranConnection = DataSourceUtils.getConnection(dataSourceAdaptor.getDataSource());
        try {
            //先执行业务操作
            pjp.proceed();
            //更新TX表状态为成功态,在根据TX类型确定是否要发送Confirm消息
            actionIntercepter.confirmAction(tranConnection,txid);
        }
        catch (Throwable e){
            logger.error("invoke distributed transaction error,txid="+txid+" payload="+payload,e);
            //如果失败则需要发送Cancel信息并更改TX表的状态为Cancelled
            newConnection= dataSource.getConnection();
            try{
                if(logger.isInfoEnabled()){
                    logger.info("will send \"cancel\" message,txid="+txid+" payload="+payload);
                }
                actionIntercepter.cancelAction(newConnection,txid);
            }
            finally {
                DataSourceUtils.doCloseConnection(newConnection,dataSource);
            }
            throw e;
        }
    }

    //切面注解TXTry
    @Pointcut("@annotation(io.anyway.galaxy.annotation.TXTry)")
    public void pointcutTXTry(){}

    @Around("pointcutTXTry()")
    public void processTXTry(ProceedingJoinPoint pjp) throws Throwable {
        //先验证事务
        assertTransactional();
        //交易txid
        Assert.notNull(TXContextHolder.getTXContext());
        String txid= TXContextHolder.getTXContext().getTXid();

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
        pjp.proceed();
        //获取外出业务开启事务的对应的数据库连接
        Connection tranConnection = DataSourceUtils.getConnection(dataSourceAdaptor.getDataSource());
        serviceIntercepter.tryService(tranConnection,payload,txid);
    }

    //切面注解TXConfirm
    @Pointcut("@annotation(io.anyway.galaxy.annotation.TXConfirm)")
    public void pointcutTXConfirm(){}

    @Around("pointcutTXConfirm()")
    public void processTXConfirm(ProceedingJoinPoint pjp) throws Throwable {
        //先验证事务
        assertTransactional();
        //交易txid
        Assert.notNull(TXContextHolder.getTXContext());
        String txid= TXContextHolder.getTXContext().getTXid();

        pjp.proceed();
        //获取外出业务开启事务的对应的数据库连接
        Connection tranConnection = DataSourceUtils.getConnection(dataSourceAdaptor.getDataSource());
        //提交TX表的confirm状态
        serviceIntercepter.confirmService(tranConnection,txid);
    }

    //切面注解TXCancel
    @Pointcut("@annotation(io.anyway.galaxy.annotation.TXCancel)")
    public void pointcutTXCancel(){}

    @Around("pointcutTXCancel()")
    public void processTXCancel(ProceedingJoinPoint pjp) throws Throwable {
        //先验证事务
        assertTransactional();
        //交易txid
        Assert.notNull(TXContextHolder.getTXContext());
        String txid= TXContextHolder.getTXContext().getTXid();

        pjp.proceed();
        //获取外出业务开启事务的对应的数据库连接
        Connection tranConnection = DataSourceUtils.getConnection(dataSourceAdaptor.getDataSource());
        //提交TX表的confirm状态
        serviceIntercepter.cancelService(tranConnection,txid);
    }

    private void assertTransactional()throws Throwable{
        DataSource dataSource= dataSourceAdaptor.getDataSource();
        Assert.notNull(dataSource,"datasource can not empty");
        ConnectionHolder conHolder = (ConnectionHolder) TransactionSynchronizationManager.getResource(dataSource);
        if (conHolder == null || conHolder.getConnectionHandle()==null || !conHolder.isSynchronizedWithTransaction()) {
            throw new TXException("transaction connection is null");
        }
    }

}

