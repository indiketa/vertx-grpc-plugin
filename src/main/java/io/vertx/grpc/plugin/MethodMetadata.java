package io.vertx.grpc.plugin;

import com.google.common.base.Strings;

/**
 * @author <a href="mailto:eduard.catala@gmail.com">Eduard Català</a>
 */
public class MethodMetadata {

    public String methodName;
    public String inputType;
    public String outputType;
    public boolean deprecated;
    public boolean isManyInput;
    public boolean isManyOutput;
    public String grpcCallsMethodName;
    public String grpcServerCallsMethodName;
    public int methodNumber;
    public String javaDoc;

    // This method mimics the upper-casing method ogf gRPC to ensure compatibility
    // See https://github.com/grpc/grpc-java/blob/v1.8.0/compiler/src/java_plugin/cpp/java_generator.cpp#L58
    public String methodNameUpperUnderscore() {
        StringBuilder s = new StringBuilder();
        for (int i = 0; i < methodName.length(); i++) {
            char c = methodName.charAt(i);
            s.append(Character.toUpperCase(c));
            if ((i < methodName.length() - 1) && Character.isLowerCase(c) && Character.isUpperCase(methodName.charAt(i + 1))) {
                s.append('_');
            }
        }
        return s.toString();
    }

    public String methodNamePascalCase() {
        String mn = methodName.replace("_", "");
        return String.valueOf(Character.toUpperCase(mn.charAt(0))) + mn.substring(1);
    }

    public String methodNameCamelCase() {
        String mn = methodName.replace("_", "");
        return String.valueOf(Character.toLowerCase(mn.charAt(0))) + mn.substring(1);
    }

    public String methodHeader() {
        String mh = "";
        if (!Strings.isNullOrEmpty(javaDoc)) {
            mh = javaDoc;
        }

        if (deprecated) {
            mh += "\n        @Deprecated";
        }

        mh += "\n        @SuppressWarnings(\"unchecked\")";

        return mh;
    }

}
