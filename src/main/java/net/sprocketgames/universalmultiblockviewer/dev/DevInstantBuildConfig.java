package net.sprocketgames.universalmultiblockviewer.dev;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Private development switch. It is disabled in every new installation. */
public final class DevInstantBuildConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    public static final ModConfigSpec.BooleanValue ENABLED = BUILDER
        .comment("Enables the private developer Build Here control. Requires Creative mode and operator permission.")
        .define("devInstantBuild", false);
    public static final ModConfigSpec SPEC = BUILDER.build();

    private DevInstantBuildConfig() { }

    public static boolean enabled() { return ENABLED.get(); }
}
