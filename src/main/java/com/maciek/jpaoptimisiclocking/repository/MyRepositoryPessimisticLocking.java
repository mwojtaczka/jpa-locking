package com.maciek.jpaoptimisiclocking.repository;

import com.maciek.jpaoptimisiclocking.model.Account;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.LockModeType;
import java.util.Optional;

@Repository
public interface MyRepositoryPessimisticLocking extends CrudRepository<Account, Long> {

    @Transactional
    @Lock(LockModeType.PESSIMISTIC_WRITE) //unfortunately H2 DB doea not support PESSIMISTIC_READ
    @Query("select a from Account a where a.id = :id")
    Optional<Account> findById_pessimisticLocking(Long id);

}
