package net.soulsweaponry.entity.mobs;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityGroup;
import net.minecraft.entity.EntityStatuses;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.RevengeGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.WitherSkeletonEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import net.soulsweaponry.config.ConfigConstructor;
import net.soulsweaponry.entity.ai.goal.AccursedLordGoal;
import net.soulsweaponry.registry.ItemRegistry;
import net.soulsweaponry.networking.PacketRegistry;
import net.soulsweaponry.registry.SoundRegistry;
import net.soulsweaponry.registry.WeaponRegistry;
import net.soulsweaponry.util.CustomDeathHandler;
import net.soulsweaponry.util.ParticleNetworking;
import software.bernie.geckolib3.core.IAnimatable;
import software.bernie.geckolib3.core.IAnimationTickable;
import software.bernie.geckolib3.core.PlayState;
import software.bernie.geckolib3.core.builder.AnimationBuilder;
import software.bernie.geckolib3.core.controller.AnimationController;
import software.bernie.geckolib3.core.event.predicate.AnimationEvent;
import software.bernie.geckolib3.core.manager.AnimationData;
import software.bernie.geckolib3.core.manager.AnimationFactory;
import software.bernie.geckolib3.util.GeckoLibUtil;

public class AccursedLordBoss extends BossEntity implements IAnimatable, IAnimationTickable {

    public AnimationFactory factory = GeckoLibUtil.createFactory(this);
    public int deathTicks;
    private int spawnTicks;
    private static final TrackedData<Integer> ATTACKS = DataTracker.registerData(AccursedLordBoss.class, TrackedDataHandlerRegistry.INTEGER);
    public ArrayList<BlockPos> lavaPos = new ArrayList<>();

    public AccursedLordBoss(EntityType<? extends AccursedLordBoss> entityType, World world) {
        super(entityType, world, BossBar.Color.RED);
        this.setDrops(ItemRegistry.LORD_SOUL_RED);
        this.setDrops(ItemRegistry.WITHERED_DEMON_HEART);
        this.setDrops(WeaponRegistry.DARKIN_BLADE);
    }

    public boolean isFireImmune() {
        return true;
    }

    private <E extends IAnimatable> PlayState attackAnimations(AnimationEvent<E> event) {
        switch (this.getAttackAnimation()) {
            case FIREBALLS, WITHERBALLS ->
                    event.getController().setAnimation(new AnimationBuilder().addAnimation("animation.model.shootFireMouth"));
            case HAND_SLAM ->
                    event.getController().setAnimation(new AnimationBuilder().addAnimation("animation.model.groundSlamHand"));
            case HEATWAVE ->
                    event.getController().setAnimation(new AnimationBuilder().addAnimation("animation.model.explosion"));
            case PULL ->
                    event.getController().setAnimation(new AnimationBuilder().addAnimation("animation.model.pull"));
            case SPIN ->
                    event.getController().setAnimation(new AnimationBuilder().addAnimation("animation.model.spin"));
            case SWORDSLAM ->
                    event.getController().setAnimation(new AnimationBuilder().addAnimation("animation.model.swordSlam"));
            case DEATH ->
                    event.getController().setAnimation(new AnimationBuilder().addAnimation("animation.model.death"));
            case SPAWN ->
                    event.getController().setAnimation(new AnimationBuilder().addAnimation("animation.model.spawn"));
            default -> event.getController().setAnimation(new AnimationBuilder().addAnimation("animation.model.idle"));
        }
        return PlayState.CONTINUE;
    }

    @Override
    public int getTicksUntilDeath() {
        return 150;
    }

    @Override
    public int getDeathTicks() {
        return this.deathTicks;
    }
    
    @Override
    public void updatePostDeath() {
        this.deathTicks++;
        if (this.deathTicks >= this.getTicksUntilDeath() && !this.world.isClient) {
            this.world.sendEntityStatus(this, EntityStatuses.ADD_DEATH_PARTICLES);
            CustomDeathHandler.deathExplosionEvent(world, this.getBlockPos(), false, SoundRegistry.DAWNBREAKER_EVENT);
            this.remove(Entity.RemovalReason.KILLED);
        }
    }

