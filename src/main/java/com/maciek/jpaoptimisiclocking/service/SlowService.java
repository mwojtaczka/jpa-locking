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
public class SlowService {

    private final MyRepositoryPessimisticLocking myRepositoryPessimisticLocking;
    private final MyRepositoryOptimisticLocking myRepositoryOptimisticLocking;
    private final InMemoryOrderExecution inMemoryOrderExecution;

    public SlowService(MyRepositoryPessimisticLocking myRepositoryPessimisticLocking, MyRepositoryOptimisticLocking myRepositoryOptimisticLocking, InMemoryOrderExecution inMemoryOrderExecution) {
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

    @Transactional
    public void transferAllFundsFromOneToAnother(long creditorId, long debtorId) {
        final Account creditor = myRepositoryPessimisticLocking.findById(creditorId).orElseThrow();
        final Account debtor = myRepositoryPessimisticLocking.findById(debtorId).orElseThrow();

        final BigDecimal balance = creditor.getBalance();
        creditor.withdraw(balance);
        debtor.deposit(balance);

        sleep();

        myRepositoryPessimisticLocking.save(creditor);
        myRepositoryPessimisticLocking.save(debtor);
    }

    public void depositWithOptimisticLocking(long accId, BigDecimal amount) {
        final AccountWithVersion acc = myRepositoryOptimisticLocking.findById_optimisticLocking(accId).orElseThrow();

        sleep();
        acc.deposit(amount);
        myRepositoryOptimisticLocking.save(acc);
    }

    @Transactional
    public void transferAllFundsFromOneToAnother_optimisticLocking(long creditorId, long debtorId) {
        final AccountWithVersion creditor = myRepositoryOptimisticLocking.findById_optimisticLocking(creditorId).orElseThrow();
        final AccountWithVersion debtor = myRepositoryOptimisticLocking.findById_optimisticLocking(debtorId).orElseThrow();

        final BigDecimal balance = creditor.getBalance();
        creditor.withdraw(balance);
        debtor.deposit(balance);

        sleep();

        myRepositoryOptimisticLocking.save(creditor);
        myRepositoryOptimisticLocking.save(debtor);
    }

    @Transactional
    public void depositWithPessimisticLocking(long accId, BigDecimal amount) {
        inMemoryOrderExecution.add("SLOW_BEFORE_READ");
        final Account acc = myRepositoryPessimisticLocking.findById_pessimisticLocking(accId).orElseThrow();
        inMemoryOrderExecution.add("SLOW_AFTER_READ_BEFORE_UPDATE");

        sleep();
        acc.deposit(amount);
        myRepositoryPessimisticLocking.save(acc);
        inMemoryOrderExecution.add("SLOW_AFTER_UPDATE");
    }

    @Transactional
    public void transferAllFundsFromOneToAnother_pessimisticLocking(long creditorId, long debtorId) {
        final Account creditor = myRepositoryPessimisticLocking.findById_pessimisticLocking(creditorId).orElseThrow();
        final Account debtor = myRepositoryPessimisticLocking.findById_pessimisticLocking(debtorId).orElseThrow();

        final BigDecimal balance = creditor.getBalance();
        creditor.withdraw(balance);
        debtor.deposit(balance);

        sleep();

        myRepositoryPessimisticLocking.save(creditor);
        myRepositoryPessimisticLocking.save(debtor);
    }

    public void sleep() {
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
