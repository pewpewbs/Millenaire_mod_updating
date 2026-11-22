# Block-state compatibility boundary

Original culture/palette names are interpreted as 1.12.2 identifiers. `LegacyBlockStates.fromName(name, metadata)` uses the generated vanilla ID map and Mojang BlockStateData, then applies later BLOCK_STATE fixes through data version 2975. Named properties are matched against the generated old state/property table. Explicit properties that metadata formerly discarded (e.g. door hinge) are reapplied when supported by the modern block. Custom blocks retain LegacyVariantBlock codecs.

New ValueIO block-state and block-ID output is versioned: `v2975/minecraft:grass;` identifies the modern grass plant. Unversioned `minecraft:grass;` identifies the legacy grass block. New named states are validated against the modern registry/property definitions. This marker is internal to Millenaire text config IO; it is not a Minecraft registry namespace.

The generated text tables came from the named ID_BY_OLD_NAME and ID_BY_OLD maps in Minecraft 1.18.2 BlockStateData. tools/DumpLegacyBlockIds.java reproduces them offline; production code does not use reflection. Do not guess old families from a modern Block reference: e.g. stone_slab/double_stone_slab need the original identity. Existing generic legacy metadata calls in building/world helpers still require explicit migration.

Standalone checks use real Forge/Minecraft classes with declared fixtures for unavailable mod services. They do not validate complete world construction, networking, world generation, or a playable game.
