package com.nisovin.magicspells.spelleffects;

import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.Location;
import org.bukkit.util.Vector;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.LivingEntity;
import org.bukkit.configuration.ConfigurationSection;

import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.SpellData;
import com.nisovin.magicspells.util.TimeUtil;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.util.config.ConfigDataUtil;
import com.nisovin.magicspells.castmodifiers.ModifierSet;
import com.nisovin.magicspells.spelleffects.trackers.BuffTracker;
import com.nisovin.magicspells.spelleffects.trackers.OrbitTracker;
import com.nisovin.magicspells.spelleffects.trackers.BuffEffectlibTracker;
import com.nisovin.magicspells.spelleffects.trackers.OrbitEffectlibTracker;

import de.slikey.effectlib.Effect;
import de.slikey.effectlib.util.VectorUtils;

public abstract class SpellEffect {

	private final Random random = ThreadLocalRandom.current();

	private ConfigData<Integer> delay;

	private ConfigData<Double> chance;
	private ConfigData<Double> zOffset;
	private ConfigData<Double> heightOffset;
	private ConfigData<Double> forwardOffset;

	private Vector offset;
	private Vector relativeOffset;

	// for line
	private ConfigData<Double> distanceBetween;
	private ConfigData<Double> maxDistanceSquared;

	// for buff/orbit
	private ConfigData<Float> orbitXAxis;
	private ConfigData<Float> orbitYAxis;
	private ConfigData<Float> orbitZAxis;
	private ConfigData<Float> orbitRadius;
	private ConfigData<Float> orbitYOffset;
	private ConfigData<Float> horizOffset;
	private ConfigData<Float> horizExpandRadius;
	private ConfigData<Float> vertExpandRadius;
	private ConfigData<Float> secondsPerRevolution;

	private ConfigData<Integer> horizExpandDelay;
	private ConfigData<Integer> vertExpandDelay;
	private ConfigData<Integer> effectInterval;

	private boolean counterClockwise;

	private List<String> modifiersList;
	private List<String> locationModifiersList;

	private ModifierSet modifiers;
	private ModifierSet locationModifiers;

	public void loadFromString(String string) {
		MagicSpells.plugin.getLogger().warning("Warning: single line effects are being removed, usage encountered: " + string);
	}

	public final void loadFromConfiguration(ConfigurationSection config) {
		delay = ConfigDataUtil.getInteger(config, "delay", 0);
		chance = ConfigDataUtil.getDouble(config, "chance", -1);
		zOffset = ConfigDataUtil.getDouble(config, "z-offset", 0);
		heightOffset = ConfigDataUtil.getDouble(config, "height-offset", 0);
		forwardOffset = ConfigDataUtil.getDouble(config, "forward-offset", 0);

		String[] offsetStr = config.getString("offset", "0,0,0").split(",");
		String[] relativeStr = config.getString("relative-offset", "0,0,0").split(",");
		offset = new Vector(Double.parseDouble(offsetStr[0]), Double.parseDouble(offsetStr[1]), Double.parseDouble(offsetStr[2]));
		relativeOffset = new Vector(Double.parseDouble(relativeStr[0]), Double.parseDouble(relativeStr[1]), Double.parseDouble(relativeStr[2]));

		maxDistanceSquared = ConfigDataUtil.getDouble(config, "max-distance", 100);
		distanceBetween = ConfigDataUtil.getDouble(config, "distance-between", 1);

		String path = "orbit-";
		orbitXAxis = ConfigDataUtil.getFloat(config, path + "x-axis", 0F);
		orbitYAxis = ConfigDataUtil.getFloat(config, path + "y-axis", 0F);
		orbitZAxis = ConfigDataUtil.getFloat(config, path + "z-axis", 0F);
		orbitRadius = ConfigDataUtil.getFloat(config, path + "radius", 1F);
		orbitYOffset = ConfigDataUtil.getFloat(config, path + "y-offset", 0F);
		horizOffset = ConfigDataUtil.getFloat(config, path + "horiz-offset", 0F);
		horizExpandRadius = ConfigDataUtil.getFloat(config, path + "horiz-expand-radius", 0);
		vertExpandRadius = ConfigDataUtil.getFloat(config, path + "vert-expand-radius", 0);
		secondsPerRevolution = ConfigDataUtil.getFloat(config, path + "seconds-per-revolution", 3);

		horizExpandDelay = ConfigDataUtil.getInteger(config, path + "horiz-expand-delay", 0);
		vertExpandDelay = ConfigDataUtil.getInteger(config, path + "vert-expand-delay", 0);
		effectInterval = ConfigDataUtil.getInteger(config, "effect-interval", TimeUtil.TICKS_PER_SECOND);

		counterClockwise = config.getBoolean(path + "counter-clockwise", false);

		modifiersList = config.getStringList("modifiers");
		locationModifiersList = config.getStringList("location-modifiers");

		loadFromConfig(config);
	}

