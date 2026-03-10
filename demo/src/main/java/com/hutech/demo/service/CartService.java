package com.hutech.demo.service;

import com.hutech.demo.model.Cart;
import com.hutech.demo.model.CartItem;
import com.hutech.demo.model.CartStatus;
import com.hutech.demo.model.Product;
import com.hutech.demo.model.User;
import com.hutech.demo.repository.CartItemRepository;
import com.hutech.demo.repository.CartRepository;
import com.hutech.demo.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@Transactional
public class CartService {

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private ProductRepository productRepository;

    /**
     * Get or create active cart for user
     */
    public Cart getActiveCart(User user) {
        Optional<Cart> existingCart = cartRepository.findByUserIdAndStatus(user.getId(), CartStatus.ACTIVE);
        if (existingCart.isPresent()) {
            return existingCart.get();
        }

        Cart newCart = new Cart();
        newCart.setUser(user);
        newCart.setStatus(CartStatus.ACTIVE);
        newCart.setTotalAmount(BigDecimal.ZERO);
        newCart.setCreatedDate(LocalDateTime.now());
        newCart.setUpdatedDate(LocalDateTime.now());
        return cartRepository.save(newCart);
    }

    /**
     * Add product to cart or update quantity if already exists
     */
    public Cart addToCart(User user, Long productId, Integer quantity) {
        if (quantity <= 0 || quantity > 999) {
            throw new RuntimeException("Invalid quantity. Must be between 1 and 999.");
        }

        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new RuntimeException("Product not found"));

        if (product.getStock() <= 0) {
            throw new RuntimeException("Product out of stock");
        }

        Cart cart = getActiveCart(user);

        Optional<CartItem> existingItem = cartItemRepository.findByCartIdAndProductId(cart.getId(), productId);

        // calculate effective unit price with discount
        int discount = product.getDiscountPercent() != null ? product.getDiscountPercent() : 0;
        BigDecimal basePrice = BigDecimal.valueOf(product.getPrice());
        BigDecimal effectivePrice = basePrice
                .multiply(BigDecimal.valueOf(100 - discount))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        if (existingItem.isPresent()) {
            CartItem item = existingItem.get();
            item.setQuantity(item.getQuantity() + quantity);
            // keep existing unitPrice (already discounted when first added)
            cartItemRepository.save(item);
        } else {
            CartItem newItem = new CartItem();
            newItem.setCart(cart);
            newItem.setProduct(product);
            newItem.setQuantity(quantity);
            newItem.setUnitPrice(effectivePrice);
            newItem.setAddedDate(LocalDateTime.now());
            cartItemRepository.save(newItem);
            cart.getItems().add(newItem);
        }

        cart.calculateTotal();
        cart.setUpdatedDate(LocalDateTime.now());
        return cartRepository.save(cart);
    }

    /**
     * Remove item from cart
     */
    public Cart removeFromCart(Long cartItemId) {
        CartItem item = cartItemRepository.findById(cartItemId)
            .orElseThrow(() -> new RuntimeException("Cart item not found"));

        Cart cart = item.getCart();
        cart.getItems().remove(item);
        cartItemRepository.delete(item);

        cart.calculateTotal();
        cart.setUpdatedDate(LocalDateTime.now());
        return cartRepository.save(cart);
    }

    /**
     * Update quantity of cart item
     */
    public Cart updateItemQuantity(Long cartItemId, Integer newQuantity) {
        if (newQuantity <= 0 || newQuantity > 999) {
            throw new RuntimeException("Invalid quantity. Must be between 1 and 999.");
        }

        CartItem item = cartItemRepository.findById(cartItemId)
            .orElseThrow(() -> new RuntimeException("Cart item not found"));

        if (item.getProduct().getStock() < newQuantity) {
            throw new RuntimeException("Not enough stock available");
        }

        item.setQuantity(newQuantity);
        cartItemRepository.save(item);

        Cart cart = item.getCart();
        cart.calculateTotal();
        cart.setUpdatedDate(LocalDateTime.now());
        return cartRepository.save(cart);
    }

    /**
     * Clear all items from cart
     */
    public void clearCart(Long cartId) {
        Cart cart = cartRepository.findById(cartId)
            .orElseThrow(() -> new RuntimeException("Cart not found"));

        cart.getItems().clear();
        cart.setTotalAmount(BigDecimal.ZERO);
        cart.setUpdatedDate(LocalDateTime.now());
        cartRepository.save(cart);
    }

    /**
     * Get cart by ID
     */
    public Cart getCart(Long cartId) {
        return cartRepository.findById(cartId)
            .orElseThrow(() -> new RuntimeException("Cart not found"));
    }
}