package net.maxello.knowledgebound.config;

import net.maxello.knowledgebound.KnowledgeBound;
/**
 * Represents a single editable config field in the admin GUI.
 */
public final class ConfigGuiEntry {

    public enum EntryType {
        BOOLEAN,
        INTEGER,
        DOUBLE,
        STRING
    }

    private final String configPath;
    private final String displayName;
    private final String description;
    private final EntryType type;
    private final double min;
    private final double max;
    private final double smallStep;
    private final double largeStep;

    private ConfigGuiEntry(Builder builder) {
        this.configPath = builder.configPath;
        this.displayName = builder.displayName;
        this.description = builder.description;
        this.type = builder.type;
        this.min = builder.min;
        this.max = builder.max;
        this.smallStep = builder.smallStep;
        this.largeStep = builder.largeStep;
    }

    public String getConfigPath() { return configPath; }
    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
    public EntryType getType() { return type; }
    public double getMin() { return min; }
    public double getMax() { return max; }
    public double getSmallStep() { return smallStep; }
    public double getLargeStep() { return largeStep; }

    // Convenience factories

    public static ConfigGuiEntry bool(String path, String name, String desc) {
        return new Builder(path, name, desc, EntryType.BOOLEAN).build();
    }

    public static ConfigGuiEntry integer(String path, String name, String desc, int min, int max) {
        return new Builder(path, name, desc, EntryType.INTEGER)
                .range(min, max).steps(1, 10).build();
    }

    public static ConfigGuiEntry decimal(String path, String name, String desc, double min, double max) {
        return new Builder(path, name, desc, EntryType.DOUBLE)
                .range(min, max).steps(0.01, 0.1).build();
    }

    public static ConfigGuiEntry decimal(String path, String name, String desc, double min, double max,
                                          double small, double large) {
        return new Builder(path, name, desc, EntryType.DOUBLE)
                .range(min, max).steps(small, large).build();
    }

    public static ConfigGuiEntry string(String path, String name, String desc) {
        return new Builder(path, name, desc, EntryType.STRING).build();
    }

    public static class Builder {
        private final String configPath;
        private final String displayName;
        private final String description;
        private final EntryType type;
        private double min = Integer.MIN_VALUE;
        private double max = Integer.MAX_VALUE;
        private double smallStep = 1;
        private double largeStep = 10;

        public Builder(String configPath, String displayName, String description, EntryType type) {
            this.configPath = configPath;
            this.displayName = displayName;
            this.description = description;
            this.type = type;
        }

        public Builder range(double min, double max) {
            this.min = min;
            this.max = max;
            return this;
        }

        public Builder steps(double small, double large) {
            this.smallStep = small;
            this.largeStep = large;
            return this;
        }

        public ConfigGuiEntry build() {
            return new ConfigGuiEntry(this);
        }
    }
}


