namespace reachBack.Models
{
    public class Venta
    {
        public int Id { get; set; }
        public DateTime Fecha { get; set; } = DateTime.Now;

        public string Cliente { get; set; } = string.Empty;
        public decimal Subtotal { get; set; }
        public decimal Iva { get; set; }
        public decimal Total { get; set; }
        public string Estado { get; set; } = "Pendiente"; 

        // Una venta tiene una LISTA de detalles (los productos que compro)
        public List<VentaDetalle> Detalles { get; set; } = new();
    }
}