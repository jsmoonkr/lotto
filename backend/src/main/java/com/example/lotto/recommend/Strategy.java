package com.example.lotto.recommend;

public enum Strategy {
    /** 아무 조건 없는 무작위 */
    RANDOM,
    /** 역대 출현 횟수에 비례한 가중치 */
    FREQUENCY,
    /** 마지막 출현 후 지난 회차 수에 비례한 가중치 */
    OVERDUE,
    /** 무작위 + 홀짝, 합계, 연속번호 조건 */
    FILTERED
}
