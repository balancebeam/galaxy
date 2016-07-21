package io.anyway.galaxy.demo.service;

import io.anyway.galaxy.demo.domain.RepositoryDO;

/**
 * Created by yangzz on 16/7/19.
 */
public interface RepositoryService {

    boolean decreaseRepository(long id,long stock);
}
