package net.soulsweaponry.entity.projectile;

import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.EntityGroup;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.soulsweaponry.registry.EntityRegistry;
import net.soulsweaponry.registry.SoundRegistry;
import net.soulsweaponry.config.ConfigConstructor;
import net.soulsweaponry.items.GunItem;
import net.soulsweaponry.registry.EffectRegistry;
import net.soulsweaponry.registry.EnchantRegistry;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animatable.instance.SingletonAnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;

public class SilverBulletEntity extends NonArrowProjectile implements GeoEntity {

    private final AnimatableInstanceCache factory = new SingletonAnimatableInstanceCache(this);

    public SilverBulletEntity(EntityType<? extends SilverBulletEntity> entityType, World world) {
        super(entityType, world);
    }

    public SilverBulletEntity(World world, LivingEntity owner) {
        super(EntityRegistry.SILVER_BULLET_ENTITY_TYPE, owner, world);
    }

    public SilverBulletEntity(EntityType<? extends SilverBulletEntity> type, double x, double y, double z,
            World world) {
        super(type, x, y, z, world);
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.inGround) {
            Vec3d vec3d = this.getVelocity();
            double e = vec3d.x;
            double f = vec3d.y;
            double g = vec3d.z;
            for (int i = 0; i < 2; ++i) {
                this.world.addParticle(ParticleTypes.SMOKE, this.getX() + e * (double)i / 4.0D, this.getY() + f * (double)i / 4.0D, this.getZ() + g * (double)i / 4.0D, -e*0.2, (-f + 0.2D)*0.2, -g*0.2);
            }
        }
        Vec3d vec3d = this.getVelocity();
        this.setVelocity(vec3d.x, vec3d.y + (double)0.045f, vec3d.z);
        if (this.age > 200 && !world.isClient) {
            this.discard();
        }
    }

    public boolean isFireImmune() {
        return true;
    }

    protected void onBlockHit(BlockHitResult blockHitResult) {
        super.onBlockHit(blockHitResult);
        this.discard(); 
    }

    @Override
    protected void onEntityHit(EntityHitResult entityHitResult) {
        if (entityHitResult.getEntity() instanceof LivingEntity && ((LivingEntity) entityHitResult.getEntity()).getGroup().equals(EntityGroup.UNDEAD)) {
            this.setDamage(this.getDamage() + ConfigConstructor.silver_bullet_undead_bonus_damage);
        }
        super.onEntityHit(entityHitResult);
        if (ConfigConstructor.can_projectiles_apply_posture_break && entityHitResult.getEntity() instanceof LivingEntity target && this.getOwner() != null && this.getOwner() instanceof PlayerEntity) {
            int random = this.random.nextInt(10);
            int chance = 2;
            int amplifier = 0;
            for (ItemStack stack : this.getOwner().getHandItems()) {
                if (stack.getItem() instanceof GunItem) {
                    chance = 2 + EnchantmentHelper.getLevel(EnchantRegistry.VISCERAL, stack);
                    amplifier = EnchantmentHelper.getLevel(EnchantRegistry.VISCERAL, stack);
                }
            }
            if (random < chance) {
                if (!target.hasStatusEffect(EffectRegistry.POSTURE_BREAK)) {
                    target.world.playSound(null, target.getBlockPos(), SoundRegistry.POSTURE_BREAK_EVENT, SoundCategory.PLAYERS, .5f, 1f);
                }
                target.addStatusEffect(new StatusEffectInstance(EffectRegistry.POSTURE_BREAK, 60, amplifier));
            }
        }
        this.discard();
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return factory;
    }

    @Override
    protected ItemStack asItemStack() {
        return null;
    }
    
}
