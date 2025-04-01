package patrones.pacialdos.parcial.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import patrones.pacialdos.parcial.orm.TransaccionORM;

@Repository
public interface TransaccionJPA extends JpaRepository<TransaccionORM, Long> {
}
