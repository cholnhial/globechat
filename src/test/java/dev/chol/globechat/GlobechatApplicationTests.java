package dev.chol.globechat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class GlobechatApplicationTests {

	@Test
	void contextLoads() {
	}

}
