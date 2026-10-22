using Microsoft.EntityFrameworkCore;
using reachBack.Models;

namespace reachBack.Data
{
    public class ApplicationDbContext : DbContext
    {
        public ApplicationDbContext(DbContextOptions<ApplicationDbContext> options) : base(options) { }

        public DbSet<Usuario> Usuarios { get; set; }
        public DbSet<Categoria> Categorias { get; set; }
        public DbSet<Proveedor> Proveedores { get; set; }
        public DbSet<Producto> Productos { get; set; }
        public DbSet<FichaTecnica> FichasTecnicas { get; set; }
        public DbSet<FichaTecnicaDetalle> FichaTecnicaDetalles { get; set; }
        public DbSet<Pedido> Pedidos { get; set; }
        public DbSet<PedidoDetalle> PedidoDetalles { get; set; }
        public DbSet<CarritoItem> CarritoItems { get; set; }
        public DbSet<Movimiento> MovimientosKardex { get; set; }
    }
}