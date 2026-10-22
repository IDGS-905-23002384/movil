namespace reachBack.Models
{
    public class Pedido
    {
        public int Id { get; set; }
        public int UsuarioId { get; set; } // Comprador
        public Usuario? Usuario { get; set; }

        public DateTime Fecha { get; set; } = DateTime.Now;
        public decimal Subtotal { get; set; }

        public decimal CostoManoObra { get; set; } // Si incluye ensamble personalizado 

        public decimal Iva { get; set; }
        public decimal Total { get; set; }

        // Estados exigidos por RF3.3 y RF6.2
        public string EstadoArmado { get; set; } = "Atendido"; // Atendido, Ensamblando, Terminado
        public string EstadoEnvio { get; set; } = "Preparando paquete"; // Preparando paquete, Enviado, Entregado

        public List<PedidoDetalle> Detalles { get; set; } = new();
    }
}