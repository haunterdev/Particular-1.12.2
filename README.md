<div align="center">

  <h1>Particular (1.12.2 Forge)</h1>

  <p>Hand-crafted ambient particle effects for Minecraft 1.12.2.</p>

</div>

A Minecraft Forge 1.12.2 port of [Leclowndu93150's Particular](https://www.curseforge.com/minecraft/mc-mods/particular-reforged),
originally written for newer Minecraft versions. The look and behavior of the original effects are preserved,
rebuilt on the 1.12.2 Forge client rendering pipeline. The mod is purely client-side and cosmetic.

## Features

- **Water and lava splash crowns.** Entities falling into water or lava raise an animated splash crown sized to the entity, plus foam, rings, and droplets.
- **Cascading waterfalls.** Falling water and lava edges spawn cascade and spray particles.
- **Cave dust.** Faint drifting dust motes appear in dark caves below sea level, denser the deeper you go.
- **Fireflies.** Fireflies rise from grass, flowers, and tall plants at night within the right temperature range.
- **Water ripples.** Rain and dripping water leave expanding ripples on the surface.
- **Ender and chest bubbles.** Ambient bubble effects, including chest-open puffs.
- **Extra touches.** Emissive lava drips, cake-eating crumbs, and more.
- **In-game config.** Every effect can be toggled and tuned live under Mods, Particular, Config.

## Requirements

- Minecraft 1.12.2
- Minecraft Forge for 1.12.2 (developed against 14.23.5.2860)

No other dependencies. The mod is client-side only and will connect to any server, including vanilla.

## Installation

1. Install Minecraft Forge for 1.12.2.
2. Drop the Particular jar into your `mods` folder.
3. Launch the game. Effects are on by default and configurable under Mods, Particular, Config.

## Building

```
./gradlew build
```

The built jar lands in `build/libs/`.

## Credits

- Original Particular mod by **Leclowndu93150** and **Chai the goat**.
- 1.12.2 Forge port by **hunterhaunter**.

## License

Licensed under the **GNU Lesser General Public License v3.0**, the same license as the original mod.
See [LICENSE](LICENSE).
