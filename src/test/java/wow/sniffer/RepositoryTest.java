package wow.sniffer;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class RepositoryTest implements CommandLineRunner {

    @Test
    public void test1() {
        SpringApplication.run(RepositoryTest.class);
    }

    @Override
    public void run(String... args) throws Exception {
        System.out.println();
    }
}
