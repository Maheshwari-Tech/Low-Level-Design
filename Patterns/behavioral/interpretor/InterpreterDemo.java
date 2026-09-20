package code.behavioral.interpretor;

import java.util.Map;
import java.util.Objects;

public final class InterpreterDemo {
    private InterpreterDemo() {
    }

    private sealed interface Expression permits Variable, And, Or, Not {
        boolean interpret(Map<String, Boolean> context);
    }

    private record Variable(String name) implements Expression {
        private Variable {
            Objects.requireNonNull(name, "name");
        }

        @Override
        public boolean interpret(Map<String, Boolean> context) {
            return context.getOrDefault(name, false);
        }
    }

    private record And(Expression left, Expression right) implements Expression {
        @Override
        public boolean interpret(Map<String, Boolean> context) {
            return left.interpret(context) && right.interpret(context);
        }
    }

    private record Or(Expression left, Expression right) implements Expression {
        @Override
        public boolean interpret(Map<String, Boolean> context) {
            return left.interpret(context) || right.interpret(context);
        }
    }

    private record Not(Expression expression) implements Expression {
        @Override
        public boolean interpret(Map<String, Boolean> context) {
            return !expression.interpret(context);
        }
    }

    public static void main(String[] args) {
        Expression canPublish = new And(
                new Variable("authenticated"),
                new Or(new Variable("editor"), new Variable("admin")));

        Map<String, Boolean> editor = Map.of("authenticated", true, "editor", true);
        Map<String, Boolean> guest = Map.of("authenticated", true, "editor", false, "admin", false);

        System.out.println("editor can publish: " + canPublish.interpret(editor));
        System.out.println("guest denied: " + new Not(canPublish).interpret(guest));
    }
}
