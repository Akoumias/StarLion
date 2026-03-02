package model;

/**
 * Enum representing the types of objects in the semantic web model.
 * This is a replacement for the package-private SEMWEB_OBJECT_TYPE in the graphs package.
 * 
 * @author jakoum
 */
public enum ObjectType {
    CLASS,
    CLASS_INSTANCE,
    PROPERTY,
    PROPERTY_INSTANCE,
    SUBCLASS_OF,
    INSTANCE_OF
}