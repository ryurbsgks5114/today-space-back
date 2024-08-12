package com.complete.todayspace.domain.payment.service;

import com.complete.todayspace.domain.payment.dto.KakaoApproveResponse;
import com.complete.todayspace.domain.payment.dto.PaymentInfoRequestDto;
import com.complete.todayspace.domain.payment.dto.ReadyResponseDto;
import com.complete.todayspace.domain.payment.entity.Payment;
import com.complete.todayspace.domain.payment.entity.State;
import com.complete.todayspace.domain.payment.repository.PaymentRepository;
import com.complete.todayspace.domain.product.entity.Product;
import com.complete.todayspace.domain.product.service.ProductService;
import com.complete.todayspace.domain.user.entity.User;
import com.complete.todayspace.global.exception.CustomException;
import com.complete.todayspace.global.exception.ErrorCode;
import jakarta.persistence.OptimisticLockException;
import java.util.HashMap;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);
    private final RestTemplate restTemplate;
    private ReadyResponseDto kakaoReady;
    private final PaymentRepository paymentRepository;
    private final ProductService productService;

    @Value("${KAKAO_PAY_SECRET_KEY}")
    private String KAKAO_PAY_SECRET_KEY;

    @Value("${KAKAO_PAY_URL}")
    private String KAKAO_PAY_URL;

    @Value("${KAKAO_PAY_CID}")
    private String cid;

    @Transactional
    public ReadyResponseDto readyPayment(User user, PaymentInfoRequestDto paymentInfoRequestDto) {

        HashMap<String, String> parameters = new HashMap<>();

        parameters.put("cid", cid);
        parameters.put("partner_order_id", String.valueOf(paymentInfoRequestDto.getProductId()));
        parameters.put("partner_user_id", String.valueOf(user.getId()));
        parameters.put("item_name", String.valueOf(paymentInfoRequestDto.getItem_name()));
        parameters.put("quantity", "0");
        parameters.put("total_amount", String.valueOf(paymentInfoRequestDto.getTotal_amount()));
        parameters.put("vat_amount", "0");
        parameters.put("tax_free_amount", "0");
        parameters.put("approval_url", "https://today-space.com/payment/success");
        parameters.put("cancel_url", "https://today-space.com/payment/cancel");
        parameters.put("fail_url", "https://today-space.com/payment/fail");

        HttpEntity<HashMap<String, String>> requestEntity = new HttpEntity<>(parameters, this.getHeaders());

       try{
           kakaoReady =  restTemplate.postForObject(
               KAKAO_PAY_URL+"/online/v1/payment/ready",
               requestEntity,
               ReadyResponseDto.class);

           Product product = productService.findByProduct(paymentInfoRequestDto.getProductId());

           Optional<Payment> existingPayment = paymentRepository.findFirstByProductIdAndState(
               paymentInfoRequestDto.getProductId(), State.PROGRESS);
           if (existingPayment.isPresent()) {
               throw new CustomException(ErrorCode.COMPLATED_PAYMENT);
           }

           Payment payment = new Payment(product, paymentInfoRequestDto.getTotal_amount(), State.PROGRESS, user);
           paymentRepository.save(payment);

       } catch (OptimisticLockException e) {

           throw new CustomException(ErrorCode.COMPLATED_PAYMENT);
       }


        return kakaoReady;
    }

    @Transactional
    public KakaoApproveResponse successPayment(User user, String pgToken, Long productId) {

        HashMap<String, String> parameters = new HashMap<>();

        parameters.put("cid", "TC0ONETIME");
        parameters.put("tid", kakaoReady.getTid());
        parameters.put("partner_order_id", String.valueOf(productId));
        parameters.put("partner_user_id", String.valueOf(user.getId()));
        parameters.put("pg_token", pgToken);

        HttpEntity<HashMap<String, String>> requestEntity = new HttpEntity<>(parameters,
            this.getHeaders());

        try{

            KakaoApproveResponse approveResponse = restTemplate.postForObject(
                KAKAO_PAY_URL+"/online/v1/payment/approve",
                requestEntity,
                KakaoApproveResponse.class);

            Payment payment = paymentRepository.findByProduct_Id(productId);
            payment.updateState(State.COMPLATE);


            return approveResponse;

        } catch (RestClientException e){

            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }

    }

    @Transactional
    public void cancelPayment(Long productId) {

        Payment payment = paymentRepository.findByProduct_Id(productId);
        paymentRepository.delete(payment);
    }

    private HttpHeaders getHeaders() {
        HttpHeaders httpHeaders = new HttpHeaders();

        String auth = "SECRET_KEY " + KAKAO_PAY_SECRET_KEY;

        httpHeaders.set("Authorization", auth);
        httpHeaders.set("Content-type", "application/json");

        return httpHeaders;
    }
}
