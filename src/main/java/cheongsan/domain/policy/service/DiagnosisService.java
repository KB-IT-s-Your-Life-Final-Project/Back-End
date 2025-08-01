package cheongsan.domain.policy.service;

import cheongsan.domain.policy.dto.DiagnosisDTO;
import cheongsan.domain.policy.dto.SimpleDiagnosisDTO;
import cheongsan.domain.policy.dto.UserDiagnosisDTO;

public interface DiagnosisService {
    /**
     * 진단 응답을 분석해, 유저 상태 판정 및 추천 정책 반환
     */
    DiagnosisDTO processDiagnosis(UserDiagnosisDTO request);

    DiagnosisDTO getDiagnosis(Long diagnosisId);

    SimpleDiagnosisDTO getSimpleDiagnosis(Long diagnosisId);

    void deleteDiagnosis(Long userId);
}
