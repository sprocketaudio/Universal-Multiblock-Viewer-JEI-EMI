# Universal Multiblock Viewer 0.5

Universal Multiblock Viewer 0.5 adds Creative-mode structure placement and in-game guide capture tools for players and modpack authors.

## Build Here

- Added the green **B** button to the lower-left of the 3D preview when playing in Creative mode.
- Preview the currently selected structure at the block you are looking at before placing it.
- The preview uses the selected variant, visible optional blocks, and chosen alternatives.
- Left click places the structure and right click cancels the preview.
- Mouse wheel rotates the preview. Ctrl + mouse wheel moves it up or down.
- Placement only replaces air. If any target block is occupied, the whole placement is cancelled safely.
- Added `/umv undo` to remove your most recent Build Here placement. Blocks changed after placement are preserved.
- Build Here does not consume items or activate machines, multiblocks, or rituals.

## In-Game Guide Capture

- Added OP-only `/umv` capture commands for pack authors using KubeJS.
- Mark two corners with `/umv corner1` and `/umv corner2`.
- Set the main lookup block with `/umv master U`, `/umv master R`, or `/umv master both`.
- Save a new definition with `/umv save namespace file_name`.
- New KubeJS namespace folders are created automatically when needed.
- UMV now explains when KubeJS is required instead of creating an unusable file.

## Reloading Guides

- Added `/umv reload` to refresh resources and update UMV guide pages in JEI without restarting Minecraft.
- EMI does not expose public runtime guide registration, so a restart is still required to add a new guide page in EMI.

## Compatibility

Build Here is Creative-only. In multiplayer, install Universal Multiblock Viewer on both the client and server for placement and undo to work.

**Minecraft 1.21.1 | NeoForge | JEI / EMI**
