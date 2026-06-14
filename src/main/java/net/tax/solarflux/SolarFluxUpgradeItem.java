package net.tax.solarflux;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class SolarFluxUpgradeItem extends Item {
	private final String[] tooltipLines;

	public SolarFluxUpgradeItem(Properties properties, String... tooltipLines) {
		super(properties);
		this.tooltipLines = tooltipLines;
	}

	@Override
	public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
		for (String line : tooltipLines) {
			tooltip.add(Component.literal(line).withStyle(ChatFormatting.GRAY));
		}
	}
}