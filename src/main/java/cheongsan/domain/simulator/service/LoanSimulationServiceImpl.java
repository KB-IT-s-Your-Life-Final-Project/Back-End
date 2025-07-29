package cheongsan.domain.simulator.service;

import cheongsan.domain.simulator.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LoanSimulationServiceImpl implements LoanSimulationService {

    private final LoanRepaymentCalculator calculator;

    @Override
    public LoanAnalyzeResponseDTO analyze(LoanAnalyzeRequestDTO request) {
        return null;
    }

    @Override
    public TotalComparisonResultDTO compareTotalRepaymentWithNewLoan(List<LoanDTO> existingLoans, LoanDTO newLoan) {
        if (newLoan == null) {
            throw new IllegalArgumentException("새 대출 정보(newLoan)는 null일 수 없습니다.");
        }
        BigDecimal existingTotal = calculateTotalRepayment(existingLoans);


        List<LoanDTO> loansWithNew = new ArrayList<>(existingLoans);
        loansWithNew.add(newLoan);
        BigDecimal withNewLoanTotal = calculateTotalRepayment(loansWithNew);

        BigDecimal increaseAmount = withNewLoanTotal.subtract(existingTotal);
        BigDecimal increaseRate = existingTotal.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO : increaseAmount.divide(existingTotal, 4, RoundingMode.HALF_UP);
        return new TotalComparisonResultDTO(
                existingTotal.setScale(0, RoundingMode.HALF_UP),
                withNewLoanTotal.setScale(0, RoundingMode.HALF_UP),
                increaseRate.multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP)
        );
    }

    @Override
    public InterestComparisonResultDTO compareInterestWithNewLoan(List<LoanDTO> existingLoans, LoanDTO newLoan) {
        if (newLoan == null) {
            throw new IllegalArgumentException("새 대출 정보(newLoan)는 null일 수 없습니다.");
        }

        BigDecimal existingTotalRepayment = calculateTotalRepayment(existingLoans);
        BigDecimal existingPrincipalSum = existingLoans.stream()
                .map(LoanDTO::getPrincipal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal existingInterest = existingTotalRepayment.subtract(existingPrincipalSum);

        List<LoanDTO> withNewLoanList = new ArrayList<>(existingLoans);
        withNewLoanList.add(newLoan);

        BigDecimal withNewLoanTotalRepayment = calculateTotalRepayment(withNewLoanList);
        BigDecimal withNewLoanPrincipalSum = withNewLoanList.stream() // 🔧 수정된 부분
                .map(LoanDTO::getPrincipal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal withNewLoanInterest = withNewLoanTotalRepayment.subtract(withNewLoanPrincipalSum);

        BigDecimal increaseAmount = withNewLoanInterest.subtract(existingInterest);
        BigDecimal increaseRate = existingInterest.compareTo(BigDecimal.ZERO) == 0 ?
                BigDecimal.ZERO : increaseAmount.divide(existingInterest, 4, RoundingMode.HALF_UP);

        System.out.println("existingTotalRepayment = " + existingTotalRepayment);
        System.out.println("existingPrincipalSum = " + existingPrincipalSum);
        System.out.println("withNewLoanTotalRepayment = " + withNewLoanTotalRepayment);
        System.out.println("withNewLoanPrincipalSum = " + withNewLoanPrincipalSum);
        return new InterestComparisonResultDTO(
                existingInterest.setScale(0, RoundingMode.HALF_UP),
                withNewLoanInterest.setScale(0, RoundingMode.HALF_UP),
                increaseRate.multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP)
        );


    }


    private BigDecimal calculateTotalRepayment(List<LoanDTO> loans) {
        BigDecimal totalRepayment = BigDecimal.ZERO;

        for (LoanDTO loan : loans) {
            int months = (int) ChronoUnit.MONTHS.between(loan.getStartDate().withDayOfMonth(1), loan.getEndDate().withDayOfMonth(1)) + 1;
            PaymentResultDTO result;
            BigDecimal annualRate = BigDecimal.valueOf(loan.getInterestRate());
            BigDecimal monthlyRate = annualRate.divide(BigDecimal.valueOf(12), 10, RoundingMode.HALF_UP);

            switch (loan.getRepaymentType()) {
                case EQUAL_PAYMENT:
                    result = calculator.calculateEqualPayment(
                            monthlyRate,
                            months,
                            loan.getPrincipal()
                    );
                    break;
                case EQUAL_PRINCIPAL:
                    result = calculator.calculateEqualPrincipal(
                            monthlyRate,
                            months,
                            loan.getPrincipal()
                    );
                    break;
                case LUMP_SUM:
                    result = calculator.calculateLumpSumRepayment(
                            loan.getPrincipal(),
                            annualRate,
                            months,
                            loan.getStartDate()
                    );
                    break;

                default:
                    throw new IllegalStateException("Unexpected value: " + loan.getRepaymentType());
            }
            totalRepayment = totalRepayment.add(result.getTotalPayment());
        }
        return totalRepayment;
    }
}
