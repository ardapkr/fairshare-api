package dev.ardapeker.fairshare.group;

import dev.ardapeker.fairshare.error.InvalidRequestException;
import dev.ardapeker.fairshare.error.NotFoundException;
import dev.ardapeker.fairshare.group.Dtos.AddMemberRequest;
import dev.ardapeker.fairshare.group.Dtos.BalanceResponse;
import dev.ardapeker.fairshare.group.Dtos.CreateExpenseRequest;
import dev.ardapeker.fairshare.group.Dtos.CreateGroupRequest;
import dev.ardapeker.fairshare.group.Dtos.ExpenseResponse;
import dev.ardapeker.fairshare.group.Dtos.GroupResponse;
import dev.ardapeker.fairshare.group.Dtos.MemberResponse;
import dev.ardapeker.fairshare.group.Dtos.TransferResponse;
import dev.ardapeker.fairshare.settlement.Money;
import dev.ardapeker.fairshare.settlement.SettlementCalculator;
import dev.ardapeker.fairshare.settlement.Share;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** The use cases of the API. Controllers stay thin; the maths lives in {@link SettlementCalculator}. */
@Service
@Transactional
public class GroupService {

    private static final String DEFAULT_CURRENCY = "EUR";

    private final ExpenseGroupRepository groups;
    private final ExpenseRepository expenses;

    public GroupService(ExpenseGroupRepository groups, ExpenseRepository expenses) {
        this.groups = groups;
        this.expenses = expenses;
    }

    public GroupResponse createGroup(CreateGroupRequest request) {
        String currency = request.currency() == null ? DEFAULT_CURRENCY : request.currency();
        ExpenseGroup group = new ExpenseGroup(request.name().strip(), currency);
        request.members().forEach(name -> group.addMember(name.strip()));
        return toResponse(groups.save(group));
    }

    @Transactional(readOnly = true)
    public GroupResponse getGroup(long groupId) {
        return toResponse(findGroup(groupId));
    }

    public MemberResponse addMember(long groupId, AddMemberRequest request) {
        ExpenseGroup group = findGroup(groupId);
        Member member = group.addMember(request.name().strip());
        groups.flush(); // assigns the new member's id
        return new MemberResponse(member.getId(), member.getName());
    }

    public ExpenseResponse addExpense(long groupId, CreateExpenseRequest request) {
        ExpenseGroup group = findGroup(groupId);
        Member payer = memberOf(group, request.paidBy());

        Set<Member> participants = new LinkedHashSet<>();
        if (request.participants() == null || request.participants().isEmpty()) {
            participants.addAll(group.getMembers());
        } else {
            request.participants().forEach(id -> participants.add(memberOf(group, id)));
        }

        Expense expense = new Expense(group, request.description().strip(), Money.toCents(request.amount()),
                payer, participants);
        return toResponse(expenses.save(expense));
    }

    @Transactional(readOnly = true)
    public List<ExpenseResponse> listExpenses(long groupId) {
        findGroup(groupId);
        return expensesOf(groupId).stream().map(GroupService::toResponse).toList();
    }

    public void deleteExpense(long groupId, long expenseId) {
        Expense expense = expenses.findByIdAndGroupId(expenseId, groupId)
                .orElseThrow(() -> new NotFoundException("Expense " + expenseId + " not found in group " + groupId));
        expenses.delete(expense);
    }

    @Transactional(readOnly = true)
    public List<BalanceResponse> balances(long groupId) {
        ExpenseGroup group = findGroup(groupId);
        Map<Long, String> names = namesById(group);
        return calculateBalances(group).entrySet().stream()
                .map(e -> new BalanceResponse(e.getKey(), names.get(e.getKey()), Money.fromCents(e.getValue())))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TransferResponse> settlements(long groupId) {
        ExpenseGroup group = findGroup(groupId);
        Map<Long, String> names = namesById(group);
        return SettlementCalculator.settle(calculateBalances(group)).stream()
                .map(t -> new TransferResponse(t.fromId(), names.get(t.fromId()), t.toId(), names.get(t.toId()),
                        Money.fromCents(t.amountCents())))
                .toList();
    }

    private Map<Long, Long> calculateBalances(ExpenseGroup group) {
        List<Share> shares = expensesOf(group.getId()).stream().map(Expense::toShare).toList();
        List<Long> memberIds = group.getMembers().stream().map(Member::getId).toList();
        return SettlementCalculator.balances(memberIds, shares);
    }

    private List<Expense> expensesOf(long groupId) {
        return expenses.findByGroupIdOrderByCreatedAtAscIdAsc(groupId);
    }

    private ExpenseGroup findGroup(long groupId) {
        return groups.findById(groupId).orElseThrow(() -> new NotFoundException("Group " + groupId + " not found"));
    }

    private static Member memberOf(ExpenseGroup group, long memberId) {
        return group.findMember(memberId)
                .orElseThrow(() -> new InvalidRequestException("Member " + memberId + " is not in this group"));
    }

    private static Map<Long, String> namesById(ExpenseGroup group) {
        return group.getMembers().stream().collect(Collectors.toMap(Member::getId, Member::getName));
    }

    private static GroupResponse toResponse(ExpenseGroup group) {
        List<MemberResponse> members = group.getMembers().stream()
                .map(m -> new MemberResponse(m.getId(), m.getName()))
                .toList();
        return new GroupResponse(group.getId(), group.getName(), group.getCurrency(), members);
    }

    private static ExpenseResponse toResponse(Expense e) {
        return new ExpenseResponse(e.getId(), e.getDescription(), Money.fromCents(e.getAmountCents()),
                e.getPaidBy().getId(), e.participantIds(), e.getCreatedAt());
    }
}
