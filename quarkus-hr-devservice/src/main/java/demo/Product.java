package demo;

import io.quarkus.hibernate.reactive.panache.PanacheEntity;
import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import io.smallrye.mutiny.Uni;

import jakarta.persistence.Entity;
import java.math.BigDecimal;
import java.util.List;

@Entity
public class Product extends PanacheEntity {

  public String name;
  public BigDecimal price;

  public static Uni<List<Product>> all() {
    return findAll().list();
  }

  public static Uni<Product> findByName(String name) {
    return find("name", name).firstResult();
  }
}
