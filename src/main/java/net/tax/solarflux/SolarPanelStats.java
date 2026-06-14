package net.tax.solarflux;

import net.minecraft.world.level.block.Block;

public record SolarPanelStats(int tier, int generation, int transfer, int capacity) {
	public static final SolarPanelStats TIER_1 = new SolarPanelStats(1, 1, 8, 25000);
	public static final SolarPanelStats TIER_2 = new SolarPanelStats(2, 8, 64, 125000);
	public static final SolarPanelStats TIER_3 = new SolarPanelStats(3, 32, 256, 425000);
	public static final SolarPanelStats TIER_4 = new SolarPanelStats(4, 128, 1024, 2000000);
	public static final SolarPanelStats TIER_5 = new SolarPanelStats(5, 512, 4096, 8000000);
	public static final SolarPanelStats TIER_6 = new SolarPanelStats(6, 2048, 16384, 32000000);
	public static final SolarPanelStats TIER_7 = new SolarPanelStats(7, 8192, 65536, 64000000);
	public static final SolarPanelStats TIER_8 = new SolarPanelStats(8, 32768, 262144, 128000000);

	public static SolarPanelStats fromBlock(Block block) {
		if (block == SolarFluxFabric.SOLAR_PANEL_1) return TIER_1;
		if (block == SolarFluxFabric.SOLAR_PANEL_2) return TIER_2;
		if (block == SolarFluxFabric.SOLAR_PANEL_3) return TIER_3;
		if (block == SolarFluxFabric.SOLAR_PANEL_4) return TIER_4;
		if (block == SolarFluxFabric.SOLAR_PANEL_5) return TIER_5;
		if (block == SolarFluxFabric.SOLAR_PANEL_6) return TIER_6;
		if (block == SolarFluxFabric.SOLAR_PANEL_7) return TIER_7;
		if (block == SolarFluxFabric.SOLAR_PANEL_8) return TIER_8;

		return TIER_1;
	}
}