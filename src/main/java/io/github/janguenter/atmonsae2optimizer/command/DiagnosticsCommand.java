package io.github.janguenter.atmonsae2optimizer.command;

import io.github.janguenter.atmonsae2optimizer.diagnostics.DiagnosticsRegistry;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

public final class DiagnosticsCommand {
    private DiagnosticsCommand() {
    }

    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("atmonsae2")
                        .requires(source -> source.hasPermission(2))
                        .executes(context -> showStatus(context.getSource()))
                        .then(Commands.literal("status")
                                .executes(context -> showStatus(context.getSource())))
                        .then(Commands.literal("reset")
                                .requires(source -> source.hasPermission(4))
                                .executes(context -> reset(context.getSource()))));
    }

    private static int showStatus(net.minecraft.commands.CommandSourceStack source) {
        var snapshots = DiagnosticsRegistry.snapshots();
        source.sendSuccess(
                () -> Component.literal(
                        "AE2 watcher optimizer: " + snapshots.size()
                                + " live grid(s); exact-interest polling is unsupported/disabled"),
                false);
        for (var snapshot : snapshots) {
            source.sendSuccess(() -> Component.literal(snapshot.diagnosticLine()), false);
        }
        return snapshots.size();
    }

    private static int reset(net.minecraft.commands.CommandSourceStack source) {
        var resetCount = DiagnosticsRegistry.resetCounters();
        source.sendSuccess(
                () -> Component.literal("Reset AE2 watcher diagnostics for " + resetCount + " live grid(s)"),
                true);
        return resetCount;
    }
}