    @Override
	protected void initGoals() {
        this.goalSelector.add(1, new AccursedLordGoal(this));
        this.goalSelector.add(8, new LookAtEntityGoal(this, PlayerEntity.class, 8.0F));
        this.goalSelector.add(8, new LookAroundGoal(this));
        this.targetSelector.add(1, new ActiveTargetGoal<>(this, ChaosMonarch.class, true));
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, true));
        this.targetSelector.add(3, new ActiveTargetGoal<>(this, WitherSkeletonEntity.class, true));
        this.targetSelector.add(4, new ActiveTargetGoal<>(this, WitherEntity.class, true));
        this.targetSelector.add(5, (new RevengeGoal(this)).setGroupRevenge());
		super.initGoals();
	}

    public static DefaultAttributeContainer.Builder createDemonAttributes() {
        return HostileEntity.createHostileAttributes()
        .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 60D)
        .add(EntityAttributes.GENERIC_MAX_HEALTH, ConfigConstructor.decaying_king_health)
        .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.15D)
        .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 20.0D)
        .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 1.0D)
        .add(EntityAttributes.GENERIC_ARMOR, 5.0D)
        .add(EntityAttributes.GENERIC_ATTACK_KNOCKBACK, 2.0D);
    }

    protected void initDataTracker() {
        super.initDataTracker();
        this.dataTracker.startTracking(ATTACKS, 9);
    }

    /**
     * Set attack with enum as parameter
     */
    public void setAttackAnimation(AccursedLordAnimations attack) {
        for (int i = 0; i < AccursedLordAnimations.values().length; i++) {
            if (AccursedLordAnimations.values()[i].equals(attack)) {
                this.dataTracker.set(ATTACKS, i);
            }
        }
    }

    /**
     * Set attack with an integer to easily set the data tracker value
     */
    public void setAttackAnimation(int random) {
        this.dataTracker.set(ATTACKS, random);
    }

    public void tickMovement() {
        super.tickMovement();

        if (this.getAttackAnimation().equals(AccursedLordAnimations.SPAWN)) {
            this.spawnTicks++;
            
            for(int i = 0; i < 50; ++i) {
                Random random = this.getRandom();
                BlockPos pos = this.getBlockPos();
                double d = random.nextGaussian() * 0.05D;
                double e = random.nextGaussian() * 0.05D;
                double newX = random.nextDouble() - 0.5D + random.nextGaussian() * 0.15D + d;
                double newZ = random.nextDouble() - 0.5D + random.nextGaussian() * 0.15D + e;
                double newY = random.nextDouble() - 0.5D + random.nextDouble() * 0.5D;
                world.addParticle(ParticleTypes.FLAME, pos.getX(), pos.getY(), pos.getZ(), newX/2, newY/6, newZ/2);
                world.addParticle(ParticleTypes.LARGE_SMOKE, pos.getX(), pos.getY(), pos.getZ(), newX/2, newY/6, newZ/2);
            }
            
            if (this.spawnTicks % 10 == 0 && this.spawnTicks < 70) {
                this.world.playSound(null, this.getBlockPos(), SoundEvents.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, SoundCategory.HOSTILE, 1f, 1f);
            }
            if (this.spawnTicks > 110 && this.spawnTicks <= 112) {
                this.world.playSound(null, this.getBlockPos(), SoundRegistry.DAWNBREAKER_EVENT, SoundCategory.HOSTILE, 1f, 1f);
                Box chunkBox = new Box(this.getBlockPos()).expand(5);
                List<Entity> nearbyEntities = this.world.getOtherEntities(this, chunkBox);
                for (Entity nearbyEntity : nearbyEntities) {
                    if (nearbyEntity instanceof LivingEntity closestTarget) {
                        double x = closestTarget.getX() - (this.getX());
                        double z = closestTarget.getZ() - this.getZ();
                        closestTarget.takeKnockback(10F, -x, -z);
                        closestTarget.damage(DamageSource.mob(this), 50f * ConfigConstructor.decaying_king_damage_modifier);
                    }
                }
                if (!this.world.isClient) ParticleNetworking.sendServerParticlePacket((ServerWorld) this.world, PacketRegistry.DAWNBREAKER_PACKET_ID, this.getBlockPos());
            }
            if (this.spawnTicks >= 125) {
                this.setAttackAnimation(AccursedLordAnimations.IDLE);
            }
        }
    }

    public void removePlacedLava() {
        for (BlockPos pos : this.lavaPos) {
            if (this.world.getBlockState(pos).isOf(Blocks.LAVA)) {
                this.world.setBlockState(pos, Blocks.AIR.getDefaultState());
            }
        }
        this.lavaPos.clear();
    }

    public AccursedLordAnimations getAttackAnimation() {
        return AccursedLordAnimations.values()[this.dataTracker.get(ATTACKS)];
    }

    @Override
    public void setDeath() {
        this.setAttackAnimation(AccursedLordAnimations.DEATH);
        this.removePlacedLava();
    }

    @Override
    public boolean isUndead() {
        return true;
    }

    @Override
    public EntityGroup getGroup() {
        return EntityGroup.UNDEAD;
    }
    
    public boolean disablesShield() {
        return true;
    }

    @Override
    public double getBossMaxHealth() {
        return ConfigConstructor.decaying_king_health;
    }

    @Override
    public int tickTimer() {
        return age;
    }

    @Override
    public void registerControllers(AnimationData data) {
        data.addAnimationController(new AnimationController<>(this, "attacks", 0, this::attackAnimations));    
    }

    @Override
    public AnimationFactory getFactory() {
        return this.factory;
    }

    protected SoundEvent getAmbientSound() {
        return SoundRegistry.DEMON_BOSS_IDLE_EVENT;
    }
  
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundRegistry.DEMON_BOSS_HURT_EVENT;
    }
  
    protected SoundEvent getDeathSound() {
        return SoundRegistry.DEMON_BOSS_DEATH_EVENT;
    }

    public enum AccursedLordAnimations {
        SWORDSLAM,
        FIREBALLS,
        PULL,
        HEATWAVE,
        SPIN,
        WITHERBALLS,
        HAND_SLAM,
        SPAWN,
        DEATH,
        IDLE
    }
}
