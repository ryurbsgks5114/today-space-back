package com.complete.todayspace.domain.product.entity;

import com.complete.todayspace.domain.payment.entity.Payment;
import com.complete.todayspace.domain.product.dto.CreateProductRequestDto;
import com.complete.todayspace.domain.product.dto.EditProductRequestDto;
import com.complete.todayspace.domain.user.entity.User;
import com.complete.todayspace.domain.wish.entity.Wish;
import com.complete.todayspace.global.entity.AllTimestamp;
import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "table_product")
@NoArgsConstructor
@Getter
public class Product extends AllTimestamp {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long price;

    @Column(nullable = false, length = 60)
    private String title;

    @Column(length = 600)
    private String content;

    @Column
    @Enumerated(EnumType.STRING)
    private Address address;

    @Column
    @Enumerated(EnumType.STRING)
    private State state;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @OneToMany(mappedBy = "product", cascade = {CascadeType.PERSIST, CascadeType.REMOVE})
    private List<ImageProduct> imageProducts = new ArrayList<>();

    @OneToMany(mappedBy = "product", cascade = {CascadeType.PERSIST, CascadeType.REMOVE})
    private List<Wish> wish = new ArrayList<>();

    @OneToMany(mappedBy = "product", cascade = {CascadeType.PERSIST, CascadeType.REMOVE})
    private List<Payment> payment = new ArrayList<>();

    public Product(Long id, Long price, String title, String content, Address address, State state, User user){
        this.id = id;
        this.price = price;
        this.title = title;
        this.content = content;
        this.address = address;
        this.state = state;
        this.user = user;
    }

    public Product(CreateProductRequestDto requestDto, User user) {
        this.price = requestDto.getPrice();
        this.title = requestDto.getTitle();
        this.content = requestDto.getContent();
        this.address = requestDto.getAddress();
        this.state = requestDto.getState();
        this.user = user;
    }

    public void updateProduct(EditProductRequestDto requestDto) {
        this.price = requestDto.getPrice();
        this.title = requestDto.getTitle();
        this.content = requestDto.getContent();
        this.address = requestDto.getAddress();
        this.state = requestDto.getState();
    }
}
