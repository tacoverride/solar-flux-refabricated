package net.tax.solarflux;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

public class BlockChargingUpgradeItem extends SolarFluxUpgradeItem {
	public static final String LINK_X = "LinkedX";
	public static final String LINK_Y = "LinkedY";
	public static final String LINK_Z = "LinkedZ";
	public static final String LINK_DIMENSION = "LinkedDimension";

	public BlockChargingUpgradeItem(Properties properties) {
		super(
				properties,
				"Wirelessly transmits energy to a linked block.",
				"Range: 16 blocks."
		);
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		Level level = context.getLevel();

		if (level.isClientSide) {
			return InteractionResult.SUCCESS;
		}

		BlockPos pos = context.getClickedPos();
		BlockEntity blockEntity = level.getBlockEntity(pos);

		if (blockEntity == null) {
			if (context.getPlayer() != null) {
				context.getPlayer().displayClientMessage(
						Component.literal("No block entity found to link.").withStyle(ChatFormatting.RED),
						true
				);
			}

			return InteractionResult.FAIL;
		}

		ItemStack stack = context.getItemInHand();
		CompoundTag tag = stack.getOrCreateTag();

		tag.putInt(LINK_X, pos.getX());
		tag.putInt(LINK_Y, pos.getY());
		tag.putInt(LINK_Z, pos.getZ());
		tag.putString(LINK_DIMENSION, level.dimension().location().toString());

		if (context.getPlayer() != null) {
			context.getPlayer().displayClientMessage(
					Component.literal("Linked to block at " + pos.getX() + ", " + pos.getY() + ", " + pos.getZ())
							.withStyle(ChatFormatting.GREEN),
					true
			);
		}

		return InteractionResult.SUCCESS;
	}

	@Override
	public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
		super.appendHoverText(stack, level, tooltip, flag);

		CompoundTag tag = stack.getTag();

		if (isLinked(stack)) {
			tooltip.add(Component.literal(
					"Linked: " + tag.getInt(LINK_X) + ", " + tag.getInt(LINK_Y) + ", " + tag.getInt(LINK_Z)
			).withStyle(ChatFormatting.YELLOW));

			tooltip.add(Component.literal(
					"Dimension: " + tag.getString(LINK_DIMENSION)
			).withStyle(ChatFormatting.DARK_GRAY));
		} else {
			tooltip.add(Component.literal("Not linked.").withStyle(ChatFormatting.RED));
			tooltip.add(Component.literal("Right-click a machine before installing.").withStyle(ChatFormatting.GRAY));
		}
	}

	public static boolean isLinked(ItemStack stack) {
		CompoundTag tag = stack.getTag();

		return tag != null
				&& tag.contains(LINK_X)
				&& tag.contains(LINK_Y)
				&& tag.contains(LINK_Z)
				&& tag.contains(LINK_DIMENSION);
	}

	public static BlockPos getLinkedPos(ItemStack stack) {
		CompoundTag tag = stack.getTag();

		if (tag == null) {
			return BlockPos.ZERO;
		}

		return new BlockPos(
				tag.getInt(LINK_X),
				tag.getInt(LINK_Y),
				tag.getInt(LINK_Z)
		);
	}

	public static String getLinkedDimension(ItemStack stack) {
		CompoundTag tag = stack.getTag();

		if (tag == null) {
			return "";
		}

		return tag.getString(LINK_DIMENSION);
	}
}