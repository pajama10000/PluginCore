package xyz.fxcilities.core.particle

import org.bukkit.Location
import org.bukkit.World
import org.bukkit.plugin.Plugin
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.util.BoundingBox

class Visualizer(
    private val bb: BoundingBox,
    private val particleBuilder: ParticleBuilder,
    private val world: World
) : BukkitRunnable() {

    override fun run() {
        val coords = arrayOf(
            doubleArrayOf(bb.minX, bb.minY, bb.minZ) to doubleArrayOf(bb.maxX, bb.minY, bb.maxZ),
            doubleArrayOf(bb.maxX, bb.maxY, bb.maxZ) to doubleArrayOf(bb.minX, bb.maxY, bb.minZ),
            doubleArrayOf(bb.maxX, bb.maxY, bb.minZ) to doubleArrayOf(bb.minX, bb.minY, bb.minZ),
            doubleArrayOf(bb.minX, bb.maxY, bb.minZ) to doubleArrayOf(bb.minX, bb.minY, bb.maxZ),
            doubleArrayOf(bb.minX, bb.maxY, bb.maxZ) to doubleArrayOf(bb.maxX, bb.minY, bb.maxZ),
            doubleArrayOf(bb.maxX, bb.maxY, bb.minZ) to doubleArrayOf(bb.maxX, bb.minY, bb.maxZ)
        )

        for (coordsPair in coords) {
            val locOne = coordsPair.first
            val locTwo = coordsPair.second
            val box = BoundingBox(
                locOne[0], locOne[1], locOne[2],
                locTwo[0], locTwo[1], locTwo[2]
            )
            wireframe(box)
        }
    }

    private fun wireframe(box: BoundingBox) {
        for (x in box.minX..box.maxX)
            for (y in box.minY..box.maxY)
                for (z in box.minZ..box.maxZ) {
                    val loc = Location(world, x, y, z)
                    if (particleBuilder.shouldUseData())
                        world.spawnParticle(particleBuilder.particle, loc, 0, particleBuilder.data)
                    else
                        world.spawnParticle(particleBuilder.particle, loc, 0)
                }
    }
}
