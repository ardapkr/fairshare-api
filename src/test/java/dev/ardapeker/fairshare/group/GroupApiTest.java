package dev.ardapeker.fairshare.group;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

/** Calls the real API (controller → service → H2 database) through MockMvc. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional // every test starts with an empty database
class GroupApiTest {

    @Autowired
    private MockMvc mvc;

    private long groupId;
    private long anna;
    private long ben;
    private long cem;

    @BeforeEach
    void createFlat() throws Exception {
        String body = postJson("/api/groups", """
                {"name": "Flat 3B", "members": ["Anna", "Ben", "Cem"]}""")
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        groupId = ((Number) JsonPath.read(body, "$.id")).longValue();
        anna = ((Number) JsonPath.read(body, "$.members[0].id")).longValue();
        ben = ((Number) JsonPath.read(body, "$.members[1].id")).longValue();
        cem = ((Number) JsonPath.read(body, "$.members[2].id")).longValue();
    }

    @Test
    void createdGroupHasMembersAndDefaultCurrency() throws Exception {
        mvc.perform(get("/api/groups/{id}", groupId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Flat 3B"))
                .andExpect(jsonPath("$.currency").value("EUR"))
                .andExpect(jsonPath("$.members[*].name").value(org.hamcrest.Matchers.contains("Anna", "Ben", "Cem")));
    }

    @Test
    void expensesProduceBalancesAndSettlements() throws Exception {
        addExpense("Groceries", "60.00", anna, null);              // split 3 ways: 20.00 each
        addExpense("Pizza", "30.00", ben, "[%d, %d]".formatted(ben, cem)); // Ben and Cem only

        mvc.perform(get("/api/groups/{id}/balances", groupId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Anna"))
                .andExpect(jsonPath("$[0].balance").value(40.00))
                .andExpect(jsonPath("$[1].balance").value(-5.00))
                .andExpect(jsonPath("$[2].balance").value(-35.00));

        mvc.perform(get("/api/groups/{id}/settlements", groupId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].from").value("Cem"))
                .andExpect(jsonPath("$[0].to").value("Anna"))
                .andExpect(jsonPath("$[0].amount").value(35.00))
                .andExpect(jsonPath("$[1].from").value("Ben"))
                .andExpect(jsonPath("$[1].amount").value(5.00));
    }

    @Test
    void deletingAnExpenseUpdatesBalances() throws Exception {
        String body = addExpense("Taxi", "12.00", cem, null).andReturn().getResponse().getContentAsString();
        long expenseId = ((Number) JsonPath.read(body, "$.id")).longValue();

        mvc.perform(delete("/api/groups/{g}/expenses/{e}", groupId, expenseId)).andExpect(status().isNoContent());

        mvc.perform(get("/api/groups/{id}/expenses", groupId)).andExpect(jsonPath("$", hasSize(0)));
        mvc.perform(get("/api/groups/{id}/settlements", groupId)).andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void addedMemberCanTakePartInExpenses() throws Exception {
        String body = postJson("/api/groups/" + groupId + "/members", """
                {"name": "Dora"}""")
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long dora = ((Number) JsonPath.read(body, "$.id")).longValue();

        addExpense("Cinema", "20.00", dora, "[%d, %d]".formatted(dora, anna)).andExpect(status().isCreated());

        mvc.perform(get("/api/groups/{id}/settlements", groupId))
                .andExpect(jsonPath("$[0].from").value("Anna"))
                .andExpect(jsonPath("$[0].to").value("Dora"))
                .andExpect(jsonPath("$[0].amount").value(10.00));
    }

    @Test
    void duplicateMemberNameIsRejected() throws Exception {
        postJson("/api/groups/" + groupId + "/members", """
                {"name": "anna"}""")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("anna is already in this group"));
    }

    @Test
    void invalidExpenseListsTheBadFields() throws Exception {
        postJson("/api/groups/" + groupId + "/expenses", """
                {"description": "", "amount": -5, "paidBy": %d}""".formatted(anna))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.description").exists())
                .andExpect(jsonPath("$.errors.amount").exists());
    }

    @Test
    void amountWithMoreThanTwoDecimalsIsRejected() throws Exception {
        addExpense("Coffee", "3.999", anna, null).andExpect(status().isBadRequest());
    }

    @Test
    void payerMustBeInTheGroup() throws Exception {
        addExpense("Lunch", "10.00", 9999, null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Member 9999 is not in this group"));
    }

    @Test
    void unknownGroupIs404() throws Exception {
        mvc.perform(get("/api/groups/{id}", 9999))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Group 9999 not found"));
    }

    @Test
    void groupNeedsAtLeastOneMember() throws Exception {
        postJson("/api/groups", """
                {"name": "Empty", "members": []}""")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.members").exists());
    }

    private ResultActions addExpense(String description, String amount, long paidBy, String participants)
            throws Exception {
        String json = """
                {"description": "%s", "amount": %s, "paidBy": %d, "participants": %s}"""
                .formatted(description, amount, paidBy, participants == null ? "null" : participants);
        return postJson("/api/groups/" + groupId + "/expenses", json);
    }

    private ResultActions postJson(String url, String json) throws Exception {
        return mvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(json));
    }
}
