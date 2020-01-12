package com.maciek.jpaoptimisiclocking.service;

import com.maciek.jpaoptimisiclocking.model.AccountWithVersion;
import com.maciek.jpaoptimisiclocking.repository.MyRepositoryOptimisticLocking;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static java.math.BigDecimal.valueOf;
import static java.util.concurrent.CompletableFuture.runAsync;
import static java.util.concurrent.CompletableFuture.supplyAsync;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(SpringExtension.class)
@SpringBootTest
class OptimisticLockingTest {

    @Autowired
    private MyRepositoryOptimisticLocking myRepositoryOptimisticLocking;

    @Autowired
    private SlowService slowService;

    @Autowired
    private QuickService quickService;

    @BeforeEach
    void setup() {
        AccountWithVersion accountWithVersion = AccountWithVersion.builder()
                .id(1L)
                .balance(valueOf(100))
                .version(1L)
                .build();
        AccountWithVersion accountWithVersion2 = AccountWithVersion.builder()
                .id(2L)
                .balance(valueOf(0))
                .version(1L)
                .build();
        myRepositoryOptimisticLocking.save(accountWithVersion);
        myRepositoryOptimisticLocking.save(accountWithVersion2);
    }

    @AfterEach
    void cleanup() {
        myRepositoryOptimisticLocking.deleteAll();
    }

    @Test
    @DisplayName("should take update from QuickService and throw ObjectOptimisticLockingFailureException in SlowService " +
            "when optimistic locking enabled and QuickService update is first")
    void shouldTakeUpdateFromQuickServiceAndThrowOptimisticLockingExceptionInSlowService_whenOptimisticLocking() throws ExecutionException, InterruptedException {

        final CompletableFuture<ObjectOptimisticLockingFailureException> slowFuture = supplyAsync(() ->
                assertThrows(ObjectOptimisticLockingFailureException.class,
                        () -> slowService.depositWithOptimisticLocking(1, valueOf(10)))
        );
        final CompletableFuture<Void> quickFuture = runAsync(() -> quickService.depositWithOptimisticLocking(1, valueOf(30)));
        CompletableFuture.allOf(slowFuture, quickFuture).get();
        final AccountWithVersion acc = myRepositoryOptimisticLocking.findById_optimisticLocking(1L).orElseThrow();

        assertThat(acc.getBalance()).usingComparator(BigDecimal::compareTo).isEqualTo(valueOf(130));
    }

    @Test
    @DisplayName("should rollback money transfer transaction " +
            "when other thread updated the balance")
    void shouldRollbackMoneyTransferTransaction_whenOtherThreadUpdatedBalance() throws ExecutionException, InterruptedException {

        final CompletableFuture<ObjectOptimisticLockingFailureException> slowFuture = supplyAsync(() ->
                assertThrows(ObjectOptimisticLockingFailureException.class,
                        () -> slowService.transferAllFundsFromOneToAnother_optimisticLocking(1, 2))
        );
        final CompletableFuture<Void> quickFuture = runAsync(() -> quickService.depositWithOptimisticLocking(1, valueOf(30)));
        CompletableFuture.allOf(slowFuture, quickFuture).get();
        final AccountWithVersion creditor = myRepositoryOptimisticLocking.findById_optimisticLocking(1L).orElseThrow();
        final AccountWithVersion debtor = myRepositoryOptimisticLocking.findById_optimisticLocking(2L).orElseThrow();

        assertThat(creditor.getBalance()).usingComparator(BigDecimal::compareTo).isEqualTo(valueOf(130));
        assertThat(debtor.getBalance()).usingComparator(BigDecimal::compareTo).isEqualTo(valueOf(0));
    }

    @Test
    @DisplayName("should transfer money when other thread updated the balance but fails and didn't commit")
    void shouldTransferMoney_whenOtherThreadUpdatedBalanceButFails() throws ExecutionException, InterruptedException {

        final CompletableFuture<Void> slowFuture =
                runAsync(() -> slowService.transferAllFundsFromOneToAnother_optimisticLocking(1, 2));
        final CompletableFuture<RuntimeException> quickFuture = supplyAsync(() ->
                assertThrows(RuntimeException.class,
                        () -> quickService.depositWithOptimisticLocking_butFail(1, valueOf(30)))
        );
        CompletableFuture.allOf(slowFuture, quickFuture).get();
        final AccountWithVersion creditor = myRepositoryOptimisticLocking.findById_optimisticLocking(1L).orElseThrow();
        final AccountWithVersion debtor = myRepositoryOptimisticLocking.findById_optimisticLocking(2L).orElseThrow();

        assertThat(creditor.getBalance()).usingComparator(BigDecimal::compareTo).isEqualTo(valueOf(0));
        assertThat(debtor.getBalance()).usingComparator(BigDecimal::compareTo).isEqualTo(valueOf(100));
    }

}