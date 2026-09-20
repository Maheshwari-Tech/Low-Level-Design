package code.behavioral.command;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;

public final class CommandDemo {
    private CommandDemo() {
    }

    private interface Command {
        void execute();

        void undo();
    }

    private static final class TextEditor {
        private final StringBuilder text = new StringBuilder();

        void append(String value) {
            text.append(value);
        }

        void truncate(int length) {
            text.setLength(length);
        }

        int length() {
            return text.length();
        }

        String text() {
            return text.toString();
        }
    }

    private static final class AppendTextCommand implements Command {
        private final TextEditor editor;
        private final String value;
        private int previousLength;
        private boolean executed;

        private AppendTextCommand(TextEditor editor, String value) {
            this.editor = Objects.requireNonNull(editor, "editor");
            this.value = Objects.requireNonNull(value, "value");
        }

        @Override
        public void execute() {
            if (executed) {
                throw new IllegalStateException("command is already executed");
            }
            previousLength = editor.length();
            editor.append(value);
            executed = true;
        }

        @Override
        public void undo() {
            if (!executed) {
                throw new IllegalStateException("command is not executed");
            }
            editor.truncate(previousLength);
            executed = false;
        }
    }

    private static final class CommandHistory {
        private final Deque<Command> executed = new ArrayDeque<>();

        void execute(Command command) {
            command.execute();
            executed.push(command);
        }

        void undoLast() {
            if (!executed.isEmpty()) {
                executed.pop().undo();
            }
        }
    }

    public static void main(String[] args) {
        TextEditor editor = new TextEditor();
        CommandHistory history = new CommandHistory();

        history.execute(new AppendTextCommand(editor, "Design "));
        history.execute(new AppendTextCommand(editor, "Patterns"));
        System.out.println(editor.text());

        history.undoLast();
        System.out.println(editor.text());
    }
}
