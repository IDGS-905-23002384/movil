namespace reachBack.Models
{
    public class Producto
    {
        public int Id { get; set; }
        public string Nombre { get; set; } = string.Empty;
        public string Marca { get; set; } = string.Empty;
        public string Descripcion { get; set; } = string.Empty;
        public decimal Precio { get; set; }
        public int Stock { get; set; }
        public int StockMinimo { get; set; } = 5; 

        // "Almacen", "Terminado", "PorEnviar"
        public string TipoInventario { get; set; } = "Almacen";

        public string EspecificacionesTecnicas { get; set; } = string.Empty; 

        public int CategoriaId { get; set; }
        public Categoria? Categoria { get; set; }
    }
}