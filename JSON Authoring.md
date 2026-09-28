# JSON Authoring

Universal Multiblock Viewer displays multiblock building guides from JSON files. A definition is client-side documentation only: it never forms a machine, changes blocks, or changes how another mod works.

Install the resource-containing mod or resource pack on every client that should view a guide. Definitions are not synced from a server.

## File location

Place each definition at:

`assets/<your namespace>/universal_multiblock_viewer/multiblocks/<name>.json`

The document `id` is the guide's stable identity. It should match the namespace of the resource pack or mod that supplies the file.

## Example

```json
{
  "format": 1,
  "id": "example:small_forge",
  "title": "Small Forge",
  "description": "A documentation-only example.",
  "lookups": {
    "U": ["example:forge_core"],
    "R": ["example:forge_core"]
  },
  "default_variant": "basic",
  "variants": [
    {
      "id": "basic",
      "title": "Basic",
      "palette": {
        "C": { "block": "example:forge_core", "label": "Core" },
        "B": { "block": "minecraft:polished_blackstone_bricks" },
        "I": {
          "label": "Input",
          "any_of": [
            { "block": "example:item_input" },
            { "tag": "c:chests" }
          ],
          "default": "example:item_input"
        }
      },
      "layers": [
        ["BBB", "BCB", "BIB"],
        ["BBB", "B B", "BBB"]
      ]
    }
  ]
}
```

## Structure layout

`layers` are written bottom-to-top. Each layer is an ordered list of front-to-back rows, and each row is left-to-right. A space is air. Every layer must have the same number of rows and every row must have the same width.

## Palette entries and alternatives

Each palette entry must have exactly one of `block`, `tag`, or `any_of`.

- `block` names one exact block.
- `tag` accepts any block in that block tag.
- `any_of` lists the valid choices for that position. Each choice contains one `block` or `tag`; `default` must name one of those choice IDs.

The materials list counts one default choice per position, never every alternative. Definitions are limited to 64 blocks in each direction and 4,096 non-air positions per variant.

## JEI and EMI lookups

`lookups` controls how players find a guide in JEI and EMI.

- `U` lists item IDs whose Uses view shows the guide. Normally this is the multiblock controller or core block.
- `R` lists item IDs whose Recipes view shows the same guide.

Both arrays are optional, but at least one must contain an item. Use registered item IDs, not unplaceable block IDs. The same item may appear in both lists. Materials and alternatives do not automatically become lookup triggers.

## Testing and errors

Universal Multiblock Viewer ships without definitions. Add your JSON through a resource pack or datapack before testing. Test each host with U and R separately, and confirm existing normal crafting and Uses entries remain available.

Invalid files are skipped without crashing the client. The log identifies the resource and the failing variant, layer, row, or cell path. The current JSON format is `"format": 1`.
