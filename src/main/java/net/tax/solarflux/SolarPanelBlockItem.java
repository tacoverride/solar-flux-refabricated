package net.tax.solarflux;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;

public class SolarPanelBlockItem extends BlockItem {
	private final int generation;
	private final int transfer;
	private final int capacity;

	public SolarPanelBlockItem(Block block, Item.Properties properties, int generation, int transfer, int capacity) {
		super(block, properties);
		this.generation = generation;
		this.transfer = transfer;
		this.capacity = capacity;
	}

	@Override
	public InteractionResult place(BlockPlaceContext context) {
		ItemStack stack = context.getItemInHand();
		CompoundTag tag = stack.getTag();

		long storedEnergy = 0;
		CompoundTag copiedTag = null;

		if (tag != null) {
			copiedTag = tag.copy();

			if (tag.contains("Energy")) {
				storedEnergy = tag.getLong("Energy");
			}
		}

		InteractionResult result = super.place(context);

		if (result.consumesAction() && !context.getLevel().isClientSide) {
			Level level = context.getLevel();
			BlockPos pos = context.getClickedPos();
			BlockEntity blockEntity = level.getBlockEntity(pos);

			if (blockEntity instanceof SolarPanelBlockEntity solarPanelBlockEntity) {
				if (storedEnergy > 0) {
					solarPanelBlockEntity.restoreEnergyFromItem(storedEnergy);
				}

				if (copiedTag != null && copiedTag.contains("Upgrades")) {
					solarPanelBlockEntity.restoreUpgradesFromItemTag(copiedTag);
				}
			}
		}

		return result;
	}

	@Override
	public void appendHoverText(ItemStack stack, Level world, List<Component> tooltip, TooltipFlag context) {
		tooltip.add(Component.literal("Solar Flux [Reborn]").withStyle(ChatFormatting.BLUE));
		tooltip.add(Component.literal("Generation: " + formatNumber(generation) + " FE/tick").withStyle(ChatFormatting.GRAY));
		tooltip.add(Component.literal("Transfer: " + formatNumber(transfer) + " FE/tick").withStyle(ChatFormatting.GRAY));
		tooltip.add(Component.literal("Capacity: " + formatNumber(capacity) + " FE").withStyle(ChatFormatting.GRAY));

		CompoundTag tag = stack.getTag();

		if (tag != null && tag.contains("Energy")) {
			long energy = tag.getLong("Energy");
			tooltip.add(Component.literal("Stored: " + formatNumber(energy) + " FE").withStyle(ChatFormatting.YELLOW));
		}

		addUpgradeTooltip(tag, tooltip);
	}

	private static void addUpgradeTooltip(CompoundTag tag, List<Component> tooltip) {
		if (tag == null || !tag.contains("Upgrades")) {
			return;
		}

		CompoundTag upgradesTag = tag.getCompound("Upgrades");

		if (!upgradesTag.contains("Items")) {
			return;
		}

		ListTag items = upgradesTag.getList("Items", Tag.TAG_COMPOUND);

		if (items.isEmpty()) {
			return;
		}

		tooltip.add(Component.literal("Installed Upgrades:").withStyle(ChatFormatting.GOLD));

		for (int i = 0; i < items.size(); i++) {
			CompoundTag itemTag = items.getCompound(i);
			ItemStack upgradeStack = ItemStack.of(itemTag);

			if (!upgradeStack.isEmpty()) {
				tooltip.add(
						Component.literal("- ")
								.withStyle(ChatFormatting.DARK_GRAY)
								.append(upgradeStack.getHoverName().copy().withStyle(ChatFormatting.GRAY))
				);
			}
		}
	}

	private static String formatNumber(int number) {
		return String.format("%,d", number);
	}

	private static String formatNumber(long number) {
		return String.format("%,d", number);
	}
}