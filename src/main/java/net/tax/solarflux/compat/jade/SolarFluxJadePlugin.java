package net.tax.solarflux.compat.jade;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.tax.solarflux.SolarFluxFabric;
import net.tax.solarflux.SolarPanelBlock;
import net.tax.solarflux.SolarPanelBlockEntity;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.config.IPluginConfig;

@WailaPlugin
public class SolarFluxJadePlugin implements IWailaPlugin {
	public static final ResourceLocation SOLAR_PANEL_INFO =
			new ResourceLocation(SolarFluxFabric.MOD_ID, "solar_panel_info");

	@Override
	public void register(IWailaCommonRegistration registration) {
		registration.registerBlockDataProvider(SolarPanelComponentProvider.INSTANCE, SolarPanelBlockEntity.class);
	}

	@Override
	public void registerClient(IWailaClientRegistration registration) {
		registration.registerBlockComponent(SolarPanelComponentProvider.INSTANCE, SolarPanelBlock.class);
	}

	public enum SolarPanelComponentProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
		INSTANCE;

		@Override
		public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
			CompoundTag data = accessor.getServerData();

			if (!data.contains("Energy")) {
				return;
			}

			long energy = data.getLong("Energy");
			long capacity = data.getLong("Capacity");
			int generation = data.getInt("Generation");
			int transfer = data.getInt("Transfer");
			int efficiency = data.getInt("Efficiency");

			tooltip.add(Component.literal("Stored: " + format(energy) + " / " + format(capacity) + " FE"));
			tooltip.add(Component.literal("Generation: " + format(generation) + " FE/tick"));
			tooltip.add(Component.literal("Transfer: " + format(transfer) + " FE/tick"));
			tooltip.add(Component.literal("Efficiency: " + efficiency + "%"));
		}

		@Override
		public void appendServerData(CompoundTag data, BlockAccessor accessor) {
			if (!(accessor.getBlockEntity() instanceof SolarPanelBlockEntity solarPanel)) {
				return;
			}

			data.putLong("Energy", solarPanel.getEnergy());
			data.putLong("Capacity", solarPanel.getCapacity());
			data.putInt("Generation", solarPanel.getCurrentGeneration());
			data.putInt("Transfer", solarPanel.getTransfer());
			data.putInt("Efficiency", solarPanel.getEfficiency());
		}

		@Override
		public ResourceLocation getUid() {
			return SOLAR_PANEL_INFO;
		}

		private static String format(long number) {
			return String.format("%,d", number);
		}
	}
}