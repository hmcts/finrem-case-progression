package uk.gov.hmcts.reform.finrem.caseorchestration.controllers;

import com.google.common.collect.ImmutableList;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.ccd.client.model.AboutToStartOrSubmitCallbackResponse;
import uk.gov.hmcts.reform.ccd.client.model.CallbackRequest;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.finrem.caseorchestration.model.pba.payment.PaymentResponse;
import uk.gov.hmcts.reform.finrem.caseorchestration.service.FeeService;
import uk.gov.hmcts.reform.finrem.caseorchestration.service.PBAPaymentService;

import javax.validation.constraints.NotNull;

import java.util.Map;
import java.util.Objects;

import static uk.gov.hmcts.reform.finrem.caseorchestration.OrchestrationConstants.AUTHORIZATION_HEADER;
import static uk.gov.hmcts.reform.finrem.caseorchestration.model.ConsentedStatus.AWAITING_HWF_DECISION;
import static uk.gov.hmcts.reform.finrem.caseorchestration.model.ccd.CCDConfigConstant.AMOUNT_TO_PAY;
import static uk.gov.hmcts.reform.finrem.caseorchestration.model.ccd.CCDConfigConstant.ORDER_SUMMARY;
import static uk.gov.hmcts.reform.finrem.caseorchestration.model.ccd.CCDConfigConstant.PBA_NUMBER;
import static uk.gov.hmcts.reform.finrem.caseorchestration.model.ccd.CCDConfigConstant.PBA_PAYMENT_REFERENCE;
import static uk.gov.hmcts.reform.finrem.caseorchestration.model.ccd.CCDConfigConstant.STATE;

@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/case-orchestration")
@Slf4j
@SuppressWarnings("unchecked")
public class PBAPaymentController implements BaseController {

    private final FeeService feeService;
    private final PBAPaymentService pbaPaymentService;

    @SuppressWarnings("unchecked")
    @PostMapping(path = "/pba-payment", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Handles PBA Payments for Consented Journey")
    public ResponseEntity<AboutToStartOrSubmitCallbackResponse> pbaPayment(
            @RequestHeader(value = AUTHORIZATION_HEADER, required = false) String authToken,
            @NotNull @RequestBody @ApiParam("CaseData") CallbackRequest callbackRequest) {

        CaseDetails caseDetails = callbackRequest.getCaseDetails();
        log.info("Received request for PBA payment for consented for Case ID: {}", caseDetails.getId());

        log.info("************  Sleeping Before Processing PBA Payment");
        try {
            Thread.sleep(300 * 1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        log.info("************  Processing PBA Payment");

        validateCaseData(callbackRequest);

        final Map<String, Object> mapOfCaseData = caseDetails.getData();
        feeLookup(authToken, callbackRequest, mapOfCaseData);

        if (isPBAPayment(mapOfCaseData)) {
            if (isPBAPaymentReferenceDoesNotExists(mapOfCaseData)) {
                String ccdCaseId = Objects.toString(callbackRequest.getCaseDetails().getId());
                PaymentResponse paymentResponse = pbaPaymentService.makePayment(authToken, ccdCaseId, mapOfCaseData);
                if (!paymentResponse.isPaymentSuccess()) {
                    return paymentFailure(mapOfCaseData, paymentResponse);
                }
                mapOfCaseData.put(PBA_PAYMENT_REFERENCE, paymentResponse.getReference());
                log.info("Consented Payment Succeeded.");
            } else {
                log.info("PBA Payment Reference for Consented case already exists.");
            }
        } else {
            log.info("Not PBA Payment - Moving state to Awaiting HWF Decision");
            mapOfCaseData.put(STATE, AWAITING_HWF_DECISION.toString());
        }

        return ResponseEntity.ok(AboutToStartOrSubmitCallbackResponse.builder().data(mapOfCaseData).build());
    }

    private void feeLookup(@RequestHeader(value = AUTHORIZATION_HEADER, required = false) String authToken,
                           @RequestBody CallbackRequest callbackRequest, Map<String, Object> caseData) {
        ResponseEntity<AboutToStartOrSubmitCallbackResponse> feeResponse = new FeeLookupController(feeService)
                .feeLookup(authToken, callbackRequest);
        caseData.put(ORDER_SUMMARY, feeResponse.getBody().getData().get(ORDER_SUMMARY));
        caseData.put(AMOUNT_TO_PAY, feeResponse.getBody().getData().get(AMOUNT_TO_PAY));
    }

    private ResponseEntity<AboutToStartOrSubmitCallbackResponse> paymentFailure(Map<String, Object> caseData, PaymentResponse paymentResponse) {
        String paymentError = paymentResponse.getPaymentError();
        log.info("Payment by PBA number {} failed, payment error : {} ", caseData.get(PBA_NUMBER), paymentResponse.getPaymentError());

        return ResponseEntity.ok(AboutToStartOrSubmitCallbackResponse.builder()
            .errors(ImmutableList.of(paymentError))
            .build());
    }
}
