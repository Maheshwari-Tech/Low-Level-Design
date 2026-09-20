package com.example.lld.library_management_system;

import java.util.ArrayList;
import java.util.List;

public final class Member {
    private final String id;
    private final String name;
    private final String email;
    private final List<Loan> loans = new ArrayList<>();

    public Member(String id, String name, String email) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Member id is required");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Member name is required");
        }
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Member email is required");
        }
        this.id = id;
        this.name = name;
        this.email = email;
    }

    public void addLoan(Loan loan) {
        if (loan == null || loans.contains(loan)) {
            throw new IllegalArgumentException("A new loan is required");
        }
        loans.add(loan);
    }

    public void removeLoan(Loan loan) {
        loans.remove(loan);
    }

    public boolean hasOverdueLoans() {
        return loans.stream().anyMatch(Loan::isOverdue);
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public List<Loan> getLoans() {
        return List.copyOf(loans);
    }
}
