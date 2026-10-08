package dev.ardapeker.fairshare.settlement;

/** "Member {@code fromId} pays member {@code toId} {@code amountCents}." */
public record Transfer(long fromId, long toId, long amountCents) {
}
