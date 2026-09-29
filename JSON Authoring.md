# JSON Authoring Guide

This guide explains how to create a multiblock building guide for **Universal Multiblock Viewer**. You do not need to write Java or make a custom screen. You write one JSON file that describes the blocks and where they go.

Universal Multiblock Viewer is documentation only. Your JSON file does **not** create a working machine, validate a structure, change recipes, or change another mod's behaviour.

## Before you begin

You need:

- Minecraft 1.21.1 with Universal Multiblock Viewer installed.
- JEI, EMI, or both installed.
- A text editor. VS Code, Notepad++, or IntelliJ are good choices. Do not use a word processor such as Microsoft Word.
- The block and item IDs for the blocks you want to document.

To find an ID in-game, hover an item in JEI or EMI while holding `F3` + `H` to enable advanced tooltips. Mod documentation and `/kubejs hand` may also help, depending on your pack.

An ID normally looks like `minecraft:stone_bricks` or `examplemod:machine_core`:

- The part before `:` is the namespace or mod ID.
- The part after `:` is the block or item name.

Block IDs are used in the structure palette. Item IDs are used for JEI/EMI lookups. For ordinary placeable blocks they are usually the same, but they are not interchangeable in every mod.

## Quick start: make your first guide

The easiest way to add guides to a modpack is through KubeJS.

1. Open your instance's `kubejs` folder.
2. Create these folders if they do not already exist:

   ```text
   kubejs/
     assets/
       my_guides/
         universal_multiblock_viewer/
           multiblocks/
   ```

3. Create a file named `stone_platform.json` inside `multiblocks`.
4. Paste this complete example into it:

   ```json
   {
     "format": 1,
     "id": "my_guides:stone_platform",
     "title": "Stone Platform",
     "description": "A simple example guide.",
     "lookups": {
       "U": ["minecraft:crafting_table"]
     },
     "default_variant": "basic",
     "variants": [
       {
         "id": "basic",
         "title": "Basic",
         "palette": {
           "C": { "block": "minecraft:crafting_table", "label": "Controller" },
           "S": { "block": "minecraft:stone_bricks" }
         },
         "layers": [
           [
             "SSS",
             "SCS",
             "SSS"
           ]
         ]
       }
     ]
   }
   ```

5. Restart Minecraft. This is the most reliable way to make JEI/EMI rebuild their guide lists after adding or changing a definition.
6. In-game, hover a Crafting Table and press `U`. The **Stone Platform** guide should appear in Universal Multiblock Viewer's category.

`my_guides` is your namespace. Choose one short, lowercase name for your pack or guide collection, then use the same name in both the folder path and the JSON `id`.

## Using a resource pack instead of KubeJS

You can also place guides in a normal client resource pack or inside a mod's JAR. The JSON path is always:

```text
assets/<namespace>/universal_multiblock_viewer/multiblocks/<file name>.json
```

For example:

```text
assets/my_guides/universal_multiblock_viewer/multiblocks/stone_platform.json
```

Enable the resource pack in Minecraft's Resource Packs screen, then restart the game. Every player who should see a guide needs the resource pack or mod containing it on their own client. Definitions are not sent by a server.

## JSON basics

JSON is strict:

- Put text in double quotes: `"title": "My Machine"`.
- Put a comma after an entry when another entry follows it.
- Do **not** put a comma after the final entry in an object or list.
- JSON does not support comments such as `// like this`.
- Keep spaces inside structure rows exactly as written: a space means air.

If a guide does not load, check `logs/latest.log`. Universal Multiblock Viewer reports the file and the part of the JSON that failed.

## The top of a definition

Every guide needs these fields:

| Field | What it does |
| --- | --- |
| `format` | Always use `1`. |
| `id` | A unique ID for this guide, such as `my_guides:stone_platform`. |
| `title` | The player-facing guide name. |
| `lookups` | Controls which items can open the guide in JEI/EMI. |
| `variants` | One or more versions of the structure. |

`description` is optional. `default_variant` is optional; if you leave it out, the first variant is used.

## Make the guide appear in JEI and EMI

Use the `lookups` object to choose how players find the guide:

```json
"lookups": {
  "U": ["examplemod:machine_core"],
  "R": ["examplemod:machine_core"]
}
```

