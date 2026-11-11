package dev.galaxypearl;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.mojang.brigadier.arguments.BoolArgumentType;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class GalaxyPearlClient implements ClientModInitializer {
	/** Pearl entity id -> last known squared distance to the player. */
	private final Map<Integer, Double> tracked = new HashMap<>();
	private ClientWorld lastWorld;
	private static boolean enabled = true;

	@Override
	public void onInitializeClient() {
		HudElementRegistry.attachElementAfter(
				VanillaHudElements.MISC_OVERLAYS,
				Identifier.of("galaxypearl", "overlay"),
				GalaxyOverlay::render);

		ClientTickEvents.END_CLIENT_TICK.register(mc -> {
			ClientPlayerEntity player = mc.player;
			ClientWorld world = mc.world;
			if (player == null || world == null || world != lastWorld) {
				tracked.clear();
				lastWorld = world;
				return;
			}

			// Start tracking pearls that belong to us (or spawn right next to us).
			for (Entity e : world.getEntities()) {
				if (e instanceof EnderPearlEntity pearl && !tracked.containsKey(pearl.getId())) {
					if (pearl.getOwner() == player || pearl.squaredDistanceTo(player) < 9.0) {
						tracked.put(pearl.getId(), pearl.squaredDistanceTo(player));
					}
				}
			}

			// A tracked pearl that disappeared nearby = it landed.
			Iterator<Map.Entry<Integer, Double>> it = tracked.entrySet().iterator();
			while (it.hasNext()) {
				Map.Entry<Integer, Double> entry = it.next();
				Entity e = world.getEntityById(entry.getKey());
				if (e == null || e.isRemoved()) {
					// Ignore pearls that merely left render distance (last seen very far away).
					if (enabled && entry.getValue() < 120.0 * 120.0) {
						GalaxyOverlay.trigger();
					}
					it.remove();
				} else {
					entry.setValue(e.squaredDistanceTo(player));
				}
			}
		});

		// /galaxypearl test | /galaxypearl enabled true|false
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
				dispatcher.register(ClientCommandManager.literal("galaxypearl")
						.then(ClientCommandManager.literal("test").executes(ctx -> {
							GalaxyOverlay.trigger();
							return 1;
						}))
						.then(ClientCommandManager.literal("enabled")
								.then(ClientCommandManager.argument("value", BoolArgumentType.bool())
										.executes(ctx -> {
											enabled = BoolArgumentType.getBool(ctx, "value");
											ctx.getSource().sendFeedback(Text.literal(
													"Galaxy Pearl " + (enabled ? "on" : "off")));
											return 1;
										})))));
	}
}
