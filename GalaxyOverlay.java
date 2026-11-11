package dev.galaxypearl;

import java.util.Random;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvents;

/** Full-screen spiral galaxy + starfield that fades in, spins and zooms, then fades out. */
public final class GalaxyOverlay {
	/** Total length in seconds. Kept short so it doesn't block your view for long. */
	private static final float DURATION = 1.8f;
	private static final float FADE_IN = 0.30f;
	private static final float FADE_OUT = 0.60f;

	private static final int STARS = 260;
	private static final int GALAXY_POINTS = 900;

	private static final float[] starX = new float[STARS];
	private static final float[] starY = new float[STARS];
	private static final float[] starPhase = new float[STARS];

	private static final float[] gR = new float[GALAXY_POINTS];
	private static final float[] gAngle = new float[GALAXY_POINTS];
	private static final int[] gColor = new int[GALAXY_POINTS];
	private static final int[] gSize = new int[GALAXY_POINTS];

	private static boolean initialised = false;
	private static long startNanos = -1;

	private GalaxyOverlay() {}

	public static void trigger() {
		if (!initialised) init();
		startNanos = System.nanoTime();
		MinecraftClient mc = MinecraftClient.getInstance();
		mc.getSoundManager().play(
				PositionedSoundInstance.master(SoundEvents.BLOCK_PORTAL_TRAVEL, 1.3f, 0.35f));
	}

	private static void init() {
		Random rng = new Random(42);
		for (int i = 0; i < STARS; i++) {
			starX[i] = rng.nextFloat();
			starY[i] = rng.nextFloat();
			starPhase[i] = rng.nextFloat() * 6.2832f;
		}
		for (int i = 0; i < GALAXY_POINTS; i++) {
			float r = (float) Math.pow(rng.nextFloat(), 1.6); // denser toward the core
			int arm = i % 3;
			float scatter = (float) rng.nextGaussian() * (0.10f + 0.25f * (1f - r));
			gR[i] = r;
			gAngle[i] = arm * 2.0944f + r * 5.5f + scatter;
			gSize[i] = r < 0.2f ? 2 : (rng.nextFloat() < 0.15f ? 2 : 1);
			gColor[i] = r < 0.15f ? 0xFFF2C0
					: r < 0.45f ? 0xFF9BE8
					: r < 0.75f ? 0x9B7BFF
					: 0x5BA8FF;
		}
		initialised = true;
	}

	public static void render(DrawContext ctx, RenderTickCounter tickCounter) {
		if (startNanos < 0) return;
		float t = (System.nanoTime() - startNanos) / 1e9f;
		if (t >= DURATION) {
			startNanos = -1;
			return;
		}

		float alpha = t < FADE_IN ? smooth(t / FADE_IN)
				: t > DURATION - FADE_OUT ? smooth((DURATION - t) / FADE_OUT)
				: 1f;

		int w = ctx.getScaledWindowWidth();
		int h = ctx.getScaledWindowHeight();
		float cx = w / 2f, cy = h / 2f;
		float progress = t / DURATION;
		float zoom = 0.5f + 1.1f * (1f - (float) Math.pow(1f - progress, 3)); // ease-out zoom

		// Deep-space background.
		ctx.fill(0, 0, w, h, argb((int) (245 * alpha), 0x05030F));

		// Twinkling starfield that drifts outward from the center.
		for (int i = 0; i < STARS; i++) {
			float dx = (starX[i] - 0.5f) * w * (0.8f + 0.7f * zoom);
			float dy = (starY[i] - 0.5f) * h * (0.8f + 0.7f * zoom);
			float tw = 0.5f + 0.5f * (float) Math.sin(t * 5f + starPhase[i]);
			int a = (int) (255 * alpha * (0.35f + 0.65f * tw));
			int x = (int) (cx + dx), y = (int) (cy + dy);
			ctx.fill(x, y, x + 1, y + 1, argb(a, 0xFFFFFF));
		}

		// Spiral galaxy: tilted, rotating faster near the core.
		float maxR = Math.min(w, h) * 0.55f * zoom;
		float tilt = -0.35f;
		float cosT = (float) Math.cos(tilt), sinT = (float) Math.sin(tilt);
		for (int i = 0; i < GALAXY_POINTS; i++) {
			float r = gR[i];
			float ang = gAngle[i] + t * (1.8f - 1.2f * r);
			float R = r * maxR;
			float px = (float) Math.cos(ang) * R;
			float py = (float) Math.sin(ang) * R * 0.5f; // flatten into a disc
			int x = (int) (cx + px * cosT - py * sinT);
			int y = (int) (cy + px * sinT + py * cosT);
			float fade = 1f - 0.5f * r;
			int a = (int) (255 * alpha * fade);
			int s = gSize[i];
			ctx.fill(x, y, x + s, y + s, argb(a, gColor[i]));
		}
	}

	private static float smooth(float x) {
		x = Math.max(0f, Math.min(1f, x));
		return x * x * (3f - 2f * x);
	}

	private static int argb(int a, int rgb) {
		return (Math.min(255, Math.max(0, a)) << 24) | (rgb & 0xFFFFFF);
	}
}
