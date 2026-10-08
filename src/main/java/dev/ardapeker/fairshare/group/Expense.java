package dev.ardapeker.fairshare.group;

import dev.ardapeker.fairshare.settlement.Share;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** One payment: {@code paidBy} paid {@code amountCents}, split equally between {@code participants}. */
@Entity
@Table(name = "expenses")
public class Expense {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private ExpenseGroup group;

    private String description;

    private long amountCents;

    @ManyToOne(optional = false)
    private Member paidBy;

    @ManyToMany
    @JoinTable(name = "expense_participants")
    private Set<Member> participants = new LinkedHashSet<>();

    private Instant createdAt;

    protected Expense() {
        // for JPA
    }

    public Expense(ExpenseGroup group, String description, long amountCents, Member paidBy, Set<Member> participants) {
        this.group = group;
        this.description = description;
        this.amountCents = amountCents;
        this.paidBy = paidBy;
        this.participants = new LinkedHashSet<>(participants);
        this.createdAt = Instant.now();
    }

    /** The calculator's view of this expense. */
    public Share toShare() {
        return new Share(paidBy.getId(), amountCents, participantIds());
    }

    public List<Long> participantIds() {
        return participants.stream().map(Member::getId).sorted().toList();
    }

    public Long getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    public long getAmountCents() {
        return amountCents;
    }

    public Member getPaidBy() {
        return paidBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
