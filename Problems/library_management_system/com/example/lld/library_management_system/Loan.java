package com.example.lld.library_management_system;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

public final class Loan {
    private final String loanId;
    private final Book book;
    private final Member member;
    private final LocalDate issueDate;
    private final LocalDate dueDate;

    private LocalDate returnDate;

    public Loan(String loanId, Book book, Member member, int loanPeriodDays) {
        if (loanId == null || loanId.isBlank()) {
            throw new IllegalArgumentException("Loan id is required");
        }
        if (loanPeriodDays <= 0) {
            throw new IllegalArgumentException("Loan period must be positive");
        }
        this.loanId = loanId;
        this.book = Objects.requireNonNull(book, "book");
        this.member = Objects.requireNonNull(member, "member");
        this.issueDate = LocalDate.now();
        this.dueDate = issueDate.plusDays(loanPeriodDays);
    }

    public void returnBook() {
        if (returnDate != null) {
            throw new IllegalStateException("Loan " + loanId + " is already returned");
        }
        returnDate = LocalDate.now();
        book.returnCopy();
        member.removeLoan(this);
    }

    public boolean isOverdue() {
        return returnDate == null && LocalDate.now().isAfter(dueDate);
    }

    public double calculateFine(double finePerDay) {
        if (finePerDay < 0 || !Double.isFinite(finePerDay)) {
            throw new IllegalArgumentException("Fine per day must be finite and non-negative");
        }
        if (!isOverdue()) {
            return 0;
        }
        return ChronoUnit.DAYS.between(dueDate, LocalDate.now()) * finePerDay;
    }

    public String getLoanId() {
        return loanId;
    }

    public Book getBook() {
        return book;
    }

    public Member getMember() {
        return member;
    }

    public LocalDate getIssueDate() {
        return issueDate;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public LocalDate getReturnDate() {
        return returnDate;
    }
}
