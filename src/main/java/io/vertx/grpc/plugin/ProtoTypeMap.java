package io.vertx.grpc.plugin;

import com.google.common.base.CaseFormat;
import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.protobuf.DescriptorProtos.DescriptorProto;
import com.google.protobuf.DescriptorProtos.FileDescriptorProto;
import com.google.protobuf.DescriptorProtos.FileOptions;

import java.util.Collection;
/**
 * Based on ProtoTypeMap from salesforce jprotoc
 * @author <a href="mailto:eduard.catala@gmail.com">Eduard Català</a>
 */

public final class ProtoTypeMap {

    private static final Joiner DOT_JOINER = Joiner.on('.').skipNulls();
    private final ImmutableMap<String, String> types;

    private ProtoTypeMap(ImmutableMap<String, String> types) {
        this.types = types;
    }

    public static ProtoTypeMap of(Collection<FileDescriptorProto> fileDescriptorProtos) {

        ImmutableMap.Builder<String, String> types = ImmutableMap.builder();

        for (FileDescriptorProto fileDescriptor : fileDescriptorProtos) {
            FileOptions fileOptions = fileDescriptor.getOptions();

            String protoPackage = fileDescriptor.hasPackage() ? "." + fileDescriptor.getPackage() : "";
            String javaPackage = Strings.emptyToNull(fileOptions.hasJavaPackage() ? fileOptions.getJavaPackage() : fileDescriptor.getPackage());
            String enclosingClassName = fileOptions.getJavaMultipleFiles() ? null : getJavaOuterClassname(fileDescriptor, fileOptions);

            // Identify top-level enums
            fileDescriptor.getEnumTypeList().forEach(e -> types.put(protoPackage + "." + e.getName(), DOT_JOINER.join(javaPackage, enclosingClassName, e.getName())));

            // Identify top-level messages, and nested types
            fileDescriptor.getMessageTypeList().forEach(
                    m -> recursivelyAddTypes(types, m, protoPackage, enclosingClassName, javaPackage)
            );
        }

        return new ProtoTypeMap(types.build());
    }

    private static void recursivelyAddTypes(ImmutableMap.Builder<String, String> types, DescriptorProto m, String protoPackage, String enclosingClassName, String javaPackage) {
        // Add current type
        String protoTypeName = protoPackage + "." + m.getName();
        types.put(protoTypeName, DOT_JOINER.join(javaPackage, enclosingClassName, m.getName()));

        // Add current type nested Enums
        m.getEnumTypeList().forEach(
                e -> types.put(protoPackage + "." + m.getName() + "." + e.getName(), DOT_JOINER.join(javaPackage, enclosingClassName, m.getName(), e.getName()))
        );

        // Add current type nested types
        m.getNestedTypeList().forEach(
                n -> recursivelyAddTypes(types, n, protoPackage + "." + m.getName(), DOT_JOINER.join(enclosingClassName, m.getName()), javaPackage)
        );
    }

    public String toJavaTypeName(String protoTypeName) {
        return types.get(protoTypeName);
    }

    private static String getJavaOuterClassname(FileDescriptorProto fileDescriptor, FileOptions fileOptions) {

        if (fileOptions.hasJavaOuterClassname()) {
            return fileOptions.getJavaOuterClassname();
        }

        String filename = fileDescriptor.getName().substring(0, fileDescriptor.getName().length() - ".proto".length());

        if (filename.contains("/")) {
            filename = filename.substring(filename.lastIndexOf('/') + 1);
        }

        filename = VertxGrpcUtil.makeInvalidCharactersUnderscores(filename);
        filename = CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, filename);
        filename = VertxGrpcUtil.upcaseAfterNumber(filename);
        filename = appendOuterClassSuffix(filename, fileDescriptor);
        return filename;
    }

    //  In the event of a name conflict between the outer and inner type names, protoc adds an OuterClass suffix to the outer type's name.
    private static String appendOuterClassSuffix(final String enclosingClassName, FileDescriptorProto fd) {
        if (fd.getEnumTypeList().stream().anyMatch(enumProto -> enumProto.getName().equals(enclosingClassName))
                || fd.getMessageTypeList().stream().anyMatch(messageProto -> messageProto.getName().equals(enclosingClassName))
                || fd.getServiceList().stream().anyMatch(serviceProto -> serviceProto.getName().equals(enclosingClassName))) {
            return enclosingClassName + "OuterClass";
        } else {
            return enclosingClassName;
        }
    }

}
