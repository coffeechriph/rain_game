package roguelike.Entity

import org.joml.Vector3f
import rain.api.entity.Entity
import rain.api.entity.ParticleEmitterEntity

class LightSource(val cellX: Int, val cellY: Int, val color: Vector3f, val emitter: ParticleEmitterEntity): Entity() {
}