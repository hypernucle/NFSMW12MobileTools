package util.json;

public @interface JsonClassSubType {
    Class<?> jsonClass();

    String name();
}
