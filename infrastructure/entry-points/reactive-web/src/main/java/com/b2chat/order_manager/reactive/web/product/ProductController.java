package com.b2chat.order_manager.reactive.web.product;

import com.b2chat.order_manager.reactive.web.product.dto.ProductDto;
import com.b2chat.order_manager.reactive.web.product.dto.ProductResponseDto;
import com.b2chat.order_manager.reactive.web.product.mapper.ProductMapper;
import com.b2chat.order_manager.usecase.ProductsUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequiredArgsConstructor
@RequestMapping("/products")
public class ProductController {

    private final ProductsUseCase productsUseCase;

    @GetMapping
    public Flux<ProductResponseDto> getAllProducts(@RequestParam(name = "page", defaultValue = "0") int page) {
        return productsUseCase.getAllProductsUseCase(page)
                .map(ProductMapper.INSTANCE::toResponse);
    }

    @GetMapping("/{id}")
    public Mono<ResponseEntity<ProductResponseDto>> getProductById(@PathVariable("id") Long id) {
        return productsUseCase.getProductByIdUseCase(id)
                .map(ProductMapper.INSTANCE::toResponse)
                .map(ResponseEntity::ok);
    }

    @PostMapping
    public Mono<ResponseEntity<ProductResponseDto>> createProduct(@Valid @RequestBody ProductDto productDto) {
        return productsUseCase.createProductUseCase(ProductMapper.INSTANCE.toEntity(productDto))
                .map(ProductMapper.INSTANCE::toResponse)
                .map(response -> ResponseEntity.status(HttpStatus.CREATED).body(response));
    }

    @PutMapping("/{id}")
    public Mono<ResponseEntity<ProductResponseDto>> updateProduct(@PathVariable("id") Long id,
                                                                  @Valid @RequestBody ProductDto productDto) {
        return productsUseCase.updateProductUseCase(id, ProductMapper.INSTANCE.toEntity(productDto))
                .map(ProductMapper.INSTANCE::toResponse)
                .map(ResponseEntity::ok);
    }

    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<String>> deleteProduct(@PathVariable("id") Long id) {
        return productsUseCase.deleteProductUseCase(id)
                .map(ResponseEntity::ok);
    }
}
