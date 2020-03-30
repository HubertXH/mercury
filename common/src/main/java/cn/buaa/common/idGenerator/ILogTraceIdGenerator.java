package cn.buaa.common.idGenerator;

public interface ILogTraceIdGenerator {

    String generate();

    String generate(int randomStringLength);
}
