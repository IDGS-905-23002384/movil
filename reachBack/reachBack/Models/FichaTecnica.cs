namespace reachBack.Models
{
    public class FichaTecnica
    {
        public int Id { get; set; }
        public string NombreEquipoPrearmado { get; set; } = string.Empty;
        public string CategoriaUso { get; set; } = string.Empty; // Ofimática, Arquitectura, Desarrollo
        public decimal CostoManoObra { get; set; }
        public decimal PrecioSugerido { get; set; } 

        public List<FichaTecnicaDetalle> ComponentesRequeridos { get; set; } = new();
    }
}
