package code.structural.flyweight.wordProcessor.good;

public class Main {
    public static void main(String[] args) {
        Letter l1 = LetterFactory.createLetter('a', "a", 12);
        Letter l2 = LetterFactory.createLetter('a', "a", 12);
        Letter l3 = LetterFactory.createLetter('a', "a", 12);
        l1.display(1, 2);
        l2.display(2, 3);
        l3.display(1, 3);

    }
}
