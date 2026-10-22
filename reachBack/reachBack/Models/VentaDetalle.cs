namespace reachBack.Models
{
    public class VentaDetalle
    {
        public int Id { get; set; }

        // a una sola Venta
        public int VentaId { get; set; }
        public Venta? Venta { get; set; }

        // Producto que se está comprando 
        public int ProductoId { get; set; }
        public Producto? Producto { get; set; }

        public int Cantidad { get; set; }
        public decimal PrecioUnitario { get; set; }
        public decimal SubtotalDetalle { get; set; } // Cantidad * PrecioUnitario
    }
}