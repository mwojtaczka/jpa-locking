package com.maciek.jpaoptimisiclocking.repository;

import com.maciek.jpaoptimisiclocking.model.AccountWithVersion;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.LockModeType;
import java.util.Optional;

@Repository
public interface MyRepositoryOptimisticLocking extends CrudRepository<AccountWithVersion, Long> {

    @Transactional
    @Lock(LockModeType.OPTIMISTIC) //it is default when we have @Version in our entity, so no need to declare it explicitly
    @Query("select a from AccountWithVersion a where a.id = :id")
    Optional<AccountWithVersion> findById_optimisticLocking(Long id);

}
