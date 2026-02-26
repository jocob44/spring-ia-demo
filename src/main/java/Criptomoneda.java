public class Criptomoneda {
    private String nombre;
    private String simbolo;
    private double precio;
    private int cantidad;

    public Criptomoneda(String nombre, String simbolo, double precio, int cantidad) {
        this.nombre = nombre;
        this.simbolo = simbolo;
        this.precio = precio;
        this.cantidad = cantidad;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getSimbolo() {
        return simbolo;
    }

    public void setSimbolo(String simbolo) {
        this.simbolo = simbolo;
    }

    public double getPrecio() {
        return precio;
    }

    public void setPrecio(double precio) {
        this.precio = precio;
    }

    public int getCantidad() {
        return cantidad;
    }

    public void setCantidad(int cantidad) {
        this.cantidad = cantidad;
    }

    @Override
    public String toString() {
        return "Criptomoneda [nombre=" + nombre + ", simbolo=" + simbolo + ", precio=" + precio + ", cantidad=" + cantidad + "]";
    }
}