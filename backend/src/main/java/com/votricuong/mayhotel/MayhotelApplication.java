package com.votricuong.mayhotel;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.convert.DefaultMongoTypeMapper;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;

@SpringBootApplication
@EnableScheduling
public class MayhotelApplication implements InitializingBean {

    @Autowired
    private MappingMongoConverter mappingMongoConverter;

	public static void main(String[] args) {
		SpringApplication.run(MayhotelApplication.class, args);
	}

    @Override
    public void afterPropertiesSet() throws Exception {
        // Loại bỏ trường _class khi lưu vào MongoDB
        mappingMongoConverter.setTypeMapper(new DefaultMongoTypeMapper(null));
    }
}
