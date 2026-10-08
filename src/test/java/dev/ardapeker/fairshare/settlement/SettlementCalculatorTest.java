package dev.ardapeker.fairshare.settlement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

class SettlementCalculatorTest {

    private static final long ANNA = 1;
    private static final long BEN = 2;
    private static final long CEM = 3;
    private static final List<Long> EVERYONE = List.of(ANNA, BEN, CEM);

    @Nested
    class Balances {

        @Test
        void equalSplitBetweenEveryone() {
            // Anna pays 30.00 for all three: she is owed 20.00, the others owe 10.00 each.
            var balances = SettlementCalculator.balances(EVERYONE, List.of(new Share(ANNA, 3000, EVERYONE)));

            assertThat(balances).isEqualTo(Map.of(ANNA, 2000L, BEN, -1000L, CEM, -1000L));
        }

        @Test
        void leftoverCentsGoToTheLowestIds() {
            // 10.00 / 3 = 3.33 r 0.01 → Anna (lowest id) owes 3.34, Ben and Cem 3.33.
            var balances = SettlementCalculator.balances(EVERYONE, List.of(new Share(BEN, 1000, EVERYONE)));

            assertThat(balances.get(ANNA)).isEqualTo(-334);
            assertThat(balances.get(BEN)).isEqualTo(1000 - 333);
            assertThat(balances.get(CEM)).isEqualTo(-333);
            assertThat(sum(balances)).isZero();
        }

        @Test
        void payerDoesNotHaveToTakePart() {
            // Cem buys a 20.00 gift for Anna and Ben's flat: they owe him 10.00 each.
            var balances = SettlementCalculator.balances(EVERYONE, List.of(new Share(CEM, 2000, List.of(ANNA, BEN))));

            assertThat(balances).isEqualTo(Map.of(ANNA, -1000L, BEN, -1000L, CEM, 2000L));
        }

        @Test
        void membersWithoutExpensesHaveZeroBalance() {
            var balances = SettlementCalculator.balances(EVERYONE, List.of());

            assertThat(balances).isEqualTo(Map.of(ANNA, 0L, BEN, 0L, CEM, 0L));
        }

        @Test
        void duplicateParticipantsAreCountedOnce() {
            var balances = SettlementCalculator.balances(EVERYONE, List.of(new Share(ANNA, 1000, List.of(BEN, BEN))));

            assertThat(balances.get(BEN)).isEqualTo(-1000);
        }

        @Test
        void sharesMustBePositiveAndHaveParticipants() {
            assertThatThrownBy(() -> new Share(ANNA, 0, EVERYONE)).isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> new Share(ANNA, 100, List.of())).isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    class Settle {

        @Test
        void oneDebtorPaysOneCreditor() {
            var transfers = SettlementCalculator.settle(Map.of(ANNA, 500L, BEN, -500L));

            assertThat(transfers).containsExactly(new Transfer(BEN, ANNA, 500));
        }

        @Test
        void largestDebtIsPaidToLargestCreditorFirst() {
            var transfers = SettlementCalculator.settle(Map.of(ANNA, 2000L, BEN, -1500L, CEM, -500L));

            assertThat(transfers).containsExactly(new Transfer(BEN, ANNA, 1500), new Transfer(CEM, ANNA, 500));
        }

        @Test
        void evenGroupNeedsNoPayments() {
            assertThat(SettlementCalculator.settle(Map.of(ANNA, 0L, BEN, 0L))).isEmpty();
        }

        @Test
        void chainOfDebtsIsShortened() {
            // Ben owes Anna 10, Cem owes Ben 10: one payment (Cem → Anna) is enough, not two.
            var balances = SettlementCalculator.balances(EVERYONE, List.of(
                    new Share(ANNA, 1000, List.of(BEN)),
                    new Share(BEN, 1000, List.of(CEM))));

            assertThat(SettlementCalculator.settle(balances)).containsExactly(new Transfer(CEM, ANNA, 1000));
        }

        /** Random groups: the payments always settle everyone exactly, with at most n − 1 payments. */
        @RepeatedTest(50)
        void alwaysSettlesEveryoneWithAtMostNMinusOnePayments() {
            Random random = new Random();
            int memberCount = 2 + random.nextInt(8);
            int expenseCount = random.nextInt(20);
            List<Long> members = new ArrayList<>();
            for (long id = 1; id <= memberCount; id++) {
                members.add(id);
            }
            List<Share> shares = new ArrayList<>();
            for (int i = 0; i < expenseCount; i++) {
                List<Long> participants = members.stream().filter(m -> random.nextBoolean()).toList();
                if (!participants.isEmpty()) {
                    long payer = members.get(random.nextInt(members.size()));
                    shares.add(new Share(payer, 1 + random.nextInt(100_00), participants));
                }
            }

            Map<Long, Long> balances = SettlementCalculator.balances(members, shares);
            List<Transfer> transfers = SettlementCalculator.settle(balances);

            assertThat(sum(balances)).isZero();
            assertThat(transfers.size()).isLessThanOrEqualTo(members.size() - 1);
            assertThat(afterPaying(balances, transfers).values()).allMatch(b -> b == 0);
            assertThat(transfers).allMatch(t -> t.amountCents() > 0 && t.fromId() != t.toId());
        }
    }

    private static long sum(Map<Long, Long> balances) {
        return balances.values().stream().mapToLong(Long::longValue).sum();
    }

    private static Map<Long, Long> afterPaying(Map<Long, Long> balances, List<Transfer> transfers) {
        Map<Long, Long> result = new HashMap<>(balances);
        for (Transfer t : transfers) {
            result.merge(t.fromId(), t.amountCents(), Long::sum);
            result.merge(t.toId(), -t.amountCents(), Long::sum);
        }
        return result;
    }
}
