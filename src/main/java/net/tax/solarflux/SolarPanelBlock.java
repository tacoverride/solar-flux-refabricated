package net.tax.solarflux;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class SolarPanelBlock extends Block implements EntityBlock {
	private static final VoxelShape SHAPE = Block.box(0.0, 0.0, 0.0, 16.0, 6.5, 16.0);

	public SolarPanelBlock(Properties properties) {
		super(properties);
	}

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new SolarPanelBlockEntity(pos, state);
	}

	@Override
	public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
		if (!level.isClientSide) {
			BlockEntity blockEntity = level.getBlockEntity(pos);

			if (blockEntity instanceof MenuProvider menuProvider && player instanceof ServerPlayer serverPlayer) {
				serverPlayer.openMenu(menuProvider);
			}
		}

		return InteractionResult.sidedSuccess(level.isClientSide);
	}

	@Override
	public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
		List<ItemStack> drops = super.getDrops(state, builder);
		BlockEntity blockEntity = builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY);

		if (blockEntity instanceof SolarPanelBlockEntity solarPanelBlockEntity) {
			long energy = solarPanelBlockEntity.getEnergy();

			if (energy > 0) {
				for (ItemStack stack : drops) {
					if (stack.is(this.asItem())) {
						stack.getOrCreateTag().putLong("Energy", energy);
					}
				}
			}
		}

		return drops;
	}

	@Override
	public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
		if (!state.is(newState.getBlock())) {
			BlockEntity blockEntity = level.getBlockEntity(pos);

			if (blockEntity instanceof SolarPanelBlockEntity solarPanelBlockEntity) {
				// Drop installed upgrades separately when the panel breaks.
				Containers.dropContents(level, pos, solarPanelBlockEntity);
				level.updateNeighbourForOutputSignal(pos, this);
			}
		}

		super.onRemove(state, level, pos, newState, movedByPiston);
	}

	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		if (level.isClientSide) {
			return null;
		}

		return (tickLevel, tickPos, tickState, blockEntity) -> {
			if (blockEntity instanceof SolarPanelBlockEntity solarPanelBlockEntity) {
				SolarPanelBlockEntity.tick(tickLevel, tickPos, tickState, solarPanelBlockEntity);
			}
		};
	}
}