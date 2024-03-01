package demo;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.transaction.Transactional;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;

import java.util.List;

@Path("/products")
public class CatalogApi {

  private static final Logger logger = LoggerFactory.getLogger(CatalogApi.class);

  @GET
  public Uni<List<Product>> listProducts() {
    logger.info("listProducts");
    return Product.all();
  }

  @POST
  public Uni<Product> createProduct(Product product) {
    logger.info("register");
    return product.persistAndFlush().replaceWith(product);
  }

  @GET
  @Path("/{id}")
  public Uni<Product> getProduct(Long id) {
    logger.info("createProduct");
    return Product.findById(id);
  }

  @GET
  @Path("/named/{name}")
  public Uni<Product> getProductByName(String name) {
    logger.info("getProductByName");
    return Product.findByName(name);
  }
}
