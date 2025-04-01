package patrones.pacialdos.parcial;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import patrones.pacialdos.parcial.services.TransaccionService;

@SpringBootApplication
public class ParcialApplication implements CommandLineRunner {

	@Autowired
	private TransaccionService transaccionService;

	public static void main(String[] args) {
		SpringApplication.run(ParcialApplication.class, args);
	}

	@Override
	public void run(String... args) {
		transaccionService.iniciarTransaccionesConcurrentes();
	}
}
