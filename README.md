# SusEventer

A spigot plugin for conditionally cancelling various cancellable Bukkit/Spigot events.

### Commands

#### Plot Config

Commands for configuring PlotSquared per-plot bypass conditions.

`/sus plot info` — Get any custom (non-default) configuration.

`/sus plot allow <Cancellable Event> <Bypass Condition>` — Set the custom bypass condition for a given event.

`/sus plot reset <Cancellable Event | ALL>` — Reset the custom bypass for the given event, or all events.

### Config File

The config file lets you enable or disable handlers for different events, and which bypass conditions may be set with
the per-plot config commands.

For format, see the default `config.yml`.

### Vocabulary
| Term   | Meaning                                                                                |
|--------|----------------------------------------------------------------------------------------|
| Actor  | The source of an event, be it a player, a block, an entity- whatever caused the event. |
| Target | The target of an event, the thing which is being acted upon by the event.              |
|        |                                                                                        |

### Cancellable Events
| Descriptive Name | Bukkit/Spigot Event           | Additional                                                    | Default     | Default Bypass | Notes                                     | Implemented? |
|------------------|-------------------------------|---------------------------------------------------------------|-------------|----------------|-------------------------------------------|--------------|
| `TargetBlockHit` | `ProjectileHitEvent`          | When the target is a `minecraft:target` block.                | `Cancelled` | `plot-trust`   | Stops target shooting shenanigans.        | FULLY        |
| `CartPush`       | `VehicleEntityCollisionEvent` | When the target is a `minecart` variant and the actor is not. | `Cancelled` | `plot-trust`   | Keeps minecart systems safe from players. | FULLY        |
| `TPToggleBypass` | `PlayerTeleportEvent`         | When the target has `tptoggle` set to disallow teleportation. | `Cancelled` | `never`        | Stops `tptoggle` bypasses.                | FULLY        |
| `BoatCollide`    | `VehicleEntityCollisionEvent` | When the target is a `boat`                                   | `Ignored`   | `never`        | Stops "ghost collisions" in boat races.   | FULLY        |
|                  |                               |                                                               |             |                |                                           |              |

### Bypass Conditions
| Name                   | Actor: Player                                    | Actor: Block                           | Actor: Entity | Implemented? |
|------------------------|--------------------------------------------------|----------------------------------------|---------------|--------------|
| `always`               | Always                                           | Always                                 | Always        | FULLY        |
| `never`                | Never                                            | Never                                  | Never         | FULLY        |
| `plot-trust-inclusive` | Is trusted to the plot where the event occurred. | Always                                 | Always        | FULLY        |
| `plot-trust`           | Is trusted to the plot where the event occurred. | Is within the same plot as the target. | Is Minecart   | FULLY        |
| `is-player`            | Always                                           | Never                                  | Never         | FULLY        |
| `is-block`             | Never                                            | Always                                 | Never         | FULLY        |
| `is-entity`            | Never                                            | Never                                  | Always        | FULLY        |
|                        |                                                  |                                        |               |              |

## Development
### Note about the `runServer` Gradle task:
You'll need to do a couple of things before it works quite right.
1. Run once and then accept the EULA in `./run/EULA.txt`.
2. Run again then add working versions of `EssentialsX`, `WorldEdit`, and `PlotSquared` to `./run/plugins/`.

That should be all it takes, and after that it should work correctly.