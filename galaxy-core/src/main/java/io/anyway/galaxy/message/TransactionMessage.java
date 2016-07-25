package io.anyway.galaxy.message;

import lombok.Getter;
import lombok.Setter;

/**
 * Created by xiong.j on 2016/7/25.
 */

@Getter
@Setter
public class TransactionMessage {
    private long txId;

    private long businessId;

    private String businessType;

    private int txStatus = -1;

    private int txType = -1;

    private String context;

    private String payload;

    private int retried_count = -1;
}
