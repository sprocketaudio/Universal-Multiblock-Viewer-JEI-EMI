# Universal Multiblock Viewer 0.2 authoring — U/R lookups

Definitions are client resources. Install the resource-containing mod or resource pack on every client that should view them; they are documentation data and are not synced from a server in 0.2.

Place each JSON file at:

`assets/<your namespace>/universal_multiblock_viewer/multiblocks/<name>.json`

The document `id` is the stable identity used by both recipe viewers. `lookups` replaces the retired `associated_items` field: uppercase `U` lists item IDs whose Uses view shows the guide; uppercase `R` lists IDs whose Recipes view shows the **same** guide. Normally U lists the main controller. Extra U items or R entries are optional. At least one nonempty list is required. No legacy `associated_items` parsing is provided.

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

`layers` are bottom-to-top. Each layer is an ordered list of front-to-back rows, and each row is left-to-right. A space is air. Every layer must have the same number of rows and every row must have the same width.

Each palette entry must have exactly one of `block`, `tag`, or `any_of`. `any_of` options each contain exactly one `block` or `tag`; `default` must name one of those option IDs. The BOM counts one default per position, never every alternative. Dimensions are limited to 64 in each direction and 4,096 non-air positions per variant.

Invalid documents are skipped without crashing the client. The log identifies the resource and the failing variant/layer/row/cell path.

## Lookup rules and testing

Both arrays are optional but at least one must be nonempty. Use registered item IDs (not unplaceable block IDs). The same item may be listed under both U and R; both open **one shared guide**. Other ingredients are **not** made lookup triggers automatically. The JSON above is illustrative and references fictional `example:` IDs; replace them with real registered items before testing.

Version 0.2 ships with no multiblock definitions. Add a JSON definition through your datapack or resource pack before testing. Test each host with U and R separately, and confirm existing normal crafting/Uses entries remain available.

The structure definition format remains `"format": 1` in 0.2; this lookup-field change replaces the original draft, with no backward compatibility.
