package net.tax.solarflux.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.tax.solarflux.SolarFluxFabric;

public class SolarPanelHudOverlay {
	public static void render(GuiGraphics graphics, float tickDelta) {
		Minecraft minecraft = Minecraft.getInstance();

		if (minecraft.level == null || minecraft.player == null || minecraft.hitResult == null) {
			return;
		}

		if (minecraft.hitResult.getType() != HitResult.Type.BLOCK) {
			return;
		}

		BlockHitResult blockHitResult = (BlockHitResult) minecraft.hitResult;
		BlockPos pos = blockHitResult.getBlockPos();
		Level level = minecraft.level;
		Block block = level.getBlockState(pos).getBlock();

		if (!isSolarPanel(block)) {
			return;
		}

		int lightLevel = getLightLevelPercent(level, pos);

		int screenWidth = minecraft.getWindow().getGuiScaledWidth();
		int screenHeight = minecraft.getWindow().getGuiScaledHeight();

		int barWidth = 112;
		int barHeight = 12;

		int x = screenWidth / 2 + 18;
		int y = screenHeight / 2 + 8;

		String title = "Light Level:";
		String percent = lightLevel + "%";

		if (x + barWidth > screenWidth - 6) {
			x = screenWidth - barWidth - 6;
		}

		graphics.drawString(minecraft.font, title, x, y - 13, 0xFFFFFFFF, false);

		// darker border so it separates better from the world
		graphics.fill(x - 1, y - 1, x + barWidth + 1, y + barHeight + 1, 0xFF8E8E8E);

		// empty background
		graphics.fill(x, y, x + barWidth, y + barHeight, 0xFF3C3C3C);

		// 1-pixel yellow/orange stripes
		int fillWidth = barWidth * lightLevel / 100;
		int stripeWidth = 1;

		for (int stripeX = 0; stripeX < fillWidth; stripeX += stripeWidth) {
			int stripeEnd = Math.min(stripeX + stripeWidth, fillWidth);

			int color = ((stripeX / stripeWidth) % 2 == 0)
					? 0xFFE1B83B
					: 0xFF864F08;

			graphics.fill(x + stripeX, y, x + stripeEnd, y + barHeight, color);
		}

		// subtle dark backing under percentage text
		int textX = x + 4;
		int textY = y + 2;
		int textWidth = minecraft.font.width(percent);

		graphics.fill(textX - 1, textY - 1, textX + textWidth + 1, textY + 9, 0x66000000);

		graphics.drawString(minecraft.font, percent, textX, textY, 0xFFFFFFFF, false);
	}

	private static int getLightLevelPercent(Level level, BlockPos pos) {
		boolean canSeeSky = level.canSeeSky(pos.above());
		boolean isDay = level.isDay();

		if (canSeeSky && isDay) {
			return 100;
		}

		return 0;
	}

	private static boolean isSolarPanel(Block block) {
		return block == SolarFluxFabric.SOLAR_PANEL_1
				|| block == SolarFluxFabric.SOLAR_PANEL_2
				|| block == SolarFluxFabric.SOLAR_PANEL_3
				|| block == SolarFluxFabric.SOLAR_PANEL_4
				|| block == SolarFluxFabric.SOLAR_PANEL_5
				|| block == SolarFluxFabric.SOLAR_PANEL_6
				|| block == SolarFluxFabric.SOLAR_PANEL_7
				|| block == SolarFluxFabric.SOLAR_PANEL_8;
	}
}