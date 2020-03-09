package net.comorevi.cosse.chestguard.util;

public enum ProtectType {
    PROTECT_TYPE_DEFAULT(1),
    PROTECT_TYPE_PASSWORD(2),
    PROTECT_TYPE_SHARE(3),
    PROTECT_TYPE_PUBLIC(4);

    private final int id;

    private ProtectType(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public static ProtectType getById(int id) {
        for (ProtectType type : ProtectType.values()) {
            if (type.getId() == id) {
                return type;
            }
        }
        throw new IllegalArgumentException("No such enum object for the id: " + id);
    }
}
