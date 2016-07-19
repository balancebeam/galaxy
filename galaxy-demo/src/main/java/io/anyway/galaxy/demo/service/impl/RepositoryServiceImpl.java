package io.anyway.galaxy.demo.service.impl;

import io.anyway.galaxy.annotation.TXCompensable;
import io.anyway.galaxy.demo.dao.RepositoryDao;
import io.anyway.galaxy.demo.domain.RepositoryDO;
import io.anyway.galaxy.demo.service.RepositoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**add
 * Created by yangzz on 16/7/19.
 */
@Service
public class RepositoryServiceImpl implements RepositoryService{

    @Autowired
    private RepositoryDao dao;

    @Transactional
    public void addRepository(RepositoryDO repositoryDO){
        dao.add(repositoryDO);
    }

    @Override
    @TXCompensable(cancel = "increaseRepository")
    public boolean decreaseRepository(int id,long number){
        return 0 < dao.decrease(new RepositoryDO(id,null,number));
    }

     boolean increaseRepository(int id,long number){
        return 0 < dao.increase(new RepositoryDO(id,null,number));
    }
}
