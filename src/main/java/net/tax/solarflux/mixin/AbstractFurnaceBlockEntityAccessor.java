package net.tax.solarflux.mixin;

import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AbstractFurnaceBlockEntity.class)
public interface AbstractFurnaceBlockEntityAccessor {
	@Accessor("litTime")
	void solarflux$setLitTime(int litTime);

	@Accessor("litDuration")
	void solarflux$setLitDuration(int litDuration);
}