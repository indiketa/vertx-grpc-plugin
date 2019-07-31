package io.vertx.grpc.plugin;

import com.google.protobuf.ByteString;
import com.google.protobuf.compiler.PluginProtos.CodeGeneratorRequest;
import com.google.protobuf.compiler.PluginProtos.CodeGeneratorResponse;
import java.io.IOException;

/**
 * @author <a href="mailto:eduard.catala@gmail.com">Eduard Català</a>
 */
public class Generator  {

    public static void main(String[] args) {

        try {
            ByteString bs = ByteString.readFrom(System.in);

            CodeGeneratorRequest request = CodeGeneratorRequest.parseFrom(bs);

            CodeGeneratorResponse.newBuilder()
                    .addAllFile(VertxGrpcGenerator.generateFiles(request))
                    .build()
                    .writeTo(System.out);

        } catch (Exception ex) {
            try {
                CodeGeneratorResponse
                        .newBuilder()
                        .setError(ex.getMessage())
                        .build()
                        .writeTo(System.out);
            } catch (IOException ex2) {
                abort(ex2);
            }
        } catch (Throwable ex) {
            abort(ex);
        }
    }

    private static void abort(Throwable ex) {
        ex.printStackTrace(System.err);
        System.exit(1);
    }
}
