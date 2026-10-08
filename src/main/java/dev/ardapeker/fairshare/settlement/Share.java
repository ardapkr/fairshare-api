package dev.ardapeker.fairshare.settlement;

import java.util.List;

/**
 * One expense as the calculator sees it: who paid, how much, and who it is split between.
 * Uses plain member ids so the calculation does not depend on the database.
 */
public record Share(long payerId, long amountCents, List<Long> participantIds) {

    public Share {
        if (amountCents <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        if (participantIds.isEmpty()) {
            throw new IllegalArgumentException("An expense needs at least one participant");
        }
        participantIds = List.copyOf(participantIds);
    }
}
