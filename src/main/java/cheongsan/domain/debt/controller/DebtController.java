package cheongsan.domain.debt.controller;

import cheongsan.domain.debt.dto.*;
import cheongsan.domain.debt.service.DebtService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/cheongsan/dashboard")
@RequiredArgsConstructor
public class DebtController {
    private final DebtService debtService;

    // 추후, userId 연동 예정
    @GetMapping("/loans")
    public List<DebtInfoResponseDTO> getUserDebtList(
            @RequestParam Long userId,
            @RequestParam(required = false, defaultValue = "createdAtDesc") String sort
    ) {
        return debtService.getUserDebtList(userId);
    }

    @GetMapping("/loans/{loanId}")
    public DebtDetailResponseDTO getLoanDetail(@PathVariable Long loanId) {
        return debtService.getLoanDetail(loanId);
    }

    @PostMapping(value = "/loans", consumes = "application/json")
    public ResponseEntity<String> registerDebt(@RequestBody DebtRegisterRequestDTO dto,
                                               @RequestParam Long userId) {
        try {
            debtService.registerDebt(dto, userId);

            return ResponseEntity.ok()
                    .body("대출 상품 추가 성공");
        } catch (RuntimeException e) {
            if (e.getMessage().equals("이미 등록된 대출 계좌입니다.")) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body("이미 등록된 대출 계좌입니다.");
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("서버 오류가 발생했습니다.");
        }
    }

    @GetMapping("/loans/repaymentRatio")
    public RepaymentRatioResponseDTO getRepaymentRatio(@RequestParam Long userId) {
        return debtService.getRepaymentRatio(userId);
    }

    @GetMapping("/delinquentLoans")
    public List<DelinquentLoanResponseDTO> getDelinquentLoan(@RequestParam Long userId) {
        return debtService.getDelinquentLoans(userId);
    }

}
