package uk.gov.hmcts.reform.finrem.caseorchestration.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.finrem.caseorchestration.client.PaymentClient;
import uk.gov.hmcts.reform.finrem.caseorchestration.model.ccd.CaseData;
import uk.gov.hmcts.reform.finrem.caseorchestration.model.fee.FeeItem;
import uk.gov.hmcts.reform.finrem.caseorchestration.model.fee.FeeValue;
import uk.gov.hmcts.reform.finrem.caseorchestration.model.fee.OrderSummary;
import uk.gov.hmcts.reform.finrem.caseorchestration.model.pba.payment.FeeRequest;
import uk.gov.hmcts.reform.finrem.caseorchestration.model.pba.payment.PaymentRequest;
import uk.gov.hmcts.reform.finrem.caseorchestration.model.pba.payment.PaymentResponse;

import java.util.Map;


@Service
@RequiredArgsConstructor
@Slf4j
public class PBAPaymentService {
    private final PaymentClient paymentClient;

    @Value("${payment.api.siteId}")
    private String siteId;

    @Value("${payment.api.description}")
    private String description;


    public PaymentResponse makePayment(String authToken, String ccdCaseId, CaseData caseData) {
        log.info("Inside makePayment, authToken : {}, ccdCaseId : {}, caseData : {}", authToken, ccdCaseId, caseData);
        PaymentRequest paymentRequest = buildPaymenRequest(ccdCaseId, caseData);
        log.info("paymentRequest: {}", paymentRequest);
        PaymentResponse paymentResponse = paymentClient.pbaPayment(authToken, paymentRequest);
        log.info("paymentResponse : {} ", paymentResponse);
        return paymentResponse;
    }

    private PaymentRequest buildPaymenRequest(String ccdCaseId, CaseData caseData) {
        FeeRequest feeRequest = buildFeeRequest(caseData);
        log.info("Fee request : {} ", feeRequest);
        return buildPaymentRequest(ccdCaseId, caseData, feeRequest);
    }


    private FeeRequest buildFeeRequest(CaseData caseData) {
        OrderSummary orderSummary = caseData.getOrderSummary();
        FeeItem feeItem = orderSummary.getFees().get(0);
        FeeValue feeValue = feeItem.getValue();
        return FeeRequest.builder()
                .calculatedAmount(Long.valueOf(feeValue.getFeeAmount()))
                .code(feeValue.getFeeCode())
                .version(feeValue.getFeeVersion())
                .build();
    }

    private PaymentRequest buildPaymentRequest(String ccdCaseId, CaseData caseData, FeeRequest fee) {
        return PaymentRequest.builder()
                .accountNumber(caseData.getPbaNumber())
                .caseReference(caseData.getDivorceCaseNumber())
                .customerReference(caseData.getPbaReference())
                .ccdCaseNumber(ccdCaseId)
                .description(description)
                .organisationName(caseData.getSolicitorFirm())
                .siteId(siteId)
                .amount(fee.getCalculatedAmount())
                .feesList(ImmutableList.of(fee))
                .build();
    }

    /**
     * Version 2 starts
     */

    public PaymentResponse makePaymentV2(String authToken, String ccdCaseId, Map<String, Object> mapOfCaseData) {
        log.info("Inside makePayment, authToken : {}, ccdCaseId : {}, caseData : {}", authToken, ccdCaseId, mapOfCaseData);
        PaymentRequest paymentRequest = buildPaymenRequestv2(ccdCaseId, mapOfCaseData);
        log.info("paymentRequest: {}", paymentRequest);
        PaymentResponse paymentResponse = paymentClient.pbaPayment(authToken, paymentRequest);
        log.info("paymentResponse : {} ", paymentResponse);
        return paymentResponse;
    }

    private PaymentRequest buildPaymenRequestv2(String ccdCaseId, Map<String, Object> mapOfCaseData) {
        FeeRequest feeRequest = buildFeeRequestv2(mapOfCaseData);
        log.info("Fee request : {} ", feeRequest);
        return buildPaymentRequestv2(ccdCaseId, mapOfCaseData, feeRequest);
    }

    private FeeRequest buildFeeRequestv2(Map<String, Object> mapOfCaseData) {

        ObjectMapper mapper = new ObjectMapper();
        JsonNode dataJsonNode = mapper.valueToTree(mapOfCaseData);
        JsonNode feeValueAsJson = dataJsonNode.path("orderSummary").path("Fees").get(0).path("value");

        return FeeRequest.builder()
                .calculatedAmount(feeValueAsJson.path("FeeAmount").asLong())
                .code(feeValueAsJson.path("FeeCode").asText())
                .version(feeValueAsJson.path("FeeVersion").asText())
                .build();
    }

    private PaymentRequest buildPaymentRequestv2(String ccdCaseId, Map<String, Object> mapOfCaseData, FeeRequest fee) {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode dataJsonNode = mapper.valueToTree(mapOfCaseData);

        return PaymentRequest.builder()
                .accountNumber(dataJsonNode.path("pbaNumber").asText())
                .caseReference(dataJsonNode.path("divorceCaseNumber").asText())
                .customerReference(dataJsonNode.path("pbaReference").asText())
                .ccdCaseNumber(ccdCaseId)
                .description(description)
                .organisationName(dataJsonNode.path("solicitorFirm").asText())
                .siteId(siteId)
                .amount(fee.getCalculatedAmount())
                .feesList(ImmutableList.of(fee))
                .build();
    }

}
