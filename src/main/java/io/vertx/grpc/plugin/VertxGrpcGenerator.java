package io.vertx.grpc.plugin;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import com.google.common.base.Charsets;
import com.google.protobuf.DescriptorProtos.FileDescriptorProto;
import com.google.protobuf.DescriptorProtos.MethodDescriptorProto;
import com.google.protobuf.DescriptorProtos.ServiceDescriptorProto;
import com.google.protobuf.DescriptorProtos.SourceCodeInfo.Location;
import com.google.protobuf.compiler.PluginProtos.CodeGeneratorRequest;
import com.google.protobuf.compiler.PluginProtos.CodeGeneratorResponse;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author <a href="mailto:eduard.catala@gmail.com">Eduard Català</a>
 */
public class VertxGrpcGenerator {

    private static final int SERVICE_NUMBER_OF_PATHS = 2;
    private static final int METHOD_NUMBER_OF_PATHS = 4;
    private static final String CLASS_PREFIX = "";
    private static final String CLASS_SUFFIX = "VertxGrpc";
    private static MustacheFactory mustacheFactory = new DefaultMustacheFactory();

    public static List<CodeGeneratorResponse.File> generateFiles(CodeGeneratorRequest request) {

        final ProtoTypeMap typeMap = ProtoTypeMap.of(request.getProtoFileList());

        List<FileDescriptorProto> protosToGenerate = request.getProtoFileList()
                .stream()
                .filter(protoFile -> request.getFileToGenerateList().contains(protoFile.getName()))
                .collect(Collectors.toList());

        List<ServiceMetadata> services = findServices(protosToGenerate, typeMap);
        return generateServiceFiles(services);
    }

    private static List<ServiceMetadata> findServices(List<FileDescriptorProto> protos, ProtoTypeMap typeMap) {
        List<ServiceMetadata> services = new ArrayList<>();

        protos.forEach(fileProto -> {
            for (int serviceNr = 0; serviceNr < fileProto.getServiceCount(); serviceNr++) {
                services.add(buildServiceMetadata(fileProto, typeMap, serviceNr));
            }
        });

        return services;
    }

    private static ServiceMetadata buildServiceMetadata(FileDescriptorProto fileProto, ProtoTypeMap typeMap, int serviceNumber) {
        ServiceDescriptorProto serviceProto = fileProto.getService(serviceNumber);

        ServiceMetadata service = new ServiceMetadata();
        service.fileName = CLASS_PREFIX + serviceProto.getName() + CLASS_SUFFIX + ".java";
        service.className = CLASS_PREFIX + serviceProto.getName() + CLASS_SUFFIX;
        service.serviceName = serviceProto.getName();
        service.deprecated = serviceProto.getOptions() != null && serviceProto.getOptions().getDeprecated();
        service.protoName = fileProto.getName();
        service.packageName = VertxGrpcUtil.extractPackageName(fileProto);

        List<Location> locations = fileProto.getSourceCodeInfo().getLocationList();

        Location serviceLocation = locations.stream()
                .filter(l -> l.getPathCount() >= 2 && l.getPath(0) == FileDescriptorProto.SERVICE_FIELD_NUMBER && l.getPath(1) == serviceNumber)
                .filter(location -> location.getPathCount() == SERVICE_NUMBER_OF_PATHS)
                .findFirst()
                .orElseGet(Location::getDefaultInstance);

        service.javaDoc = VertxGrpcUtil.getJavaDoc(VertxGrpcUtil.getComments(serviceLocation), "");

        for (int methodNr = 0; methodNr < serviceProto.getMethodCount(); methodNr++) {
            service.methods.add(buildMethodMetadata(serviceProto.getMethod(methodNr), typeMap, locations, methodNr));
        }
        return service;
    }

    private static MethodMetadata buildMethodMetadata(MethodDescriptorProto methodProto, ProtoTypeMap typeMap, List<Location> locations, int methodNumber) {
        MethodMetadata method = new MethodMetadata();
        method.methodName = VertxGrpcUtil.lowerCaseFirst(methodProto.getName());
        method.inputType = typeMap.toJavaTypeName(methodProto.getInputType());
        method.outputType = typeMap.toJavaTypeName(methodProto.getOutputType());
        method.deprecated = methodProto.getOptions() != null && methodProto.getOptions().getDeprecated();
        method.isManyInput = methodProto.getClientStreaming();
        method.isManyOutput = methodProto.getServerStreaming();
        method.methodNumber = methodNumber;

        Location methodLocation = locations.stream()
                .filter(l -> l.getPathCount() == METHOD_NUMBER_OF_PATHS && l.getPath(METHOD_NUMBER_OF_PATHS - 1) == methodNumber)
                .findFirst()
                .orElseGet(Location::getDefaultInstance);
        method.javaDoc = VertxGrpcUtil.getJavaDoc(VertxGrpcUtil.getComments(methodLocation), "");

        if (!methodProto.getClientStreaming() && !methodProto.getServerStreaming()) {
            method.grpcCallsMethodName = "asyncUnaryCall";
            method.grpcServerCallsMethodName = "asyncUnimplementedUnaryCall";
        }
        if (!methodProto.getClientStreaming() && methodProto.getServerStreaming()) {
            method.grpcCallsMethodName = "asyncServerStreamingCall";
            method.grpcServerCallsMethodName = "asyncUnimplementedUnaryCall";
        }
        if (methodProto.getClientStreaming() && !methodProto.getServerStreaming()) {
            method.grpcCallsMethodName = "asyncClientStreamingCall";
            method.grpcServerCallsMethodName = "asyncUnimplementedStreamingCall";
        }
        if (methodProto.getClientStreaming() && methodProto.getServerStreaming()) {
            method.grpcCallsMethodName = "asyncBidiStreamingCall";
            method.grpcServerCallsMethodName = "asyncUnimplementedStreamingCall";
        }
        return method;
    }

    private static List<CodeGeneratorResponse.File> generateServiceFiles(List<ServiceMetadata> services) {
        return services.stream()
                .map(VertxGrpcGenerator::buildFile)
                .collect(Collectors.toList());
    }

    private static CodeGeneratorResponse.File buildFile(ServiceMetadata service) {

        String resourcePath = "vertx-service.moustache";

        InputStream resource = MustacheFactory.class.getClassLoader().getResourceAsStream(resourcePath);
        if (resource == null) {
            throw new RuntimeException("Could not find resource " + resourcePath);
        }

        InputStreamReader resourceReader = new InputStreamReader(resource, Charsets.UTF_8);

        Mustache template = mustacheFactory.compile(resourceReader, resourcePath);

        String content = template.execute(new StringWriter(), service).toString();;
        return CodeGeneratorResponse.File
                .newBuilder()
                .setName(VertxGrpcUtil.absoluteFileName(service))
                .setContent(content)
                .build();
    }

}
