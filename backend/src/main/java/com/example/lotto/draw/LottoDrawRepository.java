package com.example.lotto.draw;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LottoDrawRepository extends JpaRepository<LottoDraw, Integer> {

    Optional<LottoDraw> findTopByOrderByDrawNoDesc();

    List<LottoDraw> findByDrawNoGreaterThanEqualOrderByDrawNoAsc(int fromDrawNo);

    List<LottoDraw> findByDrawNoIn(List<Integer> drawNos);
}
