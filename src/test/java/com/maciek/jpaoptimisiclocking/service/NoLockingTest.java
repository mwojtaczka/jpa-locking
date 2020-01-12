package com.maciek.jpaoptimisiclocking.service;

import com.maciek.jpaoptimisiclocking.model.Account;
import com.maciek.jpaoptimisiclocking.repository.MyRepositoryPessimisticLocking;
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
class NoLockingTest {

    @Autowired
    private MyRepositoryPessimisticLocking myRepositoryPessimisticLocking;

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
    }

    @Test
    @DisplayName("should lose update from QuickService")
    void shouldLoseUpdate30FromQuickService() throws ExecutionException, InterruptedException {
        //we start with account 100
        final CompletableFuture<Void> slowFuture = runAsync(() -> slowService.deposit(1, valueOf(10)));
        final CompletableFuture<Void> quickFuture = runAsync(() -> quickService.deposit(1, valueOf(30)));

        CompletableFuture.allOf(slowFuture, quickFuture).get();
        final Account acc = myRepositoryPessimisticLocking.findById(1L).orElseThrow();

        assertThat(acc.getBalance()).usingComparator(BigDecimal::compareTo).isEqualTo(valueOf(110));
    }

    @Test
    @DisplayName("should transfer all available funds and lose 30")
    void shouldTransferAllAvailableFundsAndLose30() throws ExecutionException, InterruptedException {
        //we start with account 100
        final CompletableFuture<Void> slowFuture = runAsync(() -> slowService.transferAllFundsFromOneToAnother(1, 2));
        final CompletableFuture<Void> quickFuture = runAsync(() -> quickService.deposit(1, valueOf(30)));
        CompletableFuture.allOf(slowFuture, quickFuture).get();
        final Account acc1 = myRepositoryPessimisticLocking.findById_pessimisticLocking(1L).orElseThrow();
        final Account acc2 = myRepositoryPessimisticLocking.findById_pessimisticLocking(2L).orElseThrow();

        assertThat(acc1.getBalance()).usingComparator(BigDecimal::compareTo).isEqualTo(valueOf(0));
        assertThat(acc2.getBalance()).usingComparator(BigDecimal::compareTo).isEqualTo(valueOf(100));
    }

}