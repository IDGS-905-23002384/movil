namespace reachBack.Models
{
    public class Proveedor
    {
        public int Id { get; set; }
        public string NombreEmpresa { get; set; } = string.Empty;
        public string ContactoPersona { get; set; } = string.Empty;
        public string Telefono { get; set; } = string.Empty;
        public string Email { get; set; } = string.Empty;

        public List<Producto> Productos { get; set; } = new();
    }
}