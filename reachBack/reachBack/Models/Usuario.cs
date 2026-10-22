namespace reachBack.Models
{
    public class Usuario
    {
        public int Id { get; set; }
        public string Nombre { get; set; } = string.Empty;
        public string Email { get; set; } = string.Empty;
        public string Password { get; set; } = string.Empty;
        public string Rol { get; set; } = "Comprador"; // Comprador, Empleado, Administrador

        // Relaciones
        public List<Pedido> Pedidos { get; set; } = new();
        public List<CarritoItem> CarritoItems { get; set; } = new();
    }
}