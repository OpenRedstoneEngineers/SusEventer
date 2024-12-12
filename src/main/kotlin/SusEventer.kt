import co.aikar.commands.BaseCommand
import co.aikar.commands.ConditionFailedException
import com.plotsquared.core.configuration.caption.StaticCaption
import com.plotsquared.core.plot.Plot
import com.plotsquared.core.plot.flag.GlobalFlagContainer
import com.plotsquared.core.plot.flag.InternalFlag
import com.plotsquared.core.plot.flag.PlotFlag
import net.md_5.bungee.api.ChatMessageType
import net.md_5.bungee.api.chat.TextComponent
import org.bukkit.*
import org.bukkit.Particle.DustTransition
import org.bukkit.block.Block
import org.bukkit.entity.Boat
import org.bukkit.entity.Entity
import org.bukkit.entity.Minecart
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.ProjectileHitEvent
import org.bukkit.event.vehicle.VehicleEntityCollisionEvent
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.projectiles.BlockProjectileSource
import java.util.*
import kotlin.collections.HashMap
import co.aikar.commands.PaperCommandManager
import co.aikar.commands.annotation.CommandAlias
import co.aikar.commands.annotation.CommandCompletion
import co.aikar.commands.annotation.Conditions
import co.aikar.commands.annotation.Description
import co.aikar.commands.annotation.Subcommand
import com.earth2me.essentials.Essentials
import com.earth2me.essentials.User
import com.plotsquared.core.player.PlotPlayer
import org.bukkit.event.player.PlayerTeleportEvent


class SusEventer : JavaPlugin(), Listener {

    private enum class EventType {
        TargetBlockHit,
        CartPush,
        BoatCollide,
        TPToggleBypass,
    }

    private val eventTypeStringToEnumMap = EventType.values().associateBy { it.name }

    private val handlerConfig        = config.getConfigurationSection("handlers")!!
    private val defaultHandlerConfig = handlerConfig.defaultSection!!

    private val enabledHandlers      = defaultHandlerConfig.getKeys(false).filter { handlerConfig.getBoolean("$it.enabled") }.map { eventTypeStringToEnumMap[it]!! }.toSet()
    private val defaultHandlers      = defaultHandlerConfig.getKeys(false).associate { eventTypeStringToEnumMap[it]!! to (handlerConfig.getString("$it.default") ?: "never") }
    private val configurableHandlers = defaultHandlerConfig.getKeys(false).map { eventTypeStringToEnumMap[it]!! to handlerConfig.getStringList("$it.configurable.options") }.filter { it.second.size > 0 }.toMap()

    private val essentials = getPlugin(Essentials::class.java)

    @Suppress("Unused")
    @CommandAlias("suseventer|sus")
    @Description("SusEventer plot configuration")
    private inner class SusEventerCommand : BaseCommand() {

        @Subcommand("plot")
        @Conditions("plot-configure")
        @Description("Manage SusEventer conditions for your plot(s)")
        inner class PlotCommand : BaseCommand() {
            @Subcommand("info")
            @Description("Get the configuration for the current plot")
            fun info (player: Player) {
                val plot = getPlot(player) ?: return

                val currentFlag = plot.getFlag(SusEventerFlag::class.java)

                if (currentFlag.isEmpty()) {
                    player.sendMessage("Plot $plot has no custom configuration.")
                } else {
                    player.sendMessage("Current configuration for plot $plot:\n${currentFlag.entries.joinToString("\n") { "- ${it.key} = ${it.value}" }}")
                }
            }

            @Subcommand("allow")
            @Description("Adjust when a given event cancellation will be bypassed")
            @CommandCompletion("@configurable-handler @bypass-condition")
            fun allow (player: Player, eventType: EventType, condition: String) {
                val plot = getPlot(player) ?: return

                if (!configurableHandlers.contains(eventType)) {
                    player.sendMessage("Invalid event type.")
                    return
                }

                if (configurableHandlers[eventType]?.contains(condition) != true) {
                    player.sendMessage("Invalid condition.")
                    return
                }

                val currentFlag = plot.getFlag(SusEventerFlag::class.java)
                val newFlag =
                    if (condition == defaultHandlers[eventType])
                        currentFlag - eventType.name
                    else
                        currentFlag + (eventType.name to condition)

                plot.setFlag(SusEventerFlag(newFlag))

                player.sendMessage("Updated configuration for plot $plot with $eventType = $condition.")
            }

            @Subcommand("reset")
            @Description("Reset a bypass condition configuration")
            @CommandCompletion("ALL|@configurable-handler")
            fun reset (player: Player, eventTypeString: String) {
                val plot = getPlot(player) ?: return

                if (eventTypeString == "ALL") {
                    plot.removeFlag(SusEventerFlag::class.java)

                    player.sendMessage("Reset all custom configuration for plot $plot.")

                    return
                }

                val currentFlag = plot.getFlag(SusEventerFlag::class.java)
                plot.setFlag(SusEventerFlag(currentFlag - eventTypeString))

                player.sendMessage("Removed $eventTypeString from custom configuration for plot $plot.")
            }
        }
    }

    private fun registerCommands () {
        val commandManager = PaperCommandManager(this)

        commandManager.commandCompletions.registerStaticCompletion("configurable-handler", configurableHandlers.keys.map { it.toString() })

        commandManager.commandCompletions.registerAsyncCompletion("bypass-condition") {
            context -> configurableHandlers[context.getContextValue(EventType::class.java)]
        }

        commandManager.commandConditions.addCondition("plot-configure") {
            context ->
                val player = context.issuer.player
                val plot = getPlot(player) ?: throw ConditionFailedException("You're not on a plot!")

                if (!plot.isOwner(player.uniqueId)
                    && !player.hasPermission("plots.admin.command.add")
                // ^ IDK a better way to decide if someone has "moderator" permissions or above lol
                ) {
                    throw ConditionFailedException("Sorry, you don't have permission to do that on this plot!")
                }
        }

        commandManager.registerCommand(SusEventerCommand())
    }

    override fun onEnable() {
        super.onEnable()

        logger.info("Enabled handlers: ${enabledHandlers.joinToString(" ")}")

        registerCommands()

        GlobalFlagContainer.getInstance().addFlag(SusEventerFlag(HashMap()))
        server.pluginManager.registerEvents(this, this)
    }



    @EventHandler
    fun onProjectileHit (event: ProjectileHitEvent) {
        // TargetBlockHit
        if (enabledHandlers.contains(EventType.TargetBlockHit)
            && event.hitBlock?.type?.equals(Material.TARGET) == true
        ) {
            val shooter    = event.entity.shooter!!
            val actor: Any = if (shooter is BlockProjectileSource) shooter.block else shooter
            val target     = event.hitBlock!!

            if (shouldBypass(EventType.TargetBlockHit, actor, target, event)) return

            event.isCancelled = true
            if (shooter is Player) {
                shooter.spigot().sendMessage(
                    ChatMessageType.ACTION_BAR,
                    TextComponent.fromLegacyText(
                        FunnyMessages.getMessage(event.entity.type.name.lowercase().replace('_', ' '))
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

    @EventHandler
    fun onVehicleCollision (event: VehicleEntityCollisionEvent) {
        // CartPush
        if (enabledHandlers.contains(EventType.CartPush)
            && event.vehicle is Minecart
            && event.entity !is Minecart
            && !shouldBypass(EventType.CartPush, event.entity, event.vehicle, event)
        ) {
            event.isCancelled = true
            event.isCollisionCancelled = true
            return
        }

        // BoatCollide
        if (enabledHandlers.contains(EventType.BoatCollide)
            && event.vehicle is Boat
            && !shouldBypass(EventType.BoatCollide, event.entity, event.vehicle, event)
        ) {
            event.isCancelled = true
            event.isCollisionCancelled = true
            return
        }
    }

    @EventHandler
    fun onPlayerTeleport (event: PlayerTeleportEvent) {
        // TPToggleBypass
        if (enabledHandlers.contains(EventType.TPToggleBypass)
            && event.cause == PlayerTeleportEvent.TeleportCause.SPECTATE
            && event.to!!.world!!.getNearbyEntities(event.to!!, 1.0, 1.0, 1.0) { it is Player }.any { !User(it as Player, essentials).isTeleportEnabled }
        ) {
            event.isCancelled = true
            return
        }
    }

    private fun shouldBypass (eventType: EventType, actor: Any, target: Any, event: Event): Boolean {
        var bypass: String = defaultHandlers[eventType]!!
        if (configurableHandlers.contains(eventType)) {
            bypass = getPlot(target)?.getFlag(SusEventerFlag::class.java)?.get(eventType.name) ?: bypass
        }
        return getBypassCondition(bypass)(actor, target, event)
    }

    private fun getBypassCondition (name: String): (
        (actor: Any, target: Any, event: Event) -> Boolean
    ) = when(name) {
        "plot-trust" -> fun (actor, target, _) = when (actor) {
            is Player -> getPlot(target)?.isTrusted(actor.uniqueId) ?: false
            is Block  -> getPlot(target) == plotAt(actor.location, actor.world)
            else -> false
        }
        "plot-trust-inclusive" -> fun (actor, target, _) = when (actor) {
            is Player -> getPlot(target)?.isTrusted(actor.uniqueId) ?: false
            else -> true
        }
        "always" -> fun (_,_,_) = true
        "never"  -> fun (_,_,_) = false
        "is-player" -> fun (actor,_,_) = actor is Player
        "is-block"  -> fun (actor,_,_) = actor is Block
        "is-entity" -> fun (actor,_,_) = actor is Entity
        else -> fun (_,_,_) = false.also { logger.warning("Unknown bypass condition \"$name\"!") }
    }

    private fun getPlot (subject: Any) = when(subject) {
        is Player -> PlotPlayer.from(subject).currentPlot
        is Block  -> plotAt(subject.location, subject.world)
        is Entity -> plotAt(subject.location, subject.world)
        else -> null
    }

    private fun plotAt (location: Location, world: World) =
        com.plotsquared.core.location.Location.at(
            world.name,
            location.blockX,
            location.blockY,
            location.blockZ,
            0.0f, 0.0f
        ).plot

    private fun Plot.isTrusted(uuid: UUID): Boolean = this.isOwner(uuid) || this.trusted.contains(uuid)


    // Flag implementation for PlotSquared
    private class SusEventerFlag (value: Map<String, String>) :
        PlotFlag<Map<String, String>, SusEventerFlag>(
            value,
            StaticCaption.of("SusEventer"),
            StaticCaption.of("Stores preference data for SusEventer")
        ),
        InternalFlag
    {
        override fun toString(): String = value.map { "${it.key}=${it.value}" }.joinToString(",")
        override fun parse (input: String): SusEventerFlag = flagOf(input.split(',', '=').zipWithNext().toMap())
        override fun merge(newValue: Map<String, String>): SusEventerFlag = flagOf(HashMap())
        override fun getExample(): String = ""
        override fun flagOf(value: Map<String, String>): SusEventerFlag = SusEventerFlag(value)
    }
}