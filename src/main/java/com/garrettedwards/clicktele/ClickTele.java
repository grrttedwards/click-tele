package com.garrettedwards.clicktele;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.garrettedwards.clicktele.Util.*;

public class ClickTele implements ModInitializer {

    private static final Logger LOGGER = LogManager.getLogger();

    // map of player UUID string to last use time
    private final Map<String, Instant> cooldowns = new ConcurrentHashMap<>();

    private Config config;

    public Path workingDir;

    @Override
    public void onInitialize() {
        // Setup working directory
        workingDir = FabricLoader.getInstance().getConfigDir().resolve("clicktele");
        if (!Files.exists(workingDir)) {
            try {
                Files.createDirectory(workingDir);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        config = new Config(workingDir);

        ServerLifecycleEvents.SERVER_STARTED.register(this::onStartServer);
        UseItemCallback.EVENT.register(this::onHeldItemUsed);
        CommandRegistrationCallback.EVENT.register((dispatcher, unused) -> Commands.registerSetCooldownCommand(dispatcher, config));
        CommandRegistrationCallback.EVENT.register((dispatcher, unused) -> Commands.registerSetDistanceCommand(dispatcher, config));
    }

    private void onStartServer(MinecraftServer minecraftServer) {
        config.load();
    }

    private boolean IsOnCooldown(PlayerEntity player) {
        Instant lastUse = cooldowns.getOrDefault(player.getUuidAsString(), Instant.MIN);
        if (lastUse.isAfter(Instant.now().minus(config.getCooldownSeconds()))) {
            Duration remainingCooldown = config.getCooldownSeconds().minus(Duration.between(lastUse, Instant.now()));
            long remainingCooldownSeconds = remainingCooldown.getSeconds() + 1;  // so it doesn't say "try in 0 seconds"
            player.sendMessage(new LiteralText("Try again in " + remainingCooldownSeconds + " seconds"), true);
            return true;
        }
        return false;
    }

    private boolean ShouldSkip(World world, Hand hand, ItemStack itemHeld) {
        return world.isClient() ||
                hand == Hand.OFF_HAND ||
                !itemHeld.getTranslationKey().equals("item.minecraft.compass");
    }

    private TypedActionResult<ItemStack> onHeldItemUsed(PlayerEntity player, World world, Hand hand) {
        ItemStack itemHeld = player.getStackInHand(hand);
        if (ShouldSkip(world, hand, itemHeld)) {
            return TypedActionResult.pass(itemHeld);
        }

        BlockHitResult raycast = (BlockHitResult) player.raycast(config.getMaxDistance(), 0.2f, false);
        if (raycast == null || raycast.getType() == HitResult.Type.MISS) {
            player.sendMessage(new LiteralText("Target too far..."), true);
            return TypedActionResult.pass(itemHeld);
        }

        BlockPos adjustedPosition = new BlockPos(raycast.getBlockPos()).up();
        do {
            if (IsPlayerHeightOpenSpace(world, adjustedPosition)) {
                if (IsOnCooldown(player)) {
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