	public void initializeModifiers(Spell spell) {
		if (modifiersList != null) modifiers = new ModifierSet(modifiersList, spell);
		if (locationModifiersList != null) locationModifiers = new ModifierSet(locationModifiersList, spell);
	}

	protected abstract void loadFromConfig(ConfigurationSection config);

	public Location applyOffsets(Location loc) {
		return applyOffsets(loc, null);
	}

	public Location applyOffsets(Location loc, SpellData data) {
		return applyOffsets(loc, offset, relativeOffset, zOffset.get(data), heightOffset.get(data), forwardOffset.get(data));
	}

	public Location applyOffsets(Location loc, Vector offset, Vector relativeOffset, double zOffset, double heightOffset, double forwardOffset) {
		if (offset.getX() != 0 || offset.getY() != 0 || offset.getZ() != 0) loc.add(offset);
		if (relativeOffset.getX() != 0 || relativeOffset.getY() != 0 || relativeOffset.getZ() != 0)
			loc.add(VectorUtils.rotateVector(relativeOffset, loc));
		if (zOffset != 0) {
			Vector locDirection = loc.getDirection().normalize();
			Vector horizOffset = new Vector(-locDirection.getZ(), 0.0, locDirection.getX()).normalize();
			loc.add(horizOffset.multiply(zOffset));
		}
		if (heightOffset != 0) loc.setY(loc.getY() + heightOffset);
		if (forwardOffset != 0) loc.add(loc.getDirection().setY(0).normalize().multiply(forwardOffset));
		return loc;
	}

	/**
	 * Plays an effect on the specified entity.
	 *
	 * @param entity the entity to play the effect on
	 */
	@Deprecated
	public Runnable playEffect(final Entity entity) {
		return playEffect(entity, null);
	}

	/**
	 * Plays an effect on the specified entity.
	 *
	 * @param entity the entity to play the effect on
	 * @param data   the spell data of the casting spell
	 */
	public Runnable playEffect(final Entity entity, final SpellData data) {
		double chance = this.chance.get(data);
		if (chance > 0 && chance < 1 && random.nextDouble() > chance) return null;

		if (entity instanceof LivingEntity && modifiers != null && !modifiers.check((LivingEntity) entity)) return null;

		int delay = this.delay.get(data);
		if (delay <= 0) return playEffectEntity(entity, data);
		MagicSpells.scheduleDelayedTask(() -> playEffectEntity(entity, data), delay);

		return null;
	}

	@Deprecated
	protected Runnable playEffectEntity(Entity entity) {
		return playEffectLocationReal(entity == null ? null : entity.getLocation(), null);
	}

	protected Runnable playEffectEntity(Entity entity, SpellData data) {
		return playEffectLocationReal(entity == null ? null : entity.getLocation(), data);
	}

	/**
	 * Plays an effect at the specified location.
	 *
	 * @param location location to play the effect at
	 */
	@Deprecated
	public final Runnable playEffect(final Location location) {
		return playEffect(location, (SpellData) null);
	}

	/**
	 * Plays an effect at the specified location.
	 *
	 * @param location location to play the effect at
	 * @param data     the spell data of the casting spell
	 */
	public final Runnable playEffect(final Location location, final SpellData data) {
		double chance = this.chance.get(data);
		if (chance > 0 && chance < 1 && random.nextDouble() > chance) return null;

		if (locationModifiers != null && !locationModifiers.check(null, location)) return null;

		int delay = this.delay.get(data);
		if (delay <= 0) return playEffectLocationReal(location, data);
		MagicSpells.scheduleDelayedTask(() -> playEffectLocationReal(location, data), delay);

		return null;
	}

	@Deprecated
	public final Effect playEffectLib(final Location location) {
		return playEffectLib(location, null);
	}

	public final Effect playEffectLib(final Location location, final SpellData data) {
		double chance = this.chance.get(data);
		if (chance > 0 && chance < 1 && random.nextDouble() > chance) return null;

		if (locationModifiers != null && !locationModifiers.check(null, location)) return null;

		int delay = this.delay.get(data);
		if (delay <= 0) return playEffectLibLocationReal(location, data);
		MagicSpells.scheduleDelayedTask(() -> playEffectLibLocationReal(location, data), delay);

		return null;
	}

	@Deprecated
	public final Entity playEntityEffect(final Location location) {
		return playEntityEffect(location, null);
	}

	public final Entity playEntityEffect(final Location location, final SpellData data) {
		double chance = this.chance.get(data);
		if (chance > 0 && chance < 1 && random.nextDouble() > chance) return null;

		if (locationModifiers != null && !locationModifiers.check(null, location)) return null;

		int delay = this.delay.get(data);
		if (delay <= 0) return playEntityEffectLocationReal(location, data);
		MagicSpells.scheduleDelayedTask(() -> playEffectLibLocationReal(location, data), delay);

		return null;
	}

	@Deprecated
	public final ArmorStand playArmorStandEffect(final Location location) {
		return playArmorStandEffect(location, null);
	}

	public final ArmorStand playArmorStandEffect(final Location location, final SpellData data) {
		double chance = this.chance.get(data);
		if (chance > 0 && chance < 1 && random.nextDouble() > chance) return null;

		if (locationModifiers != null && !locationModifiers.check(null, location)) return null;

		int delay = this.delay.get(data);
		if (delay <= 0) return playArmorStandEffectLocationReal(location, data);
		MagicSpells.scheduleDelayedTask(() -> playEffectLibLocationReal(location, data), delay);

		return null;
	}

	private Runnable playEffectLocationReal(Location location, SpellData data) {
		if (location == null) return playEffectLocation(null, data);
		Location loc = location.clone();
		applyOffsets(loc, data);
		return playEffectLocation(loc, data);
	}

	private Effect playEffectLibLocationReal(Location location, SpellData data) {
		if (location == null) return playEffectLibLocation(null, data);
		Location loc = location.clone();
		applyOffsets(loc, data);
		return playEffectLibLocation(loc, data);
	}

	private Entity playEntityEffectLocationReal(Location location, SpellData data) {
		if (location == null) return playEntityEffectLocation(null, data);
		Location loc = location.clone();
		applyOffsets(loc, data);
		return playEntityEffectLocation(loc, data);
	}

	private ArmorStand playArmorStandEffectLocationReal(Location location, SpellData data) {
		if (location == null) return playArmorStandEffectLocation(null, data);
		Location loc = location.clone();
		applyOffsets(loc, data);
		return playArmorStandEffectLocation(loc, data);
	}

	@Deprecated
	protected Runnable playEffectLocation(Location location) {
		//expect to be overridden
		return null;
	}

	protected Runnable playEffectLocation(Location location, SpellData data) {
		//expect to be overridden
		return playEffectLocation(location);
	}

	@Deprecated
	protected Effect playEffectLibLocation(Location location) {
		//expect to be overridden
		return null;
	}

	protected Effect playEffectLibLocation(Location location, SpellData data) {
		//expect to be overridden
		return playEffectLibLocation(location);
	}

	@Deprecated
	protected Entity playEntityEffectLocation(Location location) {
		//expect to be overridden
		return null;
	}

	protected Entity playEntityEffectLocation(Location location, SpellData data) {
		//expect to be overridden
		return playEntityEffectLocation(location);
	}

	@Deprecated
	protected ArmorStand playArmorStandEffectLocation(Location location) {
		//expect to be overridden
		return null;
	}

	protected ArmorStand playArmorStandEffectLocation(Location location, SpellData data) {
		//expect to be overridden
		return playArmorStandEffectLocation(location);
	}

	@Deprecated
	public void playTrackingLinePatterns(Location origin, Location target, Entity originEntity, Entity targetEntity) {
		// no op, effects should override this with their own behavior
	}

	public void playTrackingLinePatterns(Location origin, Location target, Entity originEntity, Entity targetEntity, SpellData data) {
		// no op, effects should override this with their own behavior
		playTrackingLinePatterns(origin, target, originEntity, targetEntity);
	}

	/**
	 * Plays an effect between two locations (such as a smoke trail type effect).
	 *
	 * @param location1 the starting location
	 * @param location2 the ending location
	 */
	@Deprecated
	public Runnable playEffect(Location location1, Location location2) {
		return playEffect(location1, location2, null);
	}

