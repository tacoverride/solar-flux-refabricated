package net.tax.solarflux.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.gui.screens.MenuScreens;
import net.tax.solarflux.SolarFluxFabric;

public class SolarFluxFabricClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		MenuScreens.register(SolarFluxFabric.SOLAR_PANEL_MENU, SolarPanelScreen::new);

		HudRenderCallback.EVENT.register(SolarPanelHudOverlay::render);
	}
}