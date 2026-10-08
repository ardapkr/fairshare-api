package dev.ardapeker.fairshare.group;

import dev.ardapeker.fairshare.error.InvalidRequestException;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** A group of people sharing costs, e.g. a flat or a trip. */
@Entity
@Table(name = "expense_groups")
public class ExpenseGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private String currency;

    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id")
    private List<Member> members = new ArrayList<>();

    protected ExpenseGroup() {
        // for JPA
    }

    public ExpenseGroup(String name, String currency) {
        this.name = name;
        this.currency = currency;
    }

    /** Adds a member; names are unique within a group (ignoring case). */
    public Member addMember(String memberName) {
        boolean taken = members.stream().anyMatch(m -> m.getName().equalsIgnoreCase(memberName));
        if (taken) {
            throw new InvalidRequestException(memberName + " is already in this group");
        }
        Member member = new Member(this, memberName);
        members.add(member);
        return member;
    }

    public Optional<Member> findMember(long memberId) {
        return members.stream().filter(m -> Long.valueOf(memberId).equals(m.getId())).findFirst();
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getCurrency() {
        return currency;
    }

    public List<Member> getMembers() {
        return List.copyOf(members);
    }
}
