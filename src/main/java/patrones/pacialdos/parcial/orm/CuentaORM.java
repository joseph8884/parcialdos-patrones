package patrones.pacialdos.parcial.orm;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "cuenta")
@NoArgsConstructor
@AllArgsConstructor
@Data
public class CuentaORM {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 50)
    private String nombre;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal monto;
}
