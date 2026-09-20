package code.creational.prototype;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class PrototypeDemo {
    private PrototypeDemo() {
    }

    private interface Prototype<T> {
        T copy();
    }

    private static final class Document implements Prototype<Document> {
        private final String title;
        private final List<String> sections;

        private Document(String title, List<String> sections) {
            this.title = Objects.requireNonNull(title, "title");
            this.sections = new ArrayList<>(sections);
        }

        void addSection(String section) {
            sections.add(section);
        }

        @Override
        public Document copy() {
            return new Document(title, sections);
        }

        @Override
        public String toString() {
            return title + " " + sections;
        }
    }

    public static void main(String[] args) {
        Document template = new Document("Design", List.of("Intent", "Trade-offs"));
        Document interviewCopy = template.copy();
        interviewCopy.addSection("Follow-ups");

        System.out.println("template: " + template);
        System.out.println("copy:     " + interviewCopy);
    }
}
