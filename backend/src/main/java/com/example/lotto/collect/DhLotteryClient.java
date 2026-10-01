package com.example.lotto.collect;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

/**
 * 동행복권 회차별 당첨번호 조회 클라이언트.
 * 공식 공개 API가 아니라 웹페이지가 쓰는 주소이므로 바뀌면 이 클래스만 고치면 된다.
 */
@Component
public class DhLotteryClient {

    private static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/140.0 Safari/537.36";

    private final RestClient restClient;

    public DhLotteryClient(@Value("${lotto.sync.base-url}") String baseUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.USER_AGENT, USER_AGENT)
                .defaultHeader("X-Requested-With", "XMLHttpRequest")
                .build();
    }

    /** srchLtEpsd 주변 약 10개 회차를 돌려준다. 없는 회차면 빈 목록. */
    public List<DhLotteryResponse.Item> fetchAround(int drawNo) {
        DhLotteryResponse response = restClient.get()
                .uri("/lt645/selectPstLt645InfoNew.do?srchLtEpsd={drawNo}", drawNo)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(DhLotteryResponse.class);
        return response == null ? List.of() : response.items();
    }
}
