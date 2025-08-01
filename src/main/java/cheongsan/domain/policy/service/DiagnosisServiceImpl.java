package cheongsan.domain.policy.service;

import cheongsan.domain.policy.dto.DiagnosisDTO;
import cheongsan.domain.policy.dto.SimpleDiagnosisDTO;
import cheongsan.domain.policy.dto.UserDiagnosisDTO;
import cheongsan.domain.policy.entity.Diagnosis;
import cheongsan.domain.policy.entity.SimpleDiagnosis;
import cheongsan.domain.policy.mapper.PolicyMapper;
import cheongsan.domain.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class DiagnosisServiceImpl implements DiagnosisService {
    private final PolicyMapper policyMapper;
    private final UserMapper userMapper;

    /**
     * 진단 응답을 분석해, 유저 상태 판정 및 추천 정책 반환
     */
    @Override
    public DiagnosisDTO processDiagnosis(UserDiagnosisDTO request) {
        // (1) 사용자의 설문 답변 리스트를 가져옴
        List<UserDiagnosisDTO.Answer> answers = request.getAnswers();

        // (2) 답변들을 분석해서 유저 진단 결과(위험도, 상태 등) 판정
        Long diagnosisId = determineUserStatus(answers);

        // (3) 판단된 상태/조건을 토대로 정책 조회(실제 추천 로직은 정책 조건 매칭 규칙에 따라 달라짐)
        Diagnosis policy = policyMapper.getDiagnosisResult(diagnosisId);

        DiagnosisDTO diagnosisDTO = DiagnosisDTO.of(policy);

        return diagnosisDTO;
    }

    // 사용자 상태를 판단하는 간단 예시 로직 (실제 프로젝트 정책에 맞게 커스텀)
    // 0-일반 연체, 1 - 파산,2-회생,3-개인워크아웃,4-프리워크아웃,5-신속채무조정.
    private Long determineUserStatus(List<UserDiagnosisDTO.Answer> answers) {
        // 답변을 빠르게 조회하기 위한 Map 생성
        Map<Integer, Integer> answerMap = new HashMap<>();
        for (UserDiagnosisDTO.Answer answer : answers) {
            answerMap.put(answer.getQuestionId(), answer.getOptionId());
        }

        // 각 질문의 답변 추출
        Integer q1 = answerMap.get(1); // 연체 현황 (1~4) 연체x,30일이하,90일이하,90이상
        Integer q2 = answerMap.get(2); // 채무 규모 (1~3) 5억 미만,5~10억,10억 이상
        Integer q3 = answerMap.get(3); // 소득 여부 (1~2) 있음,없음

        // 연체 상황을 우선적으로 판단
        if (q1 == null) {
            return 0L; // 답변 불완전 시 기본값
        }

        switch (q1) {
            case 1: // 연체 없음
                return 0L; // 예방적 상담 또는 일반 금융상품 추천

            case 2: // 30일 이하 단기 연체
                if (q3 != null && q3 == 1) { // 소득 있음
                    return 5L; // 신속채무조정 (긴급)
                } else {
                    return 5L; // 신속채무조정
                }

            case 3: // 31~90일 연체
                if (q2 != null && q2 == 3) { // 채무 규모 높음 (10억 초과)
                    return 4L; // 프리워크아웃 (고액)
                } else {
                    return 4L; // 프리워크아웃
                }

            case 4: // 90일 이상 장기 연체
                if (q3 != null && q3 == 2) { // 소득 없음
                    if (q2 != null && q2 == 3) { // 채무도 높음
                        return 1L; // 개인파산
                    } else {
                        return 2L; // 개인회생 (소득 회복 가능성 고려)
                    }
                } else { // 소득 있음
                    return 3L; // 개인워크아웃
                }

            default:
                return 0L; // 예외 상황
        }
    }

    // 정책의 모든 내용 가져옴
    public DiagnosisDTO getDiagnosis(Long diagnosisId) {
        Diagnosis policy = policyMapper.getDiagnosisResult(diagnosisId);

        DiagnosisDTO diagnosisDTO = DiagnosisDTO.of(policy);
        return diagnosisDTO;
    }

    //정책 간략화하여 가져옴
    public SimpleDiagnosisDTO getSimpleDiagnosis(Long diagnosisId) {
        SimpleDiagnosis simpleDiagnosis = policyMapper.getSimpleDiagnosisResult(diagnosisId);
        SimpleDiagnosisDTO simpleDiagnosisDTO = SimpleDiagnosisDTO.of(simpleDiagnosis);
        log.info(simpleDiagnosis.toString());

        return simpleDiagnosisDTO;
    }

    public void deleteDiagnosis(Long userId) {
        // 유저 정책 내용 삭제
        userMapper.deleteDiagnosisByUserId(userId);

    }
}
