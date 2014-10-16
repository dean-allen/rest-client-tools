package com.opower.rest.client.generator.util;

import java.lang.reflect.Array;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.HashMap;
import java.util.Map;

/**
 * Type conversions and generic type manipulations
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class Types {

    public static Class<?> getRawType(Type type) {
        if (type instanceof Class<?>) {
            // type is a normal class.
            return (Class<?>) type;

        } else if (type instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) type;
            Type rawType = parameterizedType.getRawType();
            return (Class<?>) rawType;
        } else if (type instanceof GenericArrayType) {
            final GenericArrayType genericArrayType = (GenericArrayType) type;
            final Class<?> componentRawType = getRawType(genericArrayType.getGenericComponentType());
            return Array.newInstance(componentRawType, 0).getClass();
        } else if (type instanceof TypeVariable) {
            final TypeVariable typeVar = (TypeVariable) type;
            if (typeVar.getBounds() != null && typeVar.getBounds().length > 0) {
                return getRawType(typeVar.getBounds()[0]);
            }
        }
        throw new RuntimeException("Unable to determine base class from Type");
    }

    private static final Type[] EMPTY_TYPE_ARRAY = {};

    private static Map<String, Type> populateParameterizedMap(Class<?> root, ParameterizedType rootType) {
        Map<String, Type> typeVarMap = new HashMap<String, Type>();
        if (rootType != null) {
            TypeVariable<? extends Class<?>>[] vars = root.getTypeParameters();
            for (int i = 0; i < vars.length; i++) {
                typeVarMap.put(vars[i].getName(), rootType.getActualTypeArguments()[i]);
            }
        }
        return typeVarMap;
    }


    public static Type[] findInterfaceParameterizedTypes(Class<?> root, ParameterizedType rootType, Class<?> searchedForInterface) {
        Map<String, Type> typeVarMap = populateParameterizedMap(root, rootType);

        for (int i = 0; i < root.getInterfaces().length; i++) {
            Class<?> sub = root.getInterfaces()[i];
            Type genericSub = root.getGenericInterfaces()[i];
            if (sub.equals(searchedForInterface)) {
                return extractTypes(typeVarMap, genericSub);
            }
        }

        for (int i = 0; i < root.getInterfaces().length; i++) {
            Type genericSub = root.getGenericInterfaces()[i];
            Class<?> sub = root.getInterfaces()[i];

            Type[] types = recurseSuperclassForInterface(searchedForInterface, typeVarMap, genericSub, sub);
            if (types != null) return types;
        }
        if (root.isInterface()) return null;

        Class<?> superclass = root.getSuperclass();
        Type genericSuper = root.getGenericSuperclass();


        return recurseSuperclassForInterface(searchedForInterface, typeVarMap, genericSuper, superclass);
    }
    
    public static Class getTemplateParameterOfInterface(Class base, Class desiredInterface)
    {
        Object rtn = getSomething(base, desiredInterface);
        if (rtn != null && rtn instanceof Class) return (Class) rtn;
        return null;
    }

    private static Object getSomething(Class base, Class desiredInterface)
    {
        for (int i = 0; i < base.getInterfaces().length; i++)
        {
            Class intf = base.getInterfaces()[i];
            if (intf.equals(desiredInterface))
            {
                Type generic = base.getGenericInterfaces()[i];
                if (generic instanceof ParameterizedType)
                {
                    ParameterizedType p = (ParameterizedType) generic;
                    Type type = p.getActualTypeArguments()[0];
                    Class rtn = getRawTypeNoException(type);
                    if (rtn != null) return rtn;
                    return type;
                }
                else
                {
                    return null;
                }
            }
        }
        if (base.getSuperclass() == null || base.getSuperclass().equals(Object.class)) return null;
        Object rtn = getSomething(base.getSuperclass(), desiredInterface);
        if (rtn == null || rtn instanceof Class) return rtn;
        if (!(rtn instanceof TypeVariable)) return null;

        String name = ((TypeVariable) rtn).getName();
        int index = -1;
        TypeVariable[] variables = base.getSuperclass().getTypeParameters();
        if (variables == null || variables.length < 1) return null;

        for (int i = 0; i < variables.length; i++)
        {
            if (variables[i].getName().equals(name)) index = i;
        }
        if (index == -1) return null;


        Type genericSuperclass = base.getGenericSuperclass();
        if (!(genericSuperclass instanceof ParameterizedType)) return null;

        ParameterizedType pt = (ParameterizedType) genericSuperclass;
        Type type = pt.getActualTypeArguments()[index];

        Class clazz = getRawTypeNoException(type);
        if (clazz != null) return clazz;
        return type;
    }

    public static Class<?> getRawTypeNoException(Type type)
    {
        if (type instanceof Class<?>)
        {
            // type is a normal class.
            return (Class<?>) type;

        }
        else if (type instanceof ParameterizedType)
        {
            ParameterizedType parameterizedType = (ParameterizedType) type;
            Type rawType = parameterizedType.getRawType();
            return (Class<?>) rawType;
        }
        else if (type instanceof GenericArrayType)
        {
            final GenericArrayType genericArrayType = (GenericArrayType) type;
            final Class<?> componentRawType = getRawType(genericArrayType.getGenericComponentType());
            return Array.newInstance(componentRawType, 0).getClass();
        }
        return null;
    }

    private static Type[] recurseSuperclassForInterface(Class<?> searchedForInterface, Map<String, Type> typeVarMap, Type genericSub, Class<?> sub) {
        if (genericSub instanceof ParameterizedType) {
            ParameterizedType intfParam = (ParameterizedType) genericSub;
            Type[] types = findInterfaceParameterizedTypes(sub, intfParam, searchedForInterface);
            if (types != null) {
                return extractTypeVariables(typeVarMap, types);
            }
        } else {
            Type[] types = findInterfaceParameterizedTypes(sub, null, searchedForInterface);
            if (types != null) {
                return types;
            }
        }
        return null;
    }

    private static Type[] extractTypeVariables(Map<String, Type> typeVarMap, Type[] types) {
        for (int j = 0; j < types.length; j++) {
            if (types[j] instanceof TypeVariable) {
                TypeVariable tv = (TypeVariable) types[j];
                types[j] = typeVarMap.get(tv.getName());
            } else {
                types[j] = types[j];
            }
        }
        return types;
    }

    private static Type[] extractTypes(Map<String, Type> typeVarMap, Type genericSub) {
        if (genericSub instanceof ParameterizedType) {
            ParameterizedType param = (ParameterizedType) genericSub;
            Type[] types = param.getActualTypeArguments();
            Type[] returnTypes = new Type[types.length];
            System.arraycopy(types, 0, returnTypes, 0, types.length);
            extractTypeVariables(typeVarMap, returnTypes);
            return returnTypes;
        } else {
            return EMPTY_TYPE_ARRAY;
        }
    }
}
