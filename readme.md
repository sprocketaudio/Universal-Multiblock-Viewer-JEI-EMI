# Universal Multiblock Viewer (JEI/EMI)

**Interactive 3D building guides for Minecraft multiblocks, powered by JSON.**

Ever wanted to see how to build a multiblock, only to discover that the mod doesn't provide a visual guide?

Universal Multiblock Viewer lets players explore multiblock structures directly in JEI and EMI. Modpack creators can provide interactive construction guides for almost any mod using simple JSON definitions.

## ⚠️ Important: What This Mod Does

**Universal Multiblock Viewer is a visual guide, NOT a mod that adds functional multiblock machines.**

It displays structures from existing mods, helping players understand how to build them. It does not form machines, change recipes or interfere with the original mods' mechanics.

**JSON definitions are currently required.** The viewer does not automatically discover every multiblock in your modpack.

The Celestial Orrery shown in the screenshots and video is a fictional example structure created to demonstrate the viewer's features. It is not a functional machine added to Minecraft.

## Features

* **Interactive 3D models:** Rotate, pan and zoom to inspect structures from any angle.
* **Layer-by-layer viewing:** Examine individual layers or display the complete structure.
* **Block alternatives:** Select individual blocks to see every valid alternative for that position.
* **Materials list:** View required blocks and quantities, with clickable recipe lookups.
* **Multiple variants:** Explore different configurations, sizes and tiers of a multiblock.
* **Customisable viewer:** Switch between dark and light backgrounds, reset the camera and collapse the animated help panel.
* **JEI and EMI integration:** Find construction guides directly through your existing recipe viewer.

## Creating and Installing Multiblock Guides

Currently, multiblock guides must be supplied through JSON definitions created by users or modpack creators.

Definitions can be installed in two ways:

* **Resource packs:** Install or share JSON definitions as a standard Minecraft resource pack.
* **KubeJS:** Include definitions directly in your modpack's `kubejs/assets/` folder.

For example, using KubeJS:

`kubejs/assets/your_namespace/universal_multiblock_viewer/multiblocks/your_structure.json`

Or using a standard resource pack:

`assets/your_namespace/universal_multiblock_viewer/multiblocks/your_structure.json`

No custom Java code or animated tutorials are required.

See [JSON Authoring](JSON%20Authoring.md) for the complete definition format and lookup rules.

**Important:** Definitions are client-side resources. They must be installed on every client that needs to view them; they are not automatically synced from the server.

**Installing Universal Multiblock Viewer alone will not automatically add guides for every multiblock in your game.**

## Future Plans

This is just the beginning! Planned and potential additions include:

* **Bundled multiblock guides:** Include definitions for common multiblocks, particularly those belonging to mods that don't already provide visual construction guides.
* **Native mod integration:** Allow mod developers to bundle JSON definitions directly inside their own mod JARs, providing interactive construction guides without writing their own viewer.
* **Automatic detection:** Explore ways to read existing structure definitions directly from compatible mods, reducing the need for separate JSON files.
* **In-game structure capture:** Potentially allow players and pack creators to build a structure, select it in-game and automatically generate a viewer definition.
* **Additional creator tools:** More quality-of-life features to simplify creating, editing and maintaining multiblock guides.

These are future goals, not features of the current release. Automatic detection may not be possible for every mod.

***

**Minecraft 1.21.1 | NeoForge | JEI / EMI**
