package patrones.pacialdos.parcial.services;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@AllArgsConstructor
@Data
public class TransaccioEstado {
    private final String id;
    private final String origen;
    private final String destino;
    private final BigDecimal monto;
    private boolean exitosa;
    private boolean fallida;
    private final LocalDateTime timestamp;

    public TransaccioEstado(String transaccionId, String abc, String cbd, BigDecimal monto, boolean exitosa) {
        this.id = transaccionId;
        this.origen = abc;
        this.destino = cbd;
        this.monto = monto;
        this.exitosa = exitosa;
        this.fallida = !exitosa;
        this.timestamp = LocalDateTime.now();
    }
}
