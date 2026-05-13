//package com.z.finance.tracker.controller;
//
////import com.z.finance.tracker.client.OllamaClient;
//import com.z.finance.tracker.client.DeepseekClient;
//import com.z.finance.tracker.entity.Budget;
//import com.z.finance.tracker.entity.SavingsGoal;
//import com.z.finance.tracker.entity.User;
//import com.z.finance.tracker.mapper.BudgetMapper;
//import com.z.finance.tracker.mapper.GoalMapper;
//import com.z.finance.tracker.mapper.TransactionMapper;
//import com.z.finance.tracker.mapper.UserMapper;
//import com.z.finance.tracker.enums.TraType;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.http.ResponseEntity;
//import org.springframework.security.core.context.SecurityContextHolder;
//import org.springframework.web.bind.annotation.*;
//
//import java.util.*;
//
//@RestController
//@RequestMapping("/api/ai")
//public class AiController {
//
////    @Autowired private OllamaClient ollamaClient;
//    @Autowired private DeepseekClient deepseekClient;
//    @Autowired private TransactionMapper transactionMapper;
//    @Autowired private UserMapper userMapper;
//    @Autowired private BudgetMapper budgetMapper;
//    @Autowired private GoalMapper goalMapper;
//
//    /**
//     * POST /api/ai/chat
//     * Body: {
//     *   "messages": [ { "role": "user", "content": "..." } ]
//     * }
//     * The frontend sends the full conversation history each time.
//     * The backend prepends a system prompt with live financial data.
//     */
//    @PostMapping("/chat")
//    public ResponseEntity<?> chat(@RequestBody Map<String, Object> body) {
//        try {
//            Long userId = getCurrentUserId();
//
//            // ── Build financial context from real DB data ──────────
//            String financialContext = buildFinancialContext(userId);
//
//            // ── System prompt ──────────────────────────────────────
//            String systemPrompt = """
//                You are a helpful personal finance advisor with access to the user's \
//                real financial data. Be direct, specific, and actionable. \
//                Use the data below to give personalized advice. \
//                Keep responses concise (3-5 sentences max unless asked for more). \
//                When relevant, reference specific numbers from their data.
//
//                === USER'S FINANCIAL DATA ===
//                """ + financialContext;
//
//            // ── Prepend system message to conversation ─────────────
//            List<Map<String, String>> conversation = new ArrayList<>();
//            conversation.add(Map.of("role", "system", "content", systemPrompt));
//
//            // Append frontend messages
//            List<Map<String, String>> frontendMessages =
//                (List<Map<String, String>>) body.get("messages");
//            conversation.addAll(frontendMessages);
//
//            // ── Call Ollama ────────────────────────────────────────
//            String reply = deepseekClient.chat(conversation);
//
//            return ResponseEntity.ok(Map.of("reply", reply));
//
//        } catch (Exception e) {
//            return ResponseEntity.internalServerError()
//                .body(Map.of("error", "AI unavailable: " + e.getMessage()));
//        }
//    }
//
//    /**
//     * GET /api/ai/summary
//     * Returns an auto-generated financial summary with no user prompt.
//     * Used for the initial analysis card shown on the AI screen.
//     */
//    @GetMapping("/summary")
//    public ResponseEntity<?> getSummary() {
//        try {
//                Long userId = getCurrentUserId();
//            String financialContext = buildFinancialContext(userId);
//
//            String prompt = """
//                Based on this user's financial data, give them 3-4 bullet points \
//                of specific, actionable advice. Focus on their biggest opportunities \
//                to save money or improve their financial health. \
//                Be encouraging but honest. Start directly with the bullet points, \
//                no preamble.
//
//                === FINANCIAL DATA ===
//                """ + financialContext;
//
//            List<Map<String, String>> messages = List.of(
//                Map.of("role", "user", "content", prompt)
//            );
//
//            String reply = deepseekClient.chat(messages);
//            return ResponseEntity.ok(Map.of("summary", reply));
//
//        } catch (Exception e) {
//            return ResponseEntity.internalServerError()
//                .body(Map.of("error", "AI unavailable: " + e.getMessage()));
//        }
//    }
//
//    // ── Build context string from real user data ───────────────────
//    private String buildFinancialContext(Long userId) {
//        StringBuilder sb = new StringBuilder();
//
//        // ── 1. All-time cash flow ──────────────────────────────────────
//        var cashFlow = transactionMapper.getIncomeVsExpense(userId);
//        double totalIncome  = toDouble(cashFlow.get("totalIncome"));
//        double totalExpense = toDouble(cashFlow.get("totalExpense"));
//        double balance      = totalIncome - totalExpense;
//        double savingsRate  = totalIncome > 0
//                ? ((totalIncome - totalExpense) / totalIncome * 100) : 0;
//
//        sb.append("=== OVERALL FINANCES ===\n");
//        sb.append(String.format(
//                "Total Income: %.0f | Total Expenses: %.0f | Net Balance: %.0f | Savings Rate: %.1f%%\n\n",
//                totalIncome, totalExpense, balance, savingsRate
//        ));
//
//        // ── 2. Month-by-month breakdown (last 6 months) ────────────────
//        var monthly = transactionMapper.getMonthlyBreakdown(userId, 6);
//        sb.append("=== LAST 6 MONTHS (newest first) ===\n");
//
//        // Calculate month-over-month expense change
//        for (int i = 0; i < monthly.size(); i++) {
//            var m = monthly.get(i);
//            double inc = toDouble(m.get("income"));
//            double exp = toDouble(m.get("expense"));
//            double net = inc - exp;
//            long   txCount = toLong(m.get("transactionCount"));
//
//            String momChange = "";
//            if (i < monthly.size() - 1) {
//                double prevExp = toDouble(monthly.get(i + 1).get("expense"));
//                if (prevExp > 0) {
//                    double pct = ((exp - prevExp) / prevExp) * 100;
//                    momChange = String.format(" (%s%.0f%% vs prev month)",
//                            pct >= 0 ? "+" : "", pct);
//                }
//            }
//
//            sb.append(String.format(
//                    "  %s → Income: %.0f | Expense: %.0f%s | Net: %.0f | %d transactions\n",
//                    m.get("month"), inc, exp, momChange, net, txCount
//            ));
//        }
//
//        // ── 3. Expense category breakdown ─────────────────────────────
//        var expenseCategories = transactionMapper.getCategorySummary(userId, TraType.EXPENSE);
//        expenseCategories.sort((a, b) ->
//                Double.compare(toDouble(b.get("value")), toDouble(a.get("value"))));
//
//        sb.append("\n=== EXPENSE BREAKDOWN BY CATEGORY ===\n");
//        expenseCategories.stream().limit(8).forEach(cat -> {
//            double val = toDouble(cat.get("value"));
//            double pct = totalExpense > 0 ? (val / totalExpense * 100) : 0;
//            sb.append(String.format(
//                    "  %s: %.0f (%.1f%% of total spending)\n",
//                    cat.get("name"), val, pct
//            ));
//        });
//
//        // ── 4. Income sources ──────────────────────────────────────────
//        var incomeCategories = transactionMapper.getCategorySummary(userId, TraType.INCOME);
//        sb.append("\n=== INCOME SOURCES ===\n");
//        incomeCategories.forEach(cat -> sb.append(String.format(
//                "  %s: %.0f\n", cat.get("name"), toDouble(cat.get("value"))
//        )));
//
//        // ── 5. Biggest recent expenses ─────────────────────────────────
//        var biggest = transactionMapper.getBiggestTransactions(userId, 8);
//        sb.append("\n=== BIGGEST EXPENSES (last 3 months) ===\n");
//        biggest.forEach(tx -> sb.append(String.format(
//                "  %s | %s | %.0f | %s\n",
//                tx.get("date"),
//                tx.get("categoryName"),
//                toDouble(tx.get("amount")),
//                tx.get("text")
//        )));
//
//        // ── 6. Spending pattern insight ────────────────────────────────
//        if (monthly.size() >= 2) {
//            double latestExp = toDouble(monthly.get(0).get("expense"));
//            double prevExp   = toDouble(monthly.get(1).get("expense"));
//            double trend     = prevExp > 0 ? ((latestExp - prevExp) / prevExp * 100) : 0;
//
//            sb.append("\n=== SPENDING TREND ===\n");
//            sb.append(String.format(
//                    "  Most recent month expenses are %.1f%% %s than the previous month.\n",
//                    Math.abs(trend), trend >= 0 ? "higher" : "lower"
//            ));
//
//            // Flag if any single category dominates
//            if (!expenseCategories.isEmpty() && totalExpense > 0) {
//                double topCatPct = toDouble(expenseCategories.get(0).get("value")) / totalExpense * 100;
//                if (topCatPct > 40) {
//                    sb.append(String.format(
//                            "  WARNING: '%s' alone accounts for %.0f%% of all spending — highly concentrated.\n",
//                            expenseCategories.get(0).get("name"), topCatPct
//                    ));
//                }
//            }
//
//            // Flag negative savings rate
//            if (savingsRate < 0) {
//                sb.append(String.format(
//                        "  WARNING: User is spending %.0f more than they earn — negative savings rate.\n",
//                        Math.abs(balance)
//                ));
//            } else if (savingsRate < 10) {
//                sb.append("  NOTE: Savings rate is below 10% — financially fragile.\n");
//            } else if (savingsRate > 30) {
//                sb.append("  NOTE: Savings rate above 30% — strong financial discipline.\n");
//            }
//        }
//
//        // ── 7. Budgets vs actual spending ─────────────────────────────────
//        var budgets = budgetMapper.findByUserId(userId);
//        if (!budgets.isEmpty()) {
//            sb.append("\n=== MONTHLY BUDGETS vs ACTUAL SPENDING ===\n");
//
//            // Get this month's spending per category
//            var thisMonthData = transactionMapper.getMonthlyBreakdown(userId, 1);
//            // We need per-category spending for current month
//            var currentMonthExpenses = transactionMapper.getCategorySummary(userId, TraType.EXPENSE);
//            Map<String, Double> spentMap = new HashMap<>();
//            currentMonthExpenses.forEach(cat ->
//                    spentMap.put(
//                            (String) cat.get("name"),
//                            toDouble(cat.get("value"))
//                    )
//            );
//
//            boolean anyOverBudget = false;
//            for (var budget : budgets) {
//                double spent  = spentMap.getOrDefault(budget.getCategoryName(), 0.0);
//                double limit  = budget.getLimitAmount();
//                double pct    = limit > 0 ? (spent / limit * 100) : 0;
//                double remaining = limit - spent;
//                String status;
//
//                if (pct >= 100) {
//                    status = String.format("OVER BUDGET by %.0f", -remaining);
//                    anyOverBudget = true;
//                } else if (pct >= 80) {
//                    status = String.format("WARNING: %.0f%% used, %.0f remaining", pct, remaining);
//                } else {
//                    status = String.format("%.0f%% used, %.0f remaining", pct, remaining);
//                }
//
//                sb.append(String.format(
//                        "  %s: budget %.0f | spent %.0f | %s\n",
//                        budget.getCategoryName(), limit, spent, status
//                ));
//            }
//
//            if (anyOverBudget) {
//                sb.append("  ⚠ User has exceeded at least one budget this month.\n");
//            }
//
//            // Total budget utilization
//            double totalBudgeted = budgets.stream().mapToDouble(Budget::getLimitAmount).sum();
//            double totalBudgetSpent = budgets.stream()
//                    .mapToDouble(b -> spentMap.getOrDefault(b.getCategoryName(), 0.0))
//                    .sum();
//            sb.append(String.format(
//                    "  Overall budget utilization: %.0f of %.0f (%.1f%%)\n",
//                    totalBudgetSpent, totalBudgeted,
//                    totalBudgeted > 0 ? (totalBudgetSpent / totalBudgeted * 100) : 0
//            ));
//        }
//
//// ── 8. Savings goals progress ──────────────────────────────────────
//        var goals = goalMapper.findByUserId(userId);
//        if (!goals.isEmpty()) {
//            sb.append("\n=== SAVINGS GOALS ===\n");
//
//            for (var goal : goals) {
//                double pct = goal.getTargetAmount() > 0
//                        ? (goal.getCurrentAmount() / goal.getTargetAmount() * 100) : 0;
//                double remaining = goal.getTargetAmount() - goal.getCurrentAmount();
//                boolean isComplete = pct >= 100;
//
//                String deadlineInfo = "";
//                if (goal.getDeadline() != null) {
//                    long daysLeft = java.time.temporal.ChronoUnit.DAYS.between(
//                            java.time.LocalDate.now(), goal.getDeadline()
//                    );
//                    if (daysLeft < 0) {
//                        deadlineInfo = String.format(" | OVERDUE by %d days", -daysLeft);
//                    } else if (daysLeft == 0) {
//                        deadlineInfo = " | Due TODAY";
//                    } else {
//                        // Calculate required daily saving rate
//                        double dailyNeeded = daysLeft > 0 ? remaining / daysLeft : remaining;
//                        deadlineInfo = String.format(
//                                " | %d days left | need %.0f/day to hit target",
//                                daysLeft, dailyNeeded
//                        );
//                    }
//                }
//
//                sb.append(String.format(
//                        "  %s '%s': %.0f of %.0f saved (%.0f%%)%s%s\n",
//                        goal.getIcon(),
//                        goal.getName(),
//                        goal.getCurrentAmount(),
//                        goal.getTargetAmount(),
//                        pct,
//                        isComplete ? " ✓ COMPLETED" : String.format(" | %.0f still needed", remaining),
//                        deadlineInfo
//                ));
//            }
//
//            // Summary stats
//            long completedCount = goals.stream()
//                    .filter(g -> g.getCurrentAmount() >= g.getTargetAmount()).count();
//            double totalTargets = goals.stream().mapToDouble(SavingsGoal::getTargetAmount).sum();
//            double totalSaved   = goals.stream().mapToDouble(SavingsGoal::getCurrentAmount).sum();
//
//            sb.append(String.format(
//                    "  Summary: %d of %d goals completed | %.0f of %.0f total saved across all goals\n",
//                    completedCount, goals.size(), totalSaved, totalTargets
//            ));
//        }
//
//        return sb.toString();
//    }
//
//    private long toLong(Object val) {
//        if (val == null) return 0L;
//        if (val instanceof Number) return ((Number) val).longValue();
//        try { return Long.parseLong(val.toString()); } catch (Exception e) { return 0L; }
//    }
//
//    private double toDouble(Object val) {
//        if (val == null) return 0.0;
//        if (val instanceof Number) return ((Number) val).doubleValue();
//        try { return Double.parseDouble(val.toString()); } catch (Exception e) { return 0.0; }
//    }
//
//    public Long getCurrentUserId() {
//        String username = SecurityContextHolder.getContext().getAuthentication().getName();
//        return userMapper.findByUsername(username).getId();
//    }
//}