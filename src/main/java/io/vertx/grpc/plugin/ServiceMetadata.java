package io.vertx.grpc.plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author <a href="mailto:eduard.catala@gmail.com">Eduard Català</a>
 */
public class ServiceMetadata {

    public String fileName;
    public String protoName;
    public String packageName;
    public String className;
    public String serviceName;
    public boolean deprecated;
    public String javaDoc;
    public List<MethodMetadata> methods = new ArrayList<>();

    public List<MethodMetadata> unaryUnaryMethods() {
        return methods.stream().filter(m -> !m.isManyInput && !m.isManyOutput).collect(Collectors.toList());
    }
    
    public List<MethodMetadata> unaryManyMethods() {
        return methods.stream().filter(m -> !m.isManyInput && m.isManyOutput).collect(Collectors.toList());
    }
    
    public List<MethodMetadata> manyUnaryMethods() {
        return methods.stream().filter(m -> m.isManyInput && !m.isManyOutput).collect(Collectors.toList());
    }
    
    public List<MethodMetadata> manyManyMethods() {
        return methods.stream().filter(m -> m.isManyInput && m.isManyOutput).collect(Collectors.toList());
    }

}
