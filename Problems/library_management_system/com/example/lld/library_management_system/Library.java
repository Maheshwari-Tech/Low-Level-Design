package com.example.lld.library_management_system;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

public final class Library {
    private final Map<String, Book> books = new LinkedHashMap<>();
    private final Map<String, Member> members = new LinkedHashMap<>();
    private final Map<String, Loan> activeLoans = new LinkedHashMap<>();
    private final AtomicLong loanSequence = new AtomicLong();

    public void addBook(Book book) {
        Book requiredBook = Objects.requireNonNull(book, "book");
        if (books.putIfAbsent(requiredBook.getIsbn(), requiredBook) != null) {
            throw new IllegalArgumentException("Book already exists: " + requiredBook.getIsbn());
        }
    }

    public void addMember(Member member) {
        Member requiredMember = Objects.requireNonNull(member, "member");
        if (members.putIfAbsent(requiredMember.getId(), requiredMember) != null) {
            throw new IllegalArgumentException("Member already exists: " + requiredMember.getId());
        }
    }

    /** Returns null when the requested loan violates a lending rule. */
    public Loan lendBook(String memberId, String isbn, int loanPeriodDays) {
        if (loanPeriodDays <= 0) {
            throw new IllegalArgumentException("Loan period must be positive");
        }
        Member member = members.get(memberId);
        Book book = books.get(isbn);
        if (member == null || book == null || !book.isAvailable() || member.hasOverdueLoans()) {
            return null;
        }

        book.lendCopy();
        Loan loan = new Loan("L" + loanSequence.incrementAndGet(), book, member, loanPeriodDays);
        activeLoans.put(loan.getLoanId(), loan);
        member.addLoan(loan);
        return loan;
    }

    public boolean returnBook(String loanId) {
        Loan loan = activeLoans.remove(loanId);
        if (loan == null) {
            return false;
        }
        loan.returnBook();
        return true;
    }

    public Map<String, Book> getBooks() {
        return Map.copyOf(books);
    }

    public Map<String, Member> getMembers() {
        return Map.copyOf(members);
    }

    public Map<String, Loan> getLoans() {
        return Map.copyOf(activeLoans);
    }
}
