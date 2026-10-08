package dev.ardapeker.fairshare.settlement;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.TreeMap;

/**
 * The two calculations behind the app. Pure Java, no Spring, so it is easy to unit test.
 *
 * <ol>
 *   <li>{@link #balances}: how much each member is up (+) or down (−) after all expenses.</li>
 *   <li>{@link #settle}: a short list of payments that brings every balance back to zero.</li>
 * </ol>
 */
public final class SettlementCalculator {

    private SettlementCalculator() {
    }

    /**
     * Net balance per member in cents. Positive: the group owes them money. Negative: they owe the group.
     * The balances always add up to exactly 0.
     *
     * <p>Splitting is equal. If the amount does not divide evenly (10.00 between 3), the leftover cents go
     * one each to the participants with the lowest ids, so the result is the same every time.
     *
     * @param memberIds every member of the group, so members without expenses still get a 0 balance
     */
    public static Map<Long, Long> balances(Collection<Long> memberIds, List<Share> shares) {
        Map<Long, Long> balance = new TreeMap<>();
        memberIds.forEach(id -> balance.put(id, 0L));

        for (Share share : shares) {
            List<Long> participants = share.participantIds().stream().distinct().sorted().toList();
            long each = share.amountCents() / participants.size();
            long leftover = share.amountCents() % participants.size();

            balance.merge(share.payerId(), share.amountCents(), Long::sum);
            for (int i = 0; i < participants.size(); i++) {
                long owed = each + (i < leftover ? 1 : 0);
                balance.merge(participants.get(i), -owed, Long::sum);
            }
        }
        return balance;
    }

    /**
     * Payments that settle all balances.
     *
     * <p>Greedy: repeatedly let the member who owes the most pay the member who is owed the most. Each payment
     * brings at least one of them to zero, so with n members there are at most n − 1 payments. Finding the
     * true minimum number of payments is NP-hard (it contains the subset-sum problem), so this simple rule is
     * the usual practical choice.
     */
    public static List<Transfer> settle(Map<Long, Long> balances) {
        // Largest amount first; ties broken by id so the output is the same every time.
        Comparator<Open> largestFirst = Comparator.comparingLong((Open o) -> -o.amount).thenComparingLong(o -> o.memberId);
        PriorityQueue<Open> creditors = new PriorityQueue<>(largestFirst); // owed money
        PriorityQueue<Open> debtors = new PriorityQueue<>(largestFirst);   // owe money

        balances.forEach((id, cents) -> {
            if (cents > 0) {
                creditors.add(new Open(id, cents));
            } else if (cents < 0) {
                debtors.add(new Open(id, -cents));
            }
        });

        List<Transfer> transfers = new ArrayList<>();
        while (!creditors.isEmpty() && !debtors.isEmpty()) {
            Open creditor = creditors.poll();
            Open debtor = debtors.poll();
            long amount = Math.min(creditor.amount, debtor.amount);
            transfers.add(new Transfer(debtor.memberId, creditor.memberId, amount));

            creditor.amount -= amount;
            debtor.amount -= amount;
            if (creditor.amount > 0) {
                creditors.add(creditor);
            }
            if (debtor.amount > 0) {
                debtors.add(debtor);
            }
        }
        return transfers;
    }

    /** A member's amount still open during {@link #settle} (always positive). */
    private static final class Open {
        final long memberId;
        long amount;

        Open(long memberId, long amount) {
            this.memberId = memberId;
            this.amount = amount;
        }
    }
}
