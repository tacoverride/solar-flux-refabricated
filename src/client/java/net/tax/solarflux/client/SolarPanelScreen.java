package net.tax.solarflux.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.tax.solarflux.SolarFluxFabric;
import net.tax.solarflux.SolarPanelMenu;

public class SolarPanelScreen extends AbstractContainerScreen<SolarPanelMenu> {
	private static final ResourceLocation TEXTURE =
			new ResourceLocation(SolarFluxFabric.MOD_ID, "textures/gui/solar.png");
	private static final ResourceLocation ELEMENTS =
			new ResourceLocation(SolarFluxFabric.MOD_ID, "textures/gui/elements.png");

	private static final int TEX_SIZE = 256;

	public SolarPanelScreen(SolarPanelMenu menu, Inventory playerInventory, Component title) {
		super(menu, playerInventory, title);
		this.imageWidth = 176;
		this.imageHeight = 180;
		this.inventoryLabelY = this.imageHeight - 96 + 2; // 86
	}

	@Override
	protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
		int x = this.leftPos;
		int y = this.topPos;

		// Base background
		graphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight, TEX_SIZE, TEX_SIZE);

		// 5 upgrade slot backgrounds
		for (int i = 0; i < 5; i++) {
			graphics.blit(ELEMENTS, x + 8 + i * 18, y + 60, 18, 0, 18, 18, TEX_SIZE, TEX_SIZE);
		}

		// Top-right lightning slot (includes the bolt icon in the official texture)
		graphics.blit(ELEMENTS, x + 150, y + 8, 18, 18, 18, 18, TEX_SIZE, TEX_SIZE);

		// Left bar = sun/generation
		drawSunBar(graphics, x + 128, y + 40, menu.getGenerationBarHeight());

		// Right bar = stored power
		drawPowerBar(graphics, x + 150, y + 40, menu.getEnergyBarHeight());
	}

	private void drawPowerBar(GuiGraphics graphics, int x, int y, int fillHeight) {
		int filled = Mth.clamp(fillHeight, 0, 48);
		int empty = 48 - filled;

		// Gray inner background
		graphics.blit(ELEMENTS, x + 1, y + 1, 16, 64, 16, 48, TEX_SIZE, TEX_SIZE);

		// Colored fill
		if (filled > 0) {
			graphics.blit(ELEMENTS, x + 1, y + 1 + empty, 0, 64 + empty, 16, filled, TEX_SIZE, TEX_SIZE);
		}

		// Gray frame overlay
		graphics.blit(ELEMENTS, x, y, 64, 62, 18, 50, TEX_SIZE, TEX_SIZE);
	}

	private void drawSunBar(GuiGraphics graphics, int x, int y, int fillHeight) {
		int filled = Mth.clamp(fillHeight, 0, 48);
		int empty = 48 - filled;

		// Gray inner background
		graphics.blit(ELEMENTS, x + 1, y + 1, 48, 64, 16, 48, TEX_SIZE, TEX_SIZE);

		// Colored fill
		if (filled > 0) {
			graphics.blit(ELEMENTS, x + 1, y + 1 + empty, 32, 64 + empty, 16, filled, TEX_SIZE, TEX_SIZE);
		}

		// Gray frame overlay
		graphics.blit(ELEMENTS, x, y, 64, 62, 18, 50, TEX_SIZE, TEX_SIZE);
	}

	@Override
	protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
		graphics.drawString(this.font, this.title, 8, 4, 0x404040, false);

		PoseStack pose = graphics.pose();
		pose.pushPose();
		pose.translate(8.0F, 14.0F, 0.0F);
		pose.scale(0.9F, 0.9F, 0.9F);

		graphics.drawString(this.font, "Stored: " + format(menu.getEnergy()) + " FE", 0, 0, 0x404040, false);
		graphics.drawString(this.font, "Capacity: " + format(menu.getCapacity()) + " FE", 0, 10, 0x404040, false);
		graphics.drawString(this.font, "Generation: " + format(menu.getCurrentGeneration()) + " FE/tick", 0, 20, 0x404040, false);
		graphics.drawString(this.font, "Efficiency: " + menu.getEfficiency() + "%", 0, 30, 0x404040, false);

		pose.popPose();

		graphics.drawString(this.font, "Inventory", 8, this.inventoryLabelY, 0x404040, false);
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
		this.renderBackground(graphics);
		super.render(graphics, mouseX, mouseY, delta);
		this.renderTooltip(graphics, mouseX, mouseY);
	}

	private static String format(int number) {
		return String.format("%,d", number);
	}
}