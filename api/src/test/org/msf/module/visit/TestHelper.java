package org.msf.module.visit;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public class TestHelper {

    public static void setValuesForMemberFields(Object classInstance, String fieldName, Object valueForMemberField)
            throws Exception {
        setField(classInstance, valueForMemberField, classInstance.getClass().getDeclaredField(fieldName));
    }

    private static void setField(Object classInstance, Object valueForMemberField, Field field)
            throws IllegalAccessException {
        field.setAccessible(true);
        field.set(classInstance, valueForMemberField);
    }

    public static void setValueForFinalStaticField(Class classInstance, String fieldName, Object valueForMemberField)
            throws Exception {
        Field field = classInstance.getDeclaredField(fieldName);
        Field modifiersField = Field.class.getDeclaredField("modifiers");
        modifiersField.setAccessible(true);
        modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);
        setField(null, valueForMemberField, field);
    }
}
