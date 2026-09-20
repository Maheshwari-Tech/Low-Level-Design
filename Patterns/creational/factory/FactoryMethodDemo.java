package code.creational.factory;

public final class FactoryMethodDemo {
    private FactoryMethodDemo() {
    }

    private interface Button {
        void render();
    }

    private static final class WebButton implements Button {
        @Override
        public void render() {
            System.out.println("rendered HTML button");
        }
    }

    private static final class DesktopButton implements Button {
        @Override
        public void render() {
            System.out.println("rendered desktop button");
        }
    }

    private abstract static class Dialog {
        final void renderWindow() {
            createButton().render();
        }

        protected abstract Button createButton();
    }

    private static final class WebDialog extends Dialog {
        @Override
        protected Button createButton() {
            return new WebButton();
        }
    }

    private static final class DesktopDialog extends Dialog {
        @Override
        protected Button createButton() {
            return new DesktopButton();
        }
    }

    public static void main(String[] args) {
        Dialog dialog = args.length == 0 ? new WebDialog() : new DesktopDialog();
        dialog.renderWindow();
    }
}
