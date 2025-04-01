package patrones.pacialdos.parcial.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransaccionDTO(Long id, Long origen, Long destino, BigDecimal monto, LocalDateTime timestamp) {
}
