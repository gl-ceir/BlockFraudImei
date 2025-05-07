package com.gl.ceir.config.controller;

import com.gl.ceir.config.exceptions.MissingRequestParameterException;
import com.gl.ceir.config.exceptions.PayloadSizeExceeds;
import com.gl.ceir.config.exceptions.UnAuthorizationException;
import com.gl.ceir.config.exceptions.UnprocessableEntityException;
import com.gl.ceir.config.model.app.*;
import com.gl.ceir.config.repository.app.*;
import com.gl.ceir.config.service.Validators;
import com.gl.ceir.config.service.impl.BlockedImeiRegisterServiceImpl;
import com.gl.ceir.config.service.impl.EirsResponseParamServiceImpl;
import com.gl.ceir.config.service.impl.SystemParamServiceImpl;
import com.gl.ceir.config.service.userlogic.UserFactory;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.json.MappingJacksonValue;
import org.springframework.web.bind.annotation.*;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@RestController
public class BlockedImeiCheckController {

    private static final Logger logger = LogManager.getLogger(BlockedImeiCheckController.class);

    @Autowired
    Validators validators;

    @Autowired
    BlockedImeiRegisterServiceImpl blockedImeiServiceImpl;

    @Autowired
    RasFraudImeiReqRepo rasFraudImeiReqRepo;

    @CrossOrigin(origins = "", allowedHeaders = "")
    @PostMapping("/block/add")
    public ResponseEntity registerFraudImei(HttpServletRequest httpRequest, @RequestBody BlockedImeiRequest blockedImeiRequest) {
        getHeaderDetails(httpRequest);
        var map= validators.authorizationChecker(httpRequest);
        logger.info("Request :: {} ", blockedImeiRequest);
        String reqId = new SimpleDateFormat("yyyyMMddHHmmssSSS").format(new Date());
        String fileName = blockedImeiServiceImpl.createFile(blockedImeiRequest.toString(), "blockFraudIMEI", "req", reqId);
        var obj = rasFraudImeiReqRepo.save(new BlockApiReq("INIT", 201, blockedImeiRequest.getDevices()== null ? 0:  blockedImeiRequest.getDevices().size(), "RAS", fileName, reqId));
        validators.errorValidationCheckerForRegister(blockedImeiRequest, obj);
        var value = blockedImeiServiceImpl.registerService(blockedImeiRequest.getDevices(), obj,map);
        return ResponseEntity.status(HttpStatus.OK).headers(HttpHeaders.EMPTY).body(new MappingJacksonValue(value));
    }


    private void getHeaderDetails(HttpServletRequest request) {
        Map<String, String> headers = Collections.list(request.getHeaderNames())
                .stream()
                .collect(Collectors.toMap(h -> h, request::getHeader));
        logger.info("Headers->  {}", headers);
    }


//    public CheckImeiRequest saveCheckImeiRequest(CheckImeiRequest checkImeiRequest, long startTime) {
//        try {
//            checkImeiRequest.setCheckProcessTime(String.valueOf(System.currentTimeMillis() - startTime));
//            return checkImeiRequestRepository.save(checkImeiRequest);
//        } catch (Exception e) {
//            alertServiceImpl.raiseAnAlert("alert1110", 0);
//            throw new InternalServicesException(checkImeiRequest.getLanguage(), globalErrorMsgs(checkImeiRequest.getLanguage()));
//        }
//    }

//    public String globalErrorMsgs(String language) {
//        return chkImeiRespPrmRepo.getByTagAndLanguage("CheckImeiErrorMessage", language).getValue();
//    }

//    public String createFile(String prm, String feature, String type, String reqId) {
//        try {
//            var filepath = systemParamServiceImpl.getValueByTag("CustomApiFilePath") + "/" + feature + "/" + reqId + "/";
//            Files.createDirectories(Paths.get(filepath));
//            if (StringUtils.isBlank(prm)) prm = globalErrorMsgs("en");
//            logger.info("FullFilePath " + filepath + reqId + "_" + type + ".txt");
//            FileWriter writer = new FileWriter(filepath + reqId + "_" + type + ".txt");
//            writer.write(prm);
//            writer.close();
//            return reqId + "_" + type + ".txt";
//        } catch (Exception e) {
//            logger.error("Not able to create custom file {}", e.getLocalizedMessage());
//        }
//        return null;
//    }
}
