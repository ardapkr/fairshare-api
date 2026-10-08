package dev.ardapeker.fairshare.group;

import dev.ardapeker.fairshare.group.Dtos.AddMemberRequest;
import dev.ardapeker.fairshare.group.Dtos.BalanceResponse;
import dev.ardapeker.fairshare.group.Dtos.CreateExpenseRequest;
import dev.ardapeker.fairshare.group.Dtos.CreateGroupRequest;
import dev.ardapeker.fairshare.group.Dtos.ExpenseResponse;
import dev.ardapeker.fairshare.group.Dtos.GroupResponse;
import dev.ardapeker.fairshare.group.Dtos.MemberResponse;
import dev.ardapeker.fairshare.group.Dtos.TransferResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Groups", description = "Groups, members, expenses and settling up")
@RequestMapping("/api/groups")
public class GroupController {

    private final GroupService service;

    public GroupController(GroupService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    GroupResponse createGroup(@Valid @RequestBody CreateGroupRequest request) {
        return service.createGroup(request);
    }

    @GetMapping("/{groupId}")
    GroupResponse getGroup(@PathVariable long groupId) {
        return service.getGroup(groupId);
    }

    @PostMapping("/{groupId}/members")
    @ResponseStatus(HttpStatus.CREATED)
    MemberResponse addMember(@PathVariable long groupId, @Valid @RequestBody AddMemberRequest request) {
        return service.addMember(groupId, request);
    }

    @PostMapping("/{groupId}/expenses")
    @ResponseStatus(HttpStatus.CREATED)
    ExpenseResponse addExpense(@PathVariable long groupId, @Valid @RequestBody CreateExpenseRequest request) {
        return service.addExpense(groupId, request);
    }

    @GetMapping("/{groupId}/expenses")
    List<ExpenseResponse> listExpenses(@PathVariable long groupId) {
        return service.listExpenses(groupId);
    }

    @DeleteMapping("/{groupId}/expenses/{expenseId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deleteExpense(@PathVariable long groupId, @PathVariable long expenseId) {
        service.deleteExpense(groupId, expenseId);
    }

    @GetMapping("/{groupId}/balances")
    List<BalanceResponse> balances(@PathVariable long groupId) {
        return service.balances(groupId);
    }

    /** Who should pay whom so that everyone is even. */
    @GetMapping("/{groupId}/settlements")
    List<TransferResponse> settlements(@PathVariable long groupId) {
        return service.settlements(groupId);
    }
}
