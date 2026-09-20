package com.example.lld.library_management_system;

public final class Main {
    private Main() {
    }

    public static void main(String[] args) {
        Library library = new Library();
        Book book = new Book("123", "Java Basics", "Author A", 5);
        library.addBook(book);

        Member member = new Member("M1", "John Doe", "john@example.com");
        library.addMember(member);

        Loan loan = library.lendBook(member.getId(), book.getIsbn(), 14);
        if (loan == null) {
            System.out.println("Book could not be lent.");
            return;
        }

        System.out.println("Book lent successfully. Due date: " + loan.getDueDate());
        if (library.returnBook(loan.getLoanId())) {
            System.out.println("Book returned successfully.");
        }
        System.out.println("Available copies of Java Basics: " + book.getAvailableCopies());
    }
}
