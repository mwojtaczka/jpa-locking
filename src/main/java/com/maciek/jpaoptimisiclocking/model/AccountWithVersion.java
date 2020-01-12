package com.maciek.jpaoptimisiclocking.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Version;
import java.math.BigDecimal;

@Entity
@Table(name = "account_ver")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountWithVersion {

    @Id
    private Long id;
    private BigDecimal balance;
    @Version
    private Long version;

    public void deposit(BigDecimal amount) {
        this.balance = balance.add(amount);
    }

    public void withdraw(BigDecimal amount) {
        this.balance = balance.subtract(amount);
    }

}
