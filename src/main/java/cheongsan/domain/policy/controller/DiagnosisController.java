package cheongsan.domain.policy.controller;


import cheongsan.domain.policy.dto.DiagnosisDTO;
import cheongsan.domain.policy.dto.SimpleDiagnosisDTO;
import cheongsan.domain.policy.dto.UserDiagnosisDTO;
import cheongsan.domain.policy.service.DiagnosisService;
import cheongsan.domain.user.dto.UserDTO;
import cheongsan.domain.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/cheongsan/diagnosis")
@RequiredArgsConstructor
@Log4j2
public class DiagnosisController {
    private final DiagnosisService diagnosisService;
    private final UserService userService;

    /**
     * 진단 설문 제출 API
     * - 프론트엔드는 UserDiagnosisDTO 구조의 JSON을 POST로 전송
     * - 컨트롤러에서는 해당 DTO를 파싱 후 서비스에 전달
     */
    @PostMapping("/submit")
    public ResponseEntity<DiagnosisDTO> submitDiagnosisSurvey(
            @RequestBody UserDiagnosisDTO userDiagnosisDTO
    ) {
        DiagnosisDTO result = diagnosisService.processDiagnosis(userDiagnosisDTO);

        Long currentUserId = 1L;
        Long recommendDiagnosisId = result.getId();

        userService.submitDiagnosisAnswerToUser(currentUserId, recommendDiagnosisId);

        return ResponseEntity.ok(result);
    }


    @GetMapping("/result")
    public ResponseEntity<SimpleDiagnosisDTO> getDiagnosisResult() {
        Long currentUserId = 1L;

        UserDTO userDTO = userService.getUser(currentUserId);
        log.info("userDTO={}", userDTO.getEmail());

        Long recommendedDiagnosisId = userDTO.getRecommendedProgramId();
        log.info("recommendDiagnosisId={}", recommendedDiagnosisId);
        if (recommendedDiagnosisId == null) {
            log.info("사용자(ID: {})가 추천받은 진단 결과가 없습니다. 진단을 먼저 진행하도록 안내합니다.", currentUserId);
            // 진단페이지를 띄워놔야한다.
            return ResponseEntity.noContent().build();
        }
        SimpleDiagnosisDTO simpleDiagnosisDTO = diagnosisService.getSimpleDiagnosis(recommendedDiagnosisId);
        log.info("simpleDiagnosisDTO={}", simpleDiagnosisDTO.toString());

        return ResponseEntity.ok(simpleDiagnosisDTO);
    }

    @GetMapping("/result/{diagnosisId}")
    public ResponseEntity<DiagnosisDTO> getDiagnosisProgramDetail(
            @PathVariable("diagnosisId") Long diagnosisId) {
        DiagnosisDTO diagnosisDTO = diagnosisService.getDiagnosis(diagnosisId);

        log.info("diagnosisDTO={}", diagnosisDTO);
        log.info("diagnosisDTO content={}", diagnosisDTO.getProgramName());
        return ResponseEntity.ok(diagnosisDTO);
    }

    @PostMapping("/restart")
    public ResponseEntity<?> restartDiagnosis() {
        Long currentUserId = 1L;
        diagnosisService.deleteDiagnosis(currentUserId);

        return ResponseEntity.ok("삭제 완료");
    }


}
