package me.eventually.carpethikariaddition.command;

import static net.minecraft.server.command.CommandManager.*;

import com.mojang.brigadier.CommandDispatcher;

import net.minecraft.command.argument.DimensionArgumentType;
import net.minecraft.command.permission.Permission;
import net.minecraft.command.permission.PermissionLevel;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.TeleportTarget;
import net.minecraft.world.border.WorldBorder;
import net.minecraft.world.dimension.DimensionType;

public class EasyDebugCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(literal("easydebug").then(literal("teleportDimScale").then(argument("<dim>",  DimensionArgumentType.dimension())
                .executes(context -> teleportDimScale(context.getSource(), DimensionArgumentType.getDimensionArgument(context, "<dim>"))))).requires(source -> source.getPermissions().hasPermission(new Permission.Level(
                        PermissionLevel.ADMINS
        ))));
    }

    private static int teleportDimScale(ServerCommandSource source, ServerWorld toWorld) {

        ServerPlayerEntity player = source.getPlayer();
        if (player == null) {
            source.sendError(Text.literal("You need to be a player in game to use this command!"));
            return 1;
        }
        ServerWorld fromWorld = player.getEntityWorld();
        if (!player.canTeleportBetween(fromWorld, toWorld)) {
            source.sendError(Text.literal("You can't teleport between these dimensions!"));
            return 1;
        }
        WorldBorder worldBorder = toWorld.getWorldBorder();
        double d = DimensionType.getCoordinateScaleFactor(fromWorld.getDimension(), toWorld.getDimension());
        BlockPos scaledPos = worldBorder.clampFloored(player.getX() * d, player.getY(), player.getZ() * d);
        double x = scaledPos.getX();
        double y = scaledPos.getY();
        double z = scaledPos.getZ();
        TeleportTarget target = new TeleportTarget(
                toWorld,
                Vec3d.ZERO.add(x, y, z),
                Vec3d.ZERO,
                0f, 0f,
                TeleportTarget.NO_OP
        );
        player.teleportTo(target);
        return 0;
    }
}
