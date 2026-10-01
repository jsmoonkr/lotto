package com.example.lotto.collect;

import com.example.lotto.draw.LottoDraw;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 동행복권 결과 페이지가 내부적으로 호출하는 /lt645/selectPstLt645InfoNew.do 응답.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record DhLotteryResponse(Data data) {

    public List<Item> items() {
        return data == null || data.list() == null ? List.of() : data.list();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Data(List<Item> list) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Item(
            int ltEpsd,
            String ltRflYmd,
            int tm1WnNo, int tm2WnNo, int tm3WnNo, int tm4WnNo, int tm5WnNo, int tm6WnNo,
            int bnsWnNo,
            long rnk1WnNope, long rnk1WnAmt,
            long rnk2WnNope, long rnk2WnAmt,
            long rnk3WnNope, long rnk3WnAmt,
            long rnk4WnNope, long rnk4WnAmt,
            long rnk5WnNope, long rnk5WnAmt,
            long rlvtEpsdSumNtslAmt) {

        private static final DateTimeFormatter YMD = DateTimeFormatter.BASIC_ISO_DATE;

        public LottoDraw toEntity() {
            LottoDraw draw = new LottoDraw(
                    ltEpsd,
                    LocalDate.parse(ltRflYmd, YMD),
                    List.of(tm1WnNo, tm2WnNo, tm3WnNo, tm4WnNo, tm5WnNo, tm6WnNo),
                    bnsWnNo);
            draw.setPrizes(
                    new long[]{rnk1WnNope, rnk2WnNope, rnk3WnNope, rnk4WnNope, rnk5WnNope},
                    new long[]{rnk1WnAmt, rnk2WnAmt, rnk3WnAmt, rnk4WnAmt, rnk5WnAmt},
                    rlvtEpsdSumNtslAmt);
            return draw;
        }
    }
}
