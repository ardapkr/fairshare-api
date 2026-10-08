package dev.ardapeker.fairshare.group;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** Request and response bodies of the REST API. Amounts are decimals like 12.50. */
public final class Dtos {

    private Dtos() {
    }

    public record CreateGroupRequest(
            @NotBlank @Size(max = 100) String name,
            @Pattern(regexp = "[A-Z]{3}", message = "must be a 3-letter currency code like EUR") String currency,
            @NotEmpty List<@NotBlank @Size(max = 50) String> members) {
    }

    public record AddMemberRequest(@NotBlank @Size(max = 50) String name) {
    }

    /** {@code participants} empty or missing means: split between everyone in the group. */
    public record CreateExpenseRequest(
            @NotBlank @Size(max = 200) String description,
            @NotNull @Positive @Digits(integer = 9, fraction = 2) BigDecimal amount,
            @NotNull Long paidBy,
            List<Long> participants) {
    }

    public record MemberResponse(long id, String name) {
    }

    public record GroupResponse(long id, String name, String currency, List<MemberResponse> members) {
    }

    public record ExpenseResponse(
            long id, String description, BigDecimal amount, long paidBy, List<Long> participants, Instant createdAt) {
    }

    /** Positive balance: the group owes this member. Negative: this member owes the group. */
    public record BalanceResponse(long memberId, String name, BigDecimal balance) {
    }

    public record TransferResponse(long fromMemberId, String from, long toMemberId, String to, BigDecimal amount) {
    }
}
