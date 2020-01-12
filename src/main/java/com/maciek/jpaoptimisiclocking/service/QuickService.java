package com.maciek.jpaoptimisiclocking.service;

import com.maciek.jpaoptimisiclocking.model.Account;
import com.maciek.jpaoptimisiclocking.model.AccountWithVersion;
import com.maciek.jpaoptimisiclocking.repository.InMemoryOrderExecution;
import com.maciek.jpaoptimisiclocking.repository.MyRepositoryPessimisticLocking;
import com.maciek.jpaoptimisiclocking.repository.MyRepositoryOptimisticLocking;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class QuickService {
    private final MyRepositoryPessimisticLocking myRepositoryPessimisticLocking;
    private final MyRepositoryOptimisticLocking myRepositoryOptimisticLocking;
    private final InMemoryOrderExecution inMemoryOrderExecution;

    public QuickService(MyRepositoryPessimisticLocking myRepositoryPessimisticLocking, MyRepositoryOptimisticLocking myRepositoryOptimisticLocking, InMemoryOrderExecution inMemoryOrderExecution) {
        this.myRepositoryPessimisticLocking = myRepositoryPessimisticLocking;
        this.myRepositoryOptimisticLocking = myRepositoryOptimisticLocking;
        this.inMemoryOrderExecution = inMemoryOrderExecution;
    }

    public void deposit(long accId, BigDecimal amount) {
        final Account acc = myRepositoryPessimisticLocking.findById(accId).orElseThrow();

        sleep();
        acc.deposit(amount);
        myRepositoryPessimisticLocking.save(acc);
    }

    public void depositWithOptimisticLocking(long accId, BigDecimal amount) {
        final AccountWithVersion acc = myRepositoryOptimisticLocking.findById_optimisticLocking(accId).orElseThrow();

        sleep();
        acc.deposit(amount);
        myRepositoryOptimisticLocking.save(acc);
    }

    @Transactional
    public void depositWithOptimisticLocking_butFail(long accId, BigDecimal amount) {
        final AccountWithVersion acc = myRepositoryOptimisticLocking.findById_optimisticLocking(accId).orElseThrow();

        sleep();
        acc.deposit(amount);
        myRepositoryOptimisticLocking.save(acc);
        throw new RuntimeException();
    }

    @Transactional
    public void depositWithPessimisticLocking(long accId, BigDecimal amount) {
        sleep();
        inMemoryOrderExecution.add("QUICK_BEFORE_READ");
        final Account acc = myRepositoryPessimisticLocking.findById_pessimisticLocking(accId).orElseThrow();
        inMemoryOrderExecution.add("QUICK_AFTER_READ_BEFORE_UPDATE");

        acc.deposit(amount);
        myRepositoryPessimisticLocking.save(acc);
        inMemoryOrderExecution.add("QUICK_AFTER_UPDATE");
    }

    public void sleep() {
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
