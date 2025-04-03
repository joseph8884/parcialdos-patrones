package patrones.pacialdos.parcial;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import patrones.pacialdos.parcial.services.TransaccionService;

@SpringBootApplication
public class ParcialApplication {

	public static void main(String[] args) {
		SpringApplication.run(ParcialApplication.class, args);
	}

}
