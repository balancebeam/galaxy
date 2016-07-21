package io.anyway.galaxy.domain;

import lombok.Getter;
import lombok.Setter;

import java.sql.Date;

/**
 * Created by xiongjie on 2016/7/21.
 */
@Getter
@Setter
public class TransactionInfo {

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


}
