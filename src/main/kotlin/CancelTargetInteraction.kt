import net.md_5.bungee.api.ChatMessageType
import net.md_5.bungee.api.chat.TextComponent
import org.bukkit.Color
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.Particle.DustTransition
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.ProjectileHitEvent
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.projectiles.BlockProjectileSource
import org.bukkit.projectiles.ProjectileSource

class ProjectileHitListener : Listener {
    @EventHandler
    fun onProjectileHit (event: ProjectileHitEvent) {
        if (event.hitBlock?.type?.equals(Material.TARGET) == true) {

            // If the shooter
            if (targetHitAllowed(event.hitBlock!!.location, event.entity.shooter!!)) return

            event.isCancelled = true
            val shooter = event.entity.shooter
            if (shooter is Player) {
                shooter.spigot().sendMessage(
                    ChatMessageType.ACTION_BAR,
                    TextComponent.fromLegacyText(
                        FunnyMessages.getMessage(event.entity.type.toString().lowercase())
                    )[0]
                )
                shooter.world.spawnParticle(
                    Particle.DUST_COLOR_TRANSITION,
                    event.entity.location,
                    25,
                    0.5, 0.5, 0.5,
                    DustTransition(
                        Color.BLACK,
                        Color.WHITE,
                        1.0f
                    )
                )
            }
            event.entity.remove()
        }
    }

    private fun targetHitAllowed (where: Location, who: ProjectileSource): Boolean {
        /**
         * 1. Check what plot `event.hitBlock` is on.
         * 2. Check if `event.entity.shooter` is a player who is trusted on that plot.
         * 2.a. Or a dispenser on the same plot (this should be possible).
         * 3. Then, and only then, return true here instead of false.
         */

        if (who is Player) {
            // TODO `return plotOf(where).isPlayerTrusted(who)`
            // (Pseudocode)
        } else if (who is BlockProjectileSource) {
            val sourceLocation = who.block.location
            // TODO: `return plotOf(where) == plotOf(sourceLocation)`
            // (Pseudocode)
        }

        return false
    }
}

@Suppress("unused")
class CancelTargetInteraction : JavaPlugin() {
    override fun onEnable() {
        super.onEnable()
        server.pluginManager.registerEvents(ProjectileHitListener(), this)
    }
}
