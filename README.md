<div align="center">

  <h1>Particular</h1>

  <p>Hand-crafted ambient particle effects for Minecraft 1.12.2.</p>

</div>

A 1.12.2 port of [Leclowndu93150's Particular](https://www.curseforge.com/minecraft/mc-mods/particular-reforged),
originally written for newer Minecraft versions. The effects are rebuilt on the 1.12.2 client
rendering pipeline. The mod is purely client-side and cosmetic.

## Features

- **Water and lava splash crowns.** Entities falling into water or lava raise an animated splash crown sized to the entity, plus foam, rings, and droplets.
- **Cascading waterfalls.** Falling water and lava edges spawn cascade and spray particles.
- **Cave dust.** Faint drifting dust motes appear in dark caves below sea level, denser the deeper you go.
- **Falling leaves.** Leaf blocks drop leaves tinted by their texture and biome. They sway or spin as they fall, settle flat on the ground, and float with a ripple on water.
- **Fireflies.** Fireflies rise from grass, flowers, and tall plants at night within the right temperature range. Extra spawn blocks and a biome whitelist are set in the config.
- **Water ripples.** Rain and dripping water leave expanding ripples on the surface.
- **Coastal waves.** Foam crests roll across ocean and beach water toward the nearest shore, break on the sand and fade. They need a real stretch of coast, so single posts and tiny islets are skipped. Spawn rate, distance, size, the most on screen at once and the biome list are set in the config.
- **Ender and chest bubbles.** Ambient bubble effects, including chest-open puffs.
- **Extra touches.** Emissive lava drips, cake-eating crumbs, and more.
- **In-game config.** Every effect can be toggled and tuned live under Mods, Particular, Config.

## Requirements

- Minecraft 1.12.2
- Minecraft Forge for 1.12.2 (developed against 14.23.5.2860)

The mod is client-side only.

## Known issues

- With Better Weather installed, both mods draw rain ripples on water. Set `rainRipples` to false in Particular's config to keep only Better Weather's.
- Water ripples are very faint, and leaves that settle on water look like they sit under the surface. Minecraft 1.12.2 draws particles before water, so the water is drawn over them.

## License

Licensed under the **GNU Lesser General Public License v3.0**, the same license as the original mod.
See [LICENSE](LICENSE).

Modified by xy: ported to 1.12.2 (2026), coastal waves added (2026-10-26).

## Credits

- Coastal waves follow [Coastal Waves](https://modrinth.com/mod/coastal-waves) by Verph, and the
  wave textures are Coastal Waves' own, used under the BSD 2-Clause License
  (`src/main/resources/assets/particular/textures/particle/waves/LICENSE-coastal-waves.txt`).
- The wave logic is adapted from the Coastal Waves module of
  [An Extra Touch](https://github.com/AngelicIndustries/AnExtraTouch) by Angelic Industries
  (LGPL-3.0).
