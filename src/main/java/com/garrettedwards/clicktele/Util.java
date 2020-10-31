package com.garrettedwards.clicktele;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class Util {

    public static void TeleportPlayer(PlayerEntity player, BlockPos blockPos) {
        // prevent fall damage
        player.fallDistance = 0;
        // adjust for the whole coordinates being the corner of a block
        player.teleport(blockPos.getX() + 0.5, blockPos.getY(), blockPos.getZ() + 0.5, true);
        player.sendMessage(new LiteralText("Poof!"), true);
    }

    public static boolean IsPlayerHeightOpenSpace(World world, BlockPos position) {
        return world.getBlockState(position).isAir() && world.getBlockState(position.up()).isAir();
    }

    public static String GetCoordString(BlockPos position)
    {
        return position.getX() + ", " + position.getY() + ", " + position.getZ();
    }
}
