package cheongsan.domain.policy.service;

import cheongsan.domain.policy.dto.PolicyDTO;
import cheongsan.domain.policy.dto.PolicyRequestDTO;
import cheongsan.domain.user.dto.UserDTO;
import cheongsan.domain.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PolicyServiceImpl implements PolicyService {
    private static final String POLICY_API_URL = "https://apis.data.go.kr/B554287/NationalWelfareInformationsV001/NationalWelfarelistV001";
    private final UserService userService;

    @Value("${policy.service-key}")
    private String SERVICE_KEY;

    public List<PolicyDTO> getPolicyList(PolicyRequestDTO policyRequestDTO) throws Exception {

        StringBuilder urlBuilder = new StringBuilder(POLICY_API_URL);

        urlBuilder.append("?serviceKey=").append(URLEncoder.encode(SERVICE_KEY, "UTF-8"));

        String callTp = policyRequestDTO.getCallTp();
        urlBuilder.append("&callTp=").append(callTp);

        Integer pageNo = policyRequestDTO.getPageNo();
        urlBuilder.append("&pageNo=").append(pageNo);

        Integer numOfRows = policyRequestDTO.getNumOfRows();
        urlBuilder.append("&numOfRows=").append(numOfRows);

        String srchKeyCode = policyRequestDTO.getSrchKeyCode();
        urlBuilder.append("&srchKeyCode=").append(srchKeyCode);

        // searchWrd가 null이거나 빈값이면 유저 신용 상태 가져오고 "청년"을 기본값으로
        String searchWrd = policyRequestDTO.getSearchWrd();
        if (searchWrd == null || searchWrd.trim().isEmpty()) {
            //유저 신용 상태 가져오기
            UserDTO userDTO = userService.getUser(2L);
            searchWrd = "청년";
            // 신용 상태에 따라 "부채","대출","청년"
            Long userDiagnosisResult = userDTO.getRecommendedProgramId();
            log.info("userDianosisResult={}", userDiagnosisResult);

            if (userDiagnosisResult != null) {
                if (userDiagnosisResult.equals(1L) || userDiagnosisResult.equals(2L)) {
                    searchWrd = "부채";
                } else if (userDiagnosisResult.equals(3L) || userDiagnosisResult.equals(4L) || userDiagnosisResult.equals(5L)) {
                    searchWrd = "대출";
                } else {
                    searchWrd = "청년";
                }
            } else {
                searchWrd = "청년";
            }


        }
        urlBuilder.append("&searchWrd=").append(URLEncoder.encode(searchWrd, "UTF-8"));

        log.info("urlBuilder={}", urlBuilder);

        URL url = new URL(urlBuilder.toString());
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");

        List<PolicyDTO> policyList = new ArrayList<>();
        try {
            conn.setRequestProperty("Content-Type", "application/xml; charset=UTF-8");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            int responseCode = conn.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw new RuntimeException("API 호출 실패: " + responseCode);
            }


            // XML 파싱
            Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(conn.getInputStream());
            doc.getDocumentElement().normalize();
            NodeList nodeList = doc.getElementsByTagName("servList");

            for (int i = 0; i < nodeList.getLength(); i++) {
                Node node = nodeList.item(i);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    Element e = (Element) node;
                    PolicyDTO dto = new PolicyDTO();

                    List<String> themaList = toStringList(getTagValue("intrsThemaArray", e));
                    List<String> groupList = toStringList(getTagValue("trgterIndvdlArray", e));
                    dto.setJurMnofNm(getTagValue("jurMnofNm", e));
                    dto.setSummary(getTagValue("servDgst", e));
                    dto.setServiceId(getTagValue("servId", e));
                    dto.setServiceName(getTagValue("servNm", e));
                    dto.setSupportCycle(getTagValue("sprtCycNm", e));

                    List<String> combinedList = new ArrayList<>();
                    if (themaList != null) combinedList.addAll(themaList);
                    if (groupList != null) combinedList.addAll(groupList);

                    dto.setTagList(combinedList);
                    policyList.add(dto);

                }
            }


        } finally {
            conn.disconnect();
        }

        return policyList;

    }

    private String getTagValue(String tag, Element element) {
        NodeList nodeList = element.getElementsByTagName(tag);
        if (nodeList.getLength() == 0) return null;
        Node node = nodeList.item(0).getFirstChild();
        return node == null ? null : node.getNodeValue();
    }

    // 콤마(,) 구분 문자열을 리스트로 변환하는 함수
    private List<String> toStringList(String value) {
        if (value == null || value.trim().isEmpty()) return Collections.emptyList();
        String[] arr = value.split(",");
        List<String> result = new ArrayList<>();
        for (String str : arr) {
            String trimmed = str.trim();
            if (!trimmed.isEmpty()) result.add(trimmed);
        }
        return result;
    }
}
