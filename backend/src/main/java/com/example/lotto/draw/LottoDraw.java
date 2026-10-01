package com.example.lotto.draw;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "lotto_draw")
public class LottoDraw {

    @Id
    private Integer drawNo;

    @Column(nullable = false)
    private LocalDate drawDate;

    private int n1;
    private int n2;
    private int n3;
    private int n4;
    private int n5;
    private int n6;
    private int bonus;

    // 등수별 당첨자 수와 1인당 당첨금
    private long firstWinners;
    private long firstPrize;
    private long secondWinners;
    private long secondPrize;
    private long thirdWinners;
    private long thirdPrize;
    private long fourthWinners;
    private long fourthPrize;
    private long fifthWinners;
    private long fifthPrize;

    private long totalSales;

    protected LottoDraw() {
    }

    public LottoDraw(Integer drawNo, LocalDate drawDate, List<Integer> numbers, int bonus) {
        if (numbers.size() != 6) {
            throw new IllegalArgumentException("당첨번호는 6개여야 합니다: " + numbers);
        }
        List<Integer> sorted = numbers.stream().sorted().toList();
        this.drawNo = drawNo;
        this.drawDate = drawDate;
        this.n1 = sorted.get(0);
        this.n2 = sorted.get(1);
        this.n3 = sorted.get(2);
        this.n4 = sorted.get(3);
        this.n5 = sorted.get(4);
        this.n6 = sorted.get(5);
        this.bonus = bonus;
    }

    public void setPrizes(long[] winners, long[] prizes, long totalSales) {
        this.firstWinners = winners[0];
        this.firstPrize = prizes[0];
        this.secondWinners = winners[1];
        this.secondPrize = prizes[1];
        this.thirdWinners = winners[2];
        this.thirdPrize = prizes[2];
        this.fourthWinners = winners[3];
        this.fourthPrize = prizes[3];
        this.fifthWinners = winners[4];
        this.fifthPrize = prizes[4];
        this.totalSales = totalSales;
    }

    public List<Integer> numbers() {
        return List.of(n1, n2, n3, n4, n5, n6);
    }

    public Integer getDrawNo() {
        return drawNo;
    }

    public LocalDate getDrawDate() {
        return drawDate;
    }

    public int getBonus() {
        return bonus;
    }

    public long getFirstWinners() {
        return firstWinners;
    }

    public long getFirstPrize() {
        return firstPrize;
    }

    public long getSecondWinners() {
        return secondWinners;
    }

    public long getSecondPrize() {
        return secondPrize;
    }

    public long getThirdWinners() {
        return thirdWinners;
    }

    public long getThirdPrize() {
        return thirdPrize;
    }

    public long getFourthWinners() {
        return fourthWinners;
    }

    public long getFourthPrize() {
        return fourthPrize;
    }

    public long getFifthWinners() {
        return fifthWinners;
    }

    public long getFifthPrize() {
        return fifthPrize;
    }

    public long getTotalSales() {
        return totalSales;
    }
}
