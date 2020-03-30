package cn.buaa.common.idGenerator.impl;

import cn.buaa.common.idGenerator.ILogTraceIdGenerator;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Random;

@Component
public class RandomIdGeneratorImp implements ILogTraceIdGenerator {

    private static final Logger LOGGER = LoggerFactory.getLogger(RandomIdGeneratorImp.class);

    private static final int DEFAULT_RANDOM_LENGTH = 8;

    @Override
    public String generate() {
        return generate(DEFAULT_RANDOM_LENGTH);
    }

    @Override
    public String generate(int randomStringLength) {
        StringBuilder result = new StringBuilder();
        String localAddress = acquireLocalHost();
        String randomString = generateRandomString(randomStringLength);
        if (StringUtils.isNotBlank(localAddress)) {
            result.append(localAddress).append("-");
        }
        result.append(System.currentTimeMillis())
                .append("-")
                .append(randomString);
        return result.toString();
    }


    /**
     * 生成给定长度的字符串
     *
     * @param length 指定长度
     * @return 随机字符串
     */
    private String generateRandomString(int length) {
        char[] randomChars = new char[length];
        Random random = new Random();
        int count = 0;
        while (count < length) {
            int randomInt = random.nextInt(3);
            if (randomInt == 2) {
                randomChars[count] = (char) ('a' + random.nextInt('z' - 'a'));
            } else if (randomInt == 0) {
                randomChars[count] = (char) ('A' + random.nextInt('Z' - 'A'));
            } else {
                randomChars[count] = (char) ('0' + random.nextInt('9' - '0'));
            }
            count++;
        }
        return new String(randomChars);
    }

    private String acquireLocalHost() {
        try {
            String localHostAddress = InetAddress.getLocalHost().getHostAddress();
            String[] lastAddress = StringUtils.split(localHostAddress, "\\.");
            return ArrayUtils.isEmpty(lastAddress) ? null : lastAddress[lastAddress.length - 1];
        } catch (UnknownHostException e) {
            LOGGER.warn("can't get host address.", e);
        }
        return null;
    }
}