- `U` means the guide appears when the player presses `U` (**Uses**) on one of these items.
- `R` means the guide appears when the player presses `R` (**Recipes**) on one of these items.
- You may use `U`, `R`, or both, but at least one list must contain an item.
- You may list several items. This is useful when several controller tiers open the same guide.
- Only explicitly listed items open the guide. Materials used in the structure do not automatically become lookup items.

Use item IDs here, not tags. Keep normal JEI/EMI recipes and uses intact: the viewer guide appears alongside them.

## Variants

A variant is a complete version of the same structure. You can use variants for tiers, sizes, upgrade stages, or alternate layouts.

```json
"variants": [
  {
    "id": "tier_1",
    "title": "Tier 1",
    "palette": { },
    "layers": [ ]
  },
  {
    "id": "tier_2",
    "title": "Tier 2",
    "palette": { },
    "layers": [ ]
  }
]
```

Each variant needs:

- `id`: a unique internal name, such as `tier_1`. Use lowercase letters, numbers, underscores, or hyphens.
- `title`: the name players see in the viewer. If omitted, the viewer shows the `id`.
- `palette`: the symbol-to-block list.
- `layers`: the actual structure shape.

Each variant is written in full. A later tier does not automatically inherit blocks from an earlier tier.

## The palette

The palette gives each block requirement a one-character symbol. You use the symbol in the structure rows later.

```json
"palette": {
  "C": { "block": "examplemod:machine_core", "label": "Controller" },
  "B": { "block": "minecraft:stone_bricks" }
}
```

Rules:

- Every palette key is exactly one character.
- A space cannot be a palette key because spaces always mean air.
- Each palette entry uses exactly one of `block`, `tag`, or `any_of`.
- `label` is optional. It is useful for explaining a role such as `Controller` or `Input hatch`.

### Exact block

Use `block` when one specific block is required:

```json
"C": {
  "block": "examplemod:machine_core",
  "label": "Controller"
}
```

### A specific block state

Use `state` when the block must use particular block-state properties. State values are written as strings.

```json
"L": {
  "block": "minecraft:oak_log",
  "state": {
    "axis": "y"
  }
}
```

Only exact `block` entries can use `state`. The property name and value must exist on that block.

### Block tag

Use `tag` when any block in a Minecraft block tag is valid:

```json
"F": {
  "tag": "minecraft:base_stone_overworld",
  "label": "Foundation"
}
```

Write the tag ID without `#`. For example, use `"c:chests"`, not `"#c:chests"`.

The viewer expands tags into the individual installed blocks when the guide loads. This includes modded tags. If a tag cannot be found, the log contains a warning so you can correct the tag ID or make sure the supplying mod is installed.

### Several valid choices: `any_of`

Use `any_of` when a position accepts a mixture of exact blocks and/or tags:

```json
"I": {
  "label": "Input",
  "any_of": [
    { "block": "examplemod:item_input" },
    { "tag": "c:chests" }
  ],
  "default": "examplemod:item_input"
}
```

The `default` is the block shown first and used for the materials list. It must name one of the entries directly inside `any_of`.

Tags in `any_of` are expanded into their individual blocks in the selected-block inspector. If an explicit block is also present through a tag, it appears only once. Different block states remain separate choices when you explicitly author them.

If you omit `default`, the first `any_of` entry is the default. Put an exact block first when you want a predictable default material.

## Optional blocks

Add `"optional": true` to a palette entry when the blocks are not required for the basic structure:

```json
"P": {
  "block": "minecraft:polished_blackstone_bricks",
  "label": "Optional support pillar",
  "optional": true
}
```

Every `P` in the structure becomes optional.

- Optional blocks are hidden by default in the viewer.
- Players use the `O` button to show or hide them.
- Optional blocks do not add to the required material totals.
- When shown, optional materials appear after required materials in the existing material strip.
- Optional entries can use `block`, `tag`, or `any_of` just like required entries.

Use a separate palette symbol if the same block is required in one place and optional in another.

## Writing the structure with layers

`layers` is a list of horizontal layers, written from **bottom to top**.

Each layer is a list of text rows. Read each row from left to right. The first row is one side of the structure and the final row is the opposite side. For a viewer guide, the exact world direction does not matter as long as you use one consistent orientation.

This is a 3 x 3 base with a controller in the centre:

```json
"layers": [
  [
    "BBB",
    "BCB",
    "BBB"
  ]
]
```

This two-layer structure has air above the centre block:

