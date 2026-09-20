package code.structural.composite;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class CompositeDemo {
    private CompositeDemo() {
    }

    private sealed interface FileNode permits FileLeaf, Directory {
        String name();

        long size();

        void print(String indentation);
    }

    private record FileLeaf(String name, long size) implements FileNode {
        private FileLeaf {
            Objects.requireNonNull(name, "name");
            if (size < 0) {
                throw new IllegalArgumentException("size cannot be negative");
            }
        }

        @Override
        public void print(String indentation) {
            System.out.printf("%s%s (%d)%n", indentation, name, size);
        }
    }

    private static final class Directory implements FileNode {
        private final String name;
        private final List<FileNode> children = new ArrayList<>();

        private Directory(String name) {
            this.name = Objects.requireNonNull(name, "name");
        }

        Directory add(FileNode child) {
            children.add(Objects.requireNonNull(child, "child"));
            return this;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public long size() {
            return children.stream().mapToLong(FileNode::size).sum();
        }

        @Override
        public void print(String indentation) {
            System.out.printf("%s%s/ (%d)%n", indentation, name, size());
            children.forEach(child -> child.print(indentation + "  "));
        }
    }

    public static void main(String[] args) {
        Directory root = new Directory("project")
                .add(new FileLeaf("README.md", 120))
                .add(new Directory("src").add(new FileLeaf("Main.java", 420)));
        root.print("");
    }
}
