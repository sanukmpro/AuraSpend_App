package com.auraspend.api.expense;

import com.auraspend.api.auth.CurrentUser;
import com.auraspend.api.user.AppUser;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/expenses")
@CrossOrigin
public class ExpenseController {

    private final ExpenseRepository expenses;
    private final CurrentUser current;

    public ExpenseController(ExpenseRepository e, CurrentUser c) {
        this.expenses = e;
        this.current = c;
    }

    public record ExpensePayload(Long id, double amount, String date, String category, String description) {
    }

    private ExpensePayload dto(Expense e) {
        return new ExpensePayload(e.id, e.amount, e.date, e.category, e.description);
    }

    @GetMapping
    public List<ExpensePayload> list(@RequestHeader(value = "Authorization", required = false) String auth) {
        AppUser u = current.require(auth);
        return expenses.findAllByUserIdOrderByDateDesc(u.id).stream()
                .map(this::dto)
                .toList();
    }

    @PostMapping
    public ResponseEntity<?> add(@RequestHeader(value = "Authorization", required = false) String auth,
                                 @RequestBody ExpensePayload p) {
        AppUser u = current.require(auth);
        String error = validate(p);
        if (error != null) {
            return ResponseEntity.badRequest().body(Map.of("message", error));
        }

        Expense e = new Expense();
        e.user = u;
        copy(e, p);
        return ResponseEntity.ok(dto(expenses.save(e)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@RequestHeader(value = "Authorization", required = false) String auth,
                                    @PathVariable Long id,
                                    @RequestBody ExpensePayload p) {
        AppUser u = current.require(auth);
        String error = validate(p);
        if (error != null) {
            return ResponseEntity.badRequest().body(Map.of("message", error));
        }

        Expense e = expenses.findByIdAndUserId(id, u.id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Expense not found"));
        copy(e, p);
        return ResponseEntity.ok(dto(expenses.save(e)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@RequestHeader(value = "Authorization", required = false) String auth,
                                    @PathVariable Long id) {
        AppUser u = current.require(auth);
        Expense e = expenses.findByIdAndUserId(id, u.id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Expense not found"));
        expenses.delete(e);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/import")
    public ResponseEntity<?> importExpenses(@RequestHeader(value = "Authorization", required = false) String auth,
                                           @RequestBody Map<String, List<ExpensePayload>> body) {
        AppUser u = current.require(auth);
        List<ExpensePayload> payload = body.get("expenses");
        if (payload == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Missing expenses array"));
        }

        List<ExpensePayload> result = new ArrayList<>();
        for (ExpensePayload p : payload) {
            String error = validate(p);
            if (error != null) {
                return ResponseEntity.badRequest().body(Map.of("message", error));
            }
            Expense e = new Expense();
            e.user = u;
            copy(e, p);
            result.add(dto(expenses.save(e)));
        }
        return ResponseEntity.ok(result);
    }

    private void copy(Expense e, ExpensePayload p) {
        e.amount = p.amount();
        e.date = p.date();
        e.category = p.category();
        e.description = p.description() == null ? "" : p.description().trim();
    }

    private String validate(ExpensePayload p) {
        if (p == null || !Double.isFinite(p.amount()) || p.amount() <= 0) {
            return "Amount must be greater than zero.";
        }
        try {
            LocalDate.parse(p.date());
        } catch (Exception e) {
            return "Date must be YYYY-MM-DD.";
        }
        if (p.category() == null || !List.of("food", "transport", "rent", "fun", "utilities", "others").contains(p.category())) {
            return "Invalid category.";
        }
        return null;
    }
}
