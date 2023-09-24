package ecdar.abstractions;

import ecdar.backend.GrpcRequest;
import ecdar.backend.GrpcRequestFactory;

import java.util.function.Consumer;

public interface RequestSource <T> {
    GrpcRequest accept(GrpcRequestFactory requestFactory, Consumer<T> successConsumer, Consumer<Throwable> errorConsumer);
}
