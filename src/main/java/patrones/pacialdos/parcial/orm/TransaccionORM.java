package patrones.pacialdos.parcial.orm;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transaccion")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransaccionORM {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "origen", nullable = false)
    private CuentaORM origen;

    @ManyToOne
    @JoinColumn(name = "destino", nullable = false)
    private CuentaORM destino;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal monto;

    @Column(nullable = false, updatable = false)
    private LocalDateTime timestamp = LocalDateTime.now();
}

