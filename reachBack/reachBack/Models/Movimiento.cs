namespace reachBack.Models
{
    public class Movimiento
    {
        public int Id { get; set; }
        public int ProductoId { get; set; }
        public Producto? Producto { get; set; }

        public string TipoMovimiento { get; set; } = "Entrada"; // Entrada / Salida 
        public int Cantidad { get; set; }
        public DateTime Fecha { get; set; } = DateTime.Now;
        public string Motivo { get; set; } = string.Empty; // Venta, Ensamble, Abastecimiento
        public int? PedidoId { get; set; }
        public string AdministradorResponsable { get; set; } = string.Empty;
    }
}
