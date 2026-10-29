package com.auraspend.api.expense;

import com.auraspend.api.user.AppUser;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "expenses")
public class Expense {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    public AppUser user;

    @Column(nullable = false)
    public double amount;

    @Column(nullable = false, length = 10)
    public String date;

    @Column(nullable = false, length = 30)
    public String category;

    @Column(length = 500)
    public String description;

    public Expense() {
    }
}
