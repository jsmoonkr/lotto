package com.example.lotto.recommend;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "recommendation", indexes = {@Index(columnList = "targetDrawNo"), @Index(columnList = "userId")})
public class Recommendation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Strategy strategy;

    /** 이 추천이 겨냥한 회차 (추천 시점의 최신 회차 + 1) */
    private int targetDrawNo;

    /** 추천받은 사용자. 기록은 본인 것만 보인다. */
    @Column(nullable = false)
    private Long userId;

    private int n1;
    private int n2;
    private int n3;
    private int n4;
    private int n5;
    private int n6;

    protected Recommendation() {
    }

    public Recommendation(Long userId, Strategy strategy, int targetDrawNo, List<Integer> numbers) {
        this.userId = userId;
        this.createdAt = LocalDateTime.now();
        this.strategy = strategy;
        this.targetDrawNo = targetDrawNo;
        this.n1 = numbers.get(0);
        this.n2 = numbers.get(1);
        this.n3 = numbers.get(2);
        this.n4 = numbers.get(3);
        this.n5 = numbers.get(4);
        this.n6 = numbers.get(5);
    }

    public List<Integer> numbers() {
        return List.of(n1, n2, n3, n4, n5, n6);
    }

    public Long getId() {
        return id;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public Strategy getStrategy() {
        return strategy;
    }

    public int getTargetDrawNo() {
        return targetDrawNo;
    }
}
