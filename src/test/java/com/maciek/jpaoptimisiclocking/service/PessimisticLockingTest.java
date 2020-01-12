package com.maciek.jpaoptimisiclocking.service;

import com.maciek.jpaoptimisiclocking.model.Account;
import com.maciek.jpaoptimisiclocking.repository.InMemoryOrderExecution;
import com.maciek.jpaoptimisiclocking.repository.MyRepositoryPessimisticLocking;
import com.maciek.jpaoptimisiclocking.repository.MyRepositoryOptimisticLocking;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
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
class PessimisticLockingTest {

    @Autowired
    private MyRepositoryOptimisticLocking myRepositoryOptimisticLocking;

    @Autowired
    private MyRepositoryPessimisticLocking myRepositoryPessimisticLocking;

    @Autowired
    private InMemoryOrderExecution inMemoryOrderExecution;

    @Autowired
    private SlowService slowService;

    @Autowired
    private QuickService quickService;

    @BeforeEach
    void setup() {
        Account account = Account.builder()
                .id(1L)
                .balance(valueOf(100))
                .build();
        myRepositoryPessimisticLocking.save(account);
        Account account2 = Account.builder()
                .id(2L)
                .balance(valueOf(0))
                .build();
        myRepositoryPessimisticLocking.save(account2);
    }

    @AfterEach
    void cleanup() {
        myRepositoryPessimisticLocking.deleteAll();
        inMemoryOrderExecution.reset();
    }


    @Test
    @DisplayName("should take update from both services " +
            "when SlowService locked the resource and QuickService has to wait for unlock")
    void shouldTakeUpdateFromSBothServices_whenSlowServiceLockedResourceAndQuickServiceHasToWaitForIt() throws ExecutionException, InterruptedException {

        final CompletableFuture<Void> slowFuture = runAsync(() -> slowService.depositWithPessimisticLocking(1, valueOf(10)));
        final CompletableFuture<Void> quickFuture = runAsync(() -> quickService.depositWithPessimisticLocking(1, valueOf(30)));
        CompletableFuture.allOf(slowFuture, quickFuture).get();
        final Account acc = myRepositoryPessimisticLocking.findById_pessimisticLocking(1L).orElseThrow();

        assertThat(inMemoryOrderExecution.getExecutions()) //that way we can see that QuickService has to wait until SlowService unlock resource
                .containsExactly(
                        "SLOW_BEFORE_READ",
                        "SLOW_AFTER_READ_BEFORE_UPDATE",
                        "QUICK_BEFORE_READ",
                        "SLOW_AFTER_UPDATE", //here the unlock appears
                        "QUICK_AFTER_READ_BEFORE_UPDATE", //the read is performed after unlocking the resource
                        "QUICK_AFTER_UPDATE");
        assertThat(acc.getBalance()).usingComparator(BigDecimal::compareTo).isEqualTo(valueOf(140));
    }

    @Test
    @DisplayName("should transfer all available funds and after deposit 30 to account with id 1 " +
            "when SlowService locked the resource and QuickService has to wait for unlock")
    void shouldTransferAllAvailableFundsAndAfterDeposit30_whenSlowServiceLockedResourceAndQuickServiceHasToWaitForIt() throws ExecutionException, InterruptedException {

        final CompletableFuture<Void> slowFuture = runAsync(() -> slowService.transferAllFundsFromOneToAnother_pessimisticLocking(1, 2));
        final CompletableFuture<Void> quickFuture = runAsync(() -> quickService.depositWithPessimisticLocking(1, valueOf(30)));
        CompletableFuture.allOf(slowFuture, quickFuture).get();
        final Account acc1 = myRepositoryPessimisticLocking.findById_pessimisticLocking(1L).orElseThrow();
        final Account acc2 = myRepositoryPessimisticLocking.findById_pessimisticLocking(2L).orElseThrow();

        assertThat(acc1.getBalance()).usingComparator(BigDecimal::compareTo).isEqualTo(valueOf(30));
        assertThat(acc2.getBalance()).usingComparator(BigDecimal::compareTo).isEqualTo(valueOf(100));
    }


}