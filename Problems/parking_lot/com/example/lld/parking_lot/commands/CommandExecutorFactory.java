package com.example.lld.parking_lot.commands;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

public final class CommandExecutorFactory {
    private final Map<Commands, CommandExecutor> executors = new EnumMap<>(Commands.class);

    public CommandExecutor getExecutor(Commands command) {
        CommandExecutor executor = executors.get(command);
        if (executor == null) {
            throw new IllegalArgumentException("No executor registered for command: " + command);
        }
        return executor;
    }

    public void addCommand(Commands command, CommandExecutor executor) {
        executors.put(
                Objects.requireNonNull(command, "command"),
                Objects.requireNonNull(executor, "executor"));
    }
}
