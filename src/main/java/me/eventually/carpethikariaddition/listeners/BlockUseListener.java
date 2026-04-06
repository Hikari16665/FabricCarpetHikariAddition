package me.eventually.carpethikariaddition.listeners;

import me.eventually.carpethikariaddition.ExtensionOwnSettings;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.Map;

public class BlockUseListener {
    private static final Map<String, Block> namedBlocks = Map.of(
            "nether_portal", Blocks.NETHER_PORTAL,
            "end_portal", Blocks.END_PORTAL,
            "end_gateway", Blocks.END_GATEWAY,
            "bubble_column", Blocks.BUBBLE_COLUMN,
            "ladder", Blocks.LADDER
    );
    public static ActionResult onBlockUse(PlayerEntity player, World world, Hand hand, HitResult hitResult) {
        if (!ExtensionOwnSettings.magicOfNamedBlock) {
            return ActionResult.PASS;
        }
        ItemStack active = player.getStackInHand(hand);
        if (active.isEmpty()) {
            return ActionResult.PASS;
        }
        String name = active.getName().getString();
        if (name == null) {
            return ActionResult.PASS;
        }
        Block block = namedBlocks.get(name);
        if (block == null) {
            return ActionResult.PASS;
        }
        if (hitResult.getType() != HitResult.Type.BLOCK) {
            return ActionResult.PASS;
        }
        int x = (int) hitResult.getPos().getX();
        int y = (int) hitResult.getPos().getY();
        int z = (int) hitResult.getPos().getZ();

        return world.setBlockState(new BlockPos(
                x, y, z-1
        ), block.getDefaultState())
                ? ActionResult.SUCCESS
                : ActionResult.PASS;
    }
}
