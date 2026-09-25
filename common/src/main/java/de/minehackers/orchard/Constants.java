package de.minehackers.orchard;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// Shared mod constants (id, name, logger, NBT size limit).
public final class Constants {

    private Constants() {}

    public static final String MOD_ID = "orchard";
    public static final String MOD_NAME = "Orchard";
    public static final Logger LOG = LoggerFactory.getLogger(MOD_ID);
    public static final long MAX_NBT_FILE_SIZE = 10 * 1024 * 1024;
}
