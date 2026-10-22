namespace reachBack.Models
{
    public class FichaTecnicaDetalle
    {
        public int Id { get; set; }
        public int FichaTecnicaId { get; set; }
        public FichaTecnica? FichaTecnica { get; set; }

        public int ProductoId { get; set; } 
        public Producto? Producto { get; set; }
        public int Cantidad { get; set; }
    }
}
