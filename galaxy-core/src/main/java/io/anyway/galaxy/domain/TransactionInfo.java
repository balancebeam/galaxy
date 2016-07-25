package io.anyway.galaxy.domain;

import lombok.Getter;
import lombok.Setter;

import java.sql.Date;
import java.text.SimpleDateFormat;

import io.anyway.galaxy.common.TransactionStatusEnum;
import io.anyway.galaxy.common.TransactionTypeEnum;

/**
 * Created by xiongjie on 2016/7/21.
 */
@Getter
@Setter
public class TransactionInfo {
    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
    
    private long txId;

    private long parentId;

    private long businessId;

    private String businessType;

    private int txStatus = -1;

    private int txType = -1;

    private String context;

    private String payload;

    private int retried_count = -1;

    private Date gmtCreated;

    private Date gmtModified;
   

    
    public String getStrGmtCreated(){
    	return sdf.format(gmtCreated).toString();
    }
    
    public String getStrGmtModified(){
    	return sdf.format(gmtModified).toString();
    }
    
    public String getStrTXType(){
    	return TransactionTypeEnum.getMemo(txType);
    }
    
    public String getStrTXStatus(){
    	return TransactionStatusEnum.getMemo(txStatus);
    }
    
}
