package io.vertx.grpc.plugin;

import com.google.common.base.CharMatcher;
import com.google.common.base.Strings;
import com.google.common.html.HtmlEscapers;
import com.google.protobuf.DescriptorProtos;
import com.google.protobuf.DescriptorProtos.FileDescriptorProto;
import com.google.protobuf.DescriptorProtos.FileOptions;
import java.io.File;
import java.util.Arrays;

/**
 * @author <a href="mailto:eduard.catala@gmail.com">Eduard Català</a>
 */
public final class VertxGrpcUtil {

    public static String extractPackageName(FileDescriptorProto proto) {
        FileOptions options = proto.getOptions();
        if (options != null) {
            String javaPackage = options.getJavaPackage();
            if (!Strings.isNullOrEmpty(javaPackage)) {
                return javaPackage;
            }
        }

        return Strings.nullToEmpty(proto.getPackage());
    }

    public static String absoluteFileName(ServiceMetadata service) {
        String dir = service.packageName.replace('.', File.separatorChar);
        if (Strings.isNullOrEmpty(dir)) {
            return service.fileName;
        } else {
            return dir + File.separator + service.fileName;
        }
    }

    public static String getComments(DescriptorProtos.SourceCodeInfo.Location location) {
        return location.getLeadingComments().isEmpty() ? location.getTrailingComments() : location.getLeadingComments();
    }

    public static String getJavaDoc(String comments, String prefix) {
        if (!comments.isEmpty()) {
            StringBuilder builder = new StringBuilder("/**\n")
                    .append(prefix).append(" * <pre>\n");
            Arrays.stream(HtmlEscapers.htmlEscaper().escape(comments).split("\n"))
                    .map(line -> line.replace("*/", "&#42;&#47;").replace("*", "&#42;"))
                    .forEach(line -> builder.append(prefix).append(" * ").append(line).append("\n"));
            builder
                    .append(prefix).append(" * </pre>\n")
                    .append(prefix).append(" */");
            return builder.toString();
        }
        return null;
    }

    public static String lowerCaseFirst(String s) {
        return Character.toLowerCase(s.charAt(0)) + s.substring(1);
    }

    public static String upcaseAfterNumber(String filename) {
        char[] filechars = filename.toCharArray();
        for (int i = 1; i < filechars.length; i++) {
            if (CharMatcher.inRange('0', '9').matches(filechars[i - 1])) {
                filechars[i] = Character.toUpperCase(filechars[i]);
            }
        }
        return new String(filechars);
    }

    public static String makeInvalidCharactersUnderscores(String filename) {
        // https://developers.google.com/protocol-buffers/docs/reference/proto3-spec
        char[] filechars = filename.toCharArray();
        for (int i = 0; i < filechars.length; i++) {
            char c = filechars[i];
            if (!CharMatcher.inRange('0', '9').or(CharMatcher.inRange('A', 'Z')).or(CharMatcher.inRange('a', 'z')).matches(c)) {
                filechars[i] = '_';
            }
        }
        return new String(filechars);
    }
}
