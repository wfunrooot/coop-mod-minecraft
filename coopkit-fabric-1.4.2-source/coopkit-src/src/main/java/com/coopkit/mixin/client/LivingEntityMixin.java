package com.coopkit.mixin.client;

import com.coopkit.client.ClientBhop;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
abstract class LivingEntityMixin {
    @Inject(method = "jumpFromGround", at = @At("TAIL"))
    private void coopkit$boostJump(CallbackInfo ci) {
        if ((Object) this instanceof LocalPlayer player) ClientBhop.onJump(player);
    }
}