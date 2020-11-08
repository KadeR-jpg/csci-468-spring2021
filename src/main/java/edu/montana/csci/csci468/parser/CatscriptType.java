package edu.montana.csci.csci468.parser;

import java.util.List;
import java.util.Objects;

public class CatscriptType {

    public static final CatscriptType INT = new CatscriptType("int", Integer.class);
    public static final CatscriptType STRING = new CatscriptType("string", String.class);
    public static final CatscriptType BOOLEAN = new CatscriptType("bool", Boolean.class);
    public static final CatscriptType OBJECT = new CatscriptType("object", Object.class);
    public static final CatscriptType NULL = new CatscriptType("null", Object.class);

    private final String name;
    private final Class javaClass;

    public CatscriptType(String name, Class javaClass) {
        this.name = name;
        this.javaClass = javaClass;
    }

    public boolean isAssignableFrom(CatscriptType type) {
        if (type == NULL) {
            return true;
        } else if (this.javaClass.isAssignableFrom(type.javaClass)) {
            return true;
        }
        return false;
    }

    public static CatscriptType getListType(CatscriptType type) {
        return new ListType(type);
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CatscriptType that = (CatscriptType) o;
        return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    static class ListType extends CatscriptType {
        private final CatscriptType componentType;
        public ListType(CatscriptType componentType) {
            super("list", List.class);
            this.componentType = componentType;
        }

        @Override
        public boolean isAssignableFrom(CatscriptType type) {
            if (type == NULL) {
                return true;
            } else if (type instanceof ListType) {
                return this.componentType.isAssignableFrom(((ListType) type).componentType);
            }
            return false;
        }

        @Override
        public String toString() {
            return super.toString() + "<" + componentType.toString() + ">";
        }
    }

}
