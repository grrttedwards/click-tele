package com.garrettedwards.clicktele;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.command.CommandManager;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.garrettedwards.clicktele.Util.*;
import static com.garrettedwards.clicktele.Util.IsPlayerHeightOpenSpace;

public class ClickTele implements ModInitializer {

    private static final Logger LOGGER = LogManager.getLogger();
    private Duration COOLDOWN_LENGTH = Duration.ofSeconds(10);

    // player UUID string to last use
    private final Map<String, Instant> cooldowns = new ConcurrentHashMap<>();

    @Override
    public void onInitialize() {
        UseItemCallback.EVENT.register(this::onRightClickAir);
        CommandRegistrationCallback.EVENT.register((dispatcher, dedicated) -> dispatcher.register(
                CommandManager.literal("ctcd")
                        .requires(serverCommandSource -> serverCommandSource.hasPermissionLevel(4))
                        .then(CommandManager.argument("seconds", IntegerArgumentType.integer())
                                .executes(context -> {
                                    Integer seconds = context.getArgument("seconds", int.class);
                                    COOLDOWN_LENGTH = Duration.ofSeconds(seconds);
                                    return Command.SINGLE_SUCCESS;
                                }))
        ));
    }

    private boolean OnCooldown(PlayerEntity player) {
        Instant lastUse = cooldowns.getOrDefault(player.getUuidAsString(), Instant.MIN);
        if (lastUse.isAfter(Instant.now().minus(COOLDOWN_LENGTH))) {
            Duration remainingCooldown = COOLDOWN_LENGTH.minus(Duration.between(lastUse, Instant.now()));
            long remainingCooldownSeconds = remainingCooldown.getSeconds() + 1;  // so it doesn't say "try in 0 seconds"
            player.sendMessage(new LiteralText("Try again in " + remainingCooldownSeconds + " seconds"), true);
            return true;
        }
        return false;
    }

    private TypedActionResult<ItemStack> onRightClickAir(PlayerEntity player, World world, Hand hand) {
        ItemStack itemHeld = player.getStackInHand(hand);
        if (world.isClient() ||
                hand == Hand.OFF_HAND ||
                !itemHeld.getTranslationKey().equals("item.minecraft.compass")) {
            return TypedActionResult.pass(itemHeld);
        }

        BlockHitResult raycast = (BlockHitResult) player.raycast(80, 0.2f, false);
        if (raycast == null || raycast.getType() == HitResult.Type.MISS) {
            player.sendMessage(new LiteralText("Target too far..."), true);
            return TypedActionResult.pass(itemHeld);
        }

        BlockPos adjustedPosition = new BlockPos(raycast.getBlockPos()).up();
        do {
            if (IsPlayerHeightOpenSpace(world, adjustedPosition)) {
                if (OnCooldown(player)) {
                    return TypedActionResult.pass(itemHeld);
                }
                TeleportPlayer(player, adjustedPosition);
                cooldowns.put(player.getUuidAsString(), Instant.now());
                LOGGER.info("[{}: ClickTele'd to {}]", player.getDisplayName().asString(), GetCoordString(adjustedPosition));
                return TypedActionResult.success(itemHeld);
            }
            adjustedPosition = adjustedPosition.up();
        } while (adjustedPosition.getY() <= 256);

        // couldn't place them in any open space
        return TypedActionResult.pass(itemHeld);
    }
}