	/**
	 * Plays an effect between two locations (such as a smoke trail type effect).
	 *
	 * @param location1 the starting location
	 * @param location2 the ending location
	 */
	public Runnable playEffect(Location location1, Location location2, SpellData data) {
		double maxDistance = this.maxDistanceSquared.get(data);
		if (location1.distanceSquared(location2) > maxDistance) return null;

		Location loc1 = location1.clone();
		Location loc2 = location2.clone();

		double distanceBetween = this.distanceBetween.get(data);
		int c = (int) Math.ceil(loc1.distance(loc2) / distanceBetween) - 1;
		if (c <= 0) return null;

		Vector v = loc2.toVector().subtract(loc1.toVector()).normalize().multiply(distanceBetween);
		Location l = loc1.clone();

		double heightOffset = this.heightOffset.get(data);
		if (heightOffset != 0) l.setY(l.getY() + heightOffset);

		for (int i = 0; i < c; i++) {
			l.add(v);
			playEffect(l, data);
		}

		return null;
	}

	@Deprecated
	public BuffEffectlibTracker playEffectlibEffectWhileActiveOnEntity(final Entity entity, final SpellEffectActiveChecker checker) {
		return new BuffEffectlibTracker(entity, checker, this, null);
	}

	public BuffEffectlibTracker playEffectlibEffectWhileActiveOnEntity(final Entity entity, final SpellEffectActiveChecker checker, SpellData data) {
		return new BuffEffectlibTracker(entity, checker, this, data);
	}

	@Deprecated
	public OrbitEffectlibTracker playEffectlibEffectWhileActiveOrbit(final Entity entity, final SpellEffectActiveChecker checker) {
		return new OrbitEffectlibTracker(entity, checker, this, null);
	}

	public OrbitEffectlibTracker playEffectlibEffectWhileActiveOrbit(final Entity entity, final SpellEffectActiveChecker checker, SpellData data) {
		return new OrbitEffectlibTracker(entity, checker, this, data);
	}

	@Deprecated
	public BuffTracker playEffectWhileActiveOnEntity(final Entity entity, final SpellEffectActiveChecker checker) {
		return new BuffTracker(entity, checker, this, null);
	}

	public BuffTracker playEffectWhileActiveOnEntity(final Entity entity, final SpellEffectActiveChecker checker, SpellData data) {
		return new BuffTracker(entity, checker, this, data);
	}

	@Deprecated
	public OrbitTracker playEffectWhileActiveOrbit(final Entity entity, final SpellEffectActiveChecker checker) {
		return new OrbitTracker(entity, checker, this, null);
	}

	public OrbitTracker playEffectWhileActiveOrbit(final Entity entity, final SpellEffectActiveChecker checker, final SpellData data) {
		return new OrbitTracker(entity, checker, this, data);
	}

	@FunctionalInterface
	public interface SpellEffectActiveChecker {
		boolean isActive(Entity entity);
	}

	public ConfigData<Integer> getDelay() {
		return delay;
	}

	public ConfigData<Double> getChance() {
		return chance;
	}

	public ConfigData<Double> getZOffset() {
		return zOffset;
	}

	public ConfigData<Double> getHeightOffset() {
		return heightOffset;
	}

	public ConfigData<Double> getForwardOffset() {
		return forwardOffset;
	}

	public Vector getOffset() {
		return offset;
	}

	public Vector getRelativeOffset() {
		return relativeOffset;
	}

	public ConfigData<Double> getMaxDistanceSquared() {
		return maxDistanceSquared;
	}

	public ConfigData<Double> getDistanceBetween() {
		return distanceBetween;
	}

	public ConfigData<Float> getOrbitXAxis() {
		return orbitXAxis;
	}

	public ConfigData<Float> getOrbitYAxis() {
		return orbitYAxis;
	}

	public ConfigData<Float> getOrbitZAxis() {
		return orbitZAxis;
	}

	public ConfigData<Float> getOrbitRadius() {
		return orbitRadius;
	}

	public ConfigData<Float> getOrbitYOffset() {
		return orbitYOffset;
	}

	public ConfigData<Float> getHorizOffset() {
		return horizOffset;
	}

	public ConfigData<Float> getHorizExpandRadius() {
		return horizExpandRadius;
	}

	public ConfigData<Float> getVertExpandRadius() {
		return vertExpandRadius;
	}

	public ConfigData<Float> getSecondsPerRevolution() {
		return secondsPerRevolution;
	}

	public ConfigData<Integer> getHorizExpandDelay() {
		return horizExpandDelay;
	}

	public ConfigData<Integer> getVertExpandDelay() {
		return vertExpandDelay;
	}

	public ConfigData<Integer> getEffectInterval() {
		return effectInterval;
	}

	public boolean isCounterClockwise() {
		return counterClockwise;
	}

	public ModifierSet getModifiers() {
		return modifiers;
	}

}