```json
"layers": [
  [
    "BBB",
    "BCB",
    "BBB"
  ],
  [
    "BBB",
    "B B",
    "BBB"
  ]
]
```

Important rules:

- All layers must have the same number of rows.
- Every row in every layer must have the same number of characters.
- Every non-space character must exist in the palette.
- A literal space is an empty position (air).
- A structure may be at most 64 blocks wide, 64 blocks deep, and 64 blocks high, with at most 4,096 non-air positions per variant.

## Full example with alternatives and optional blocks

This example demonstrates the complete current format using only vanilla blocks. It is a guide example, not a functional Minecraft machine.

```json
{
  "format": 1,
  "id": "my_guides:example_forge",
  "title": "Example Forge",
  "description": "A demonstration of a JSON multiblock guide.",
  "lookups": {
    "U": ["minecraft:smithing_table"],
    "R": ["minecraft:smithing_table"]
  },
  "default_variant": "basic",
  "variants": [
    {
      "id": "basic",
      "title": "Basic",
      "palette": {
        "C": {
          "block": "minecraft:smithing_table",
          "label": "Controller"
        },
        "B": {
          "block": "minecraft:polished_blackstone_bricks"
        },
        "S": {
          "any_of": [
            { "block": "minecraft:gold_block" },
            { "block": "minecraft:copper_block" },
            { "block": "minecraft:iron_block" }
          ],
          "default": "minecraft:gold_block",
          "label": "Decorative corner"
        },
        "P": {
          "block": "minecraft:polished_blackstone_bricks",
          "label": "Optional support pillar",
          "optional": true
        }
      },
      "layers": [
        [
          "SBS",
          "BCB",
          "SBS"
        ],
        [
          "P P",
          "   ",
          "P P"
        ]
      ]
    }
  ]
}
```

## Optional documentation fields

These fields do not change the structure. They record useful information for pack creators and future maintenance:

```json
"provenance": {
  "source_mod": "examplemod",
  "tested_version": "1.2.3",
  "notes": "Checked against the in-game guide on 2026-09-29."
}
```

- `source_mod` must be a lowercase mod ID.
- `tested_version` records the mod version you checked.
- `notes` is free-form author documentation.

If the named source mod is installed but its version differs from `tested_version`, Universal Multiblock Viewer writes a warning to the log. The guide still loads; this is only a reminder that third-party structures can change between versions.

You may also use translation keys instead of, or alongside, plain text:

```json
"title_key": "guide.my_guides.example_forge.title",
"description_key": "guide.my_guides.example_forge.description"
```

Keep a plain `title` as a readable fallback whenever possible.

## Testing checklist

Before sharing a guide:

1. Restart the client with the guide installed.
2. Check `logs/latest.log` for `Universal Multiblock Viewer` errors or warnings.
3. Press `U` on every item in `lookups.U`.
4. Press `R` on every item in `lookups.R`.
5. Check every variant and every layer in the viewer.
6. Turn on optional blocks and confirm their materials are separate from required materials.
7. Click positions with alternatives and confirm every valid option appears.
8. Check the original mod's own documentation or test structure. Universal Multiblock Viewer displays your description; it does not verify that another mod accepts the build.

## Common problems

| Problem | What to check |
| --- | --- |
| The guide does not appear | Check the file path, namespace, JSON commas, and restart Minecraft. |
| `U` or `R` does not find the guide | Check that the item ID is correct and is listed in the matching lookup array. |
| A block is missing or shows a barrier | Check the block ID, make sure the mod is installed, and check the log. |
| A tag shows no alternatives | Use a block tag ID without `#`; make sure the tag exists in the installed pack. |
| The shape is wrong | Check that layers are bottom-to-top, rows all have identical widths, and spaces are intentional air. |
| Materials are wrong | Check the palette symbol used at each position and the chosen `default` for each `any_of` entry. |
| JSON fails to load | Read the Universal Multiblock Viewer error in `logs/latest.log`; it names the field, variant, layer, row, or cell that needs fixing. |

## Current format limits

- `"format": 1` is the current and only supported format.
- A variant supports up to 64 layers, 64 rows per layer, and 64 characters per row.
- A variant supports up to 4,096 non-air block positions.
- An `any_of` entry supports up to 64 directly authored choices.
- `associated_items` is obsolete. Use `lookups.U` and `lookups.R` instead.
