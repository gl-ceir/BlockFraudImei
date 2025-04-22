package com.gl.ceir.config.service;

import com.gl.ceir.config.exceptions.MissingRequestParameterException;
import com.gl.ceir.config.exceptions.PayloadSizeExceeds;
import com.gl.ceir.config.exceptions.UnAuthorizationException;
import com.gl.ceir.config.model.app.BlockApiReq;
import com.gl.ceir.config.model.app.BlockedImeiRequest;
import com.gl.ceir.config.model.app.UserVars;
import com.gl.ceir.config.repository.app.RasFraudImeiReqRepo;
import com.gl.ceir.config.service.impl.BlockedImeiRegisterServiceImpl;
import com.gl.ceir.config.service.impl.EirsResponseParamServiceImpl;
import com.gl.ceir.config.service.userlogic.UserFactory;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Base64;
import java.util.List;
import java.util.Optional;

@Service
public class Validators {

    @Value("${mandatoryParameterMissing}")
    private String mandatoryParameterMissing;

    @Value("${fraudImeiPayLoadMaxSize}")
    private String fraudImeiPayLoadMaxSize;

    @Value("${maxSizeDefinedException}")
    private String maxSizeDefinedException;

    @Value("${errorMessage}")
    private static String errorMessage;

    @Autowired
    UserFactory userFactory;

    @Autowired
    RasFraudImeiReqRepo rasFraudImeiReqRepo;

    @Autowired
    EirsResponseParamServiceImpl eirsResponseParamServiceImpl;

    private static final Logger logger = LogManager.getLogger(Validators.class);

    public void errorValidationCheckerForRegister(BlockedImeiRequest b, BlockApiReq obj) {
       logger.info("Going to Validations" + b);
        if (b == null || b.getDevices() == null || b.getDevices().isEmpty()) {
            logger.info("Rejected Due to data is empty");
            obj.setStatus("FAIL");
            obj.setRemark(eirsResponseParamServiceImpl.getValueByTag("blockFraudImeiReqLength0"));
            obj.setStatusCode(400);
            rasFraudImeiReqRepo.save(obj);
            throw new MissingRequestParameterException("en", mandatoryParameterMissing);
        }
        if (b.getDevices().size() > Integer.parseInt(fraudImeiPayLoadMaxSize)) {
            logger.info("Rejected Due to request size is more than payload defined ");
            obj.setStatus("FAIL");
            obj.setRemark(eirsResponseParamServiceImpl.getValueByTag("blockFraudImeiPayLoadMaxSize"));
            obj.setStatusCode(413);
            rasFraudImeiReqRepo.save(obj);
            throw new PayloadSizeExceeds("en", maxSizeDefinedException);
        }
    }

    public <T> void authorizationCheckerForCustom(HttpServletRequest request) {
        if (!Optional.ofNullable(request.getHeader("Authorization")).isPresent() || !request.getHeader("Authorization").startsWith("Basic ")) {
            logger.info("Rejected Due to  Authorization  Not Present" + request.getHeader("Authorization"));
            throw new UnAuthorizationException("en", globalErrorMsgs("en"));
        }
        logger.info("Basic Authorization present " + request.getHeader("Authorization").substring(6));
        try {
            var decodedString = new String(Base64.getDecoder().decode(request.getHeader("Authorization").substring(6)));
            logger.info("user:" + decodedString.split(":")[0] + "pass:" + decodedString.split(":")[1]);
            UserVars    userValue = (UserVars) userFactory.createUser().getUserDetailDao(decodedString.split(":")[0], decodedString.split(":")[1]);

            if (userValue == null || !userValue.getUsername().equals(decodedString.split(":")[0]) || !userValue.getPassword().equals(decodedString.split(":")[1])) {
                logger.info("username password not match");
                throw new UnAuthorizationException("en", globalErrorMsgs("en"));
            }
            logger.debug("Authentication Pass");
        } catch (Exception e) {
            logger.warn("Authentication fail" + e);
            throw new UnAuthorizationException("en", globalErrorMsgs("en"));
        }
    }

    public static String globalErrorMsgs(String a) {
        return errorMessage;
    }

}




//            if (systemParamServiceImpl.getValueByTag("CustomApiAuthWithIpCheck").equalsIgnoreCase("true")) {
//                var checkimeiFeatureType = systemParamServiceImpl.getValueByTag("CUSTOM_API_FEATURE_ID");
//                FeatureIpAccessList featureIpAccessList = featureIpAccessListRepository.getByFeatureId(checkimeiFeatureType);
//                logger.info(" featureIpAccess List  " + featureIpAccessList);
//                if (featureIpAccessList == null) {
//                    throw new UnAuthorizationException("en", customImeiCheckImeiServiceImpl.globalErrorMsgs("en"));
//                }
//                if (featureIpAccessList.getTypeOfCheck() == 1) {
//                    if (!featureIpAccessList.getIpAddress().contains(request.getHeader("Header_ip"))) {//checkImeiRequest.getHeader_public_ip()
//                        logger.info("Type Check 1 But Ip not allowed ");
//                        // checkImeiServiceImpl.saveCheckImeiFailDetails(checkImeiRequest, startTime, authFeatureIpNotMatch);
//                        throw new UnAuthorizationException("en", customImeiCheckImeiServiceImpl.globalErrorMsgs("en"));
//                    }
//                } else {
//                    logger.info("Type Check 2 with feature id  " + featureIpAccessList.getFeatureIpListId() + " And User id " + userValue.getId());
//                    UserFeatureIpAccessList userFeatureIpAccessList = userFeatureIpAccessListRepository.getByFeatureIpListIdAndUserId(featureIpAccessList.getFeatureIpListId(), userValue.getId());
//                    logger.info("Response from  UserFeatureIpAccess List " + userFeatureIpAccessList);
//                    if (userFeatureIpAccessList == null || !(userFeatureIpAccessList.getIpAddress().contains(request.getHeader("Header_ip")))) { //checkImeiRequest.getHeader_public_ip()
//                        logger.info("Type Check 2 But Ip not allowed ");
//                        throw new UnAuthorizationException("en", customImeiCheckImeiServiceImpl.globalErrorMsgs("en"));
//                    }
//                }
//            }

//            if (systemParamServiceImpl.getValueByTag("CustomApiAuthOperatorCheck").equalsIgnoreCase("true")) {
//                var systemConfig = systemConfigListRepository.findByTagAndInterp("OPERATORS", request.getHeader("Operator"));
//                if (systemConfig == null) {
//                    logger.info("Operator Not allowed ");
//                    throw new UnprocessableEntityException("en", globalErrorMsgs("en"));
//                }
//                logger.debug("Found operator with  value " + systemConfig.getValue());
//                userValue = (UserVars) userFactory.createUser().getUserDetailDao(decodedString.split(":")[0], decodedString.split(":")[1], systemConfig.getValue());
//            } else {
//                userValue = (UserVars) userFactory.createUser().getUserDetailDao(decodedString.split(":")[0], decodedString.split(":")[1]);
//            }