package com.gl.ceir.config.service.impl;

import com.gl.ceir.config.exceptions.InternalServicesException;
import com.gl.ceir.config.model.app.BlackList;
import com.gl.ceir.config.model.app.BlockApiReq;
import com.gl.ceir.config.model.app.BlockedImeiRequest;
import com.gl.ceir.config.model.app.Devices;
import com.gl.ceir.config.repository.app.BlackListRepository;
import com.gl.ceir.config.repository.app.RasFraudImeiReqRepo;
//import com.gl.custom.CustomCheck;
import com.gl.ceir.config.service.Validators;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
//import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import static com.gl.ceir.config.service.Validators.globalErrorMsgs;

import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Service
public class BlockedImeiRegisterServiceImpl {

    private static final Logger logger = LogManager.getLogger(BlockedImeiRegisterServiceImpl.class);

    @Value("${gdFailMessage}")
    private String gdFailMessage;

    @Value("${failMessage}")
    private String failMessage;

    @Value("${passMessage}")
    private String passMessage;

    @Value("${imeiInvalid_Msg}")
    private String imeiInvalid_Msg;

    @Value("${imeiReasonNotProvided}")
    private String imeiReasonNotProvided;

    @Autowired
    BlackListRepository blackListRepository;


    @Autowired
    RasFraudImeiReqRepo rasFraudImeiReqRepo;

    @Autowired
    SystemParamServiceImpl systemParamServiceImpl;


    public List<ResponseArray> registerService(List<Devices> req, BlockApiReq apiDetails) {
        int failCount = 0;
        int passCount = 0;
        List<ResponseArray> responseArray = new LinkedList<>();
        List<PrintReponse> a = new LinkedList<>();
        try {
            for (Devices data : req) {
                var reason = data.getReason();
                logger.info("Starting Registering for" + data);
                if (StringUtils.isBlank(data.getImei()) || StringUtils.isBlank(data.getReason())) {
                    logger.info("Mandatory param missing for " + data);
                    responseArray.add(new ResponseArray(data.getImei(), data.getMsisdn(), 203, imeiReasonNotProvided));
                    a.add(new PrintReponse(data.getImei(), data.getMsisdn(), 203, failMessage, imeiReasonNotProvided));
                    failCount++;
                } else if (data.getImei().length() < 14 || data.getImei().length() > 16 || !data.getImei().matches("^[ 0-9 ]+$")) {
                    logger.info("imei not valid : " + data.getImei());
                    responseArray.add(new ResponseArray(data.getImei(), data.getMsisdn(), 204, imeiInvalid_Msg));
                    a.add(new PrintReponse(data.getImei(), data.getMsisdn(), 204, failMessage, imeiInvalid_Msg));
                    failCount++;
                } else {
                    var b = new BlackList(data.getImei().substring(0, 14), data.getImei(), data.getImsi(), data.getMsisdn(), data.getImei().substring(0, 8), apiDetails.getRequestId(), "API");
                    var delta = checkImeiInBlackListData(b);
                    if (delta != null && delta.getSourceOfRequest().toLowerCase().contains(reason.toLowerCase())) { //  present in db
                        failCount++;
                        responseArray.add(new ResponseArray(b.getActualImei(), b.getMsisdn(), 201, gdFailMessage));
                        a.add(new PrintReponse(b.getActualImei(), b.getMsisdn(), 201, gdFailMessage, "Imei Present with source " + reason));
                    } else if (delta != null) {
                        if (updateInBlackListData(delta, reason)) {
                            passCount++;
                            responseArray.add(new ResponseArray(b.getActualImei(), b.getMsisdn(), 200, passMessage));
                            a.add(new PrintReponse(b.getActualImei(), b.getMsisdn(), 200, passMessage, "Pass"));
                        } else {
                            failCount++;
                            responseArray.add(new ResponseArray(b.getActualImei(), b.getMsisdn(), 202, failMessage));
                            a.add(new PrintReponse(b.getImei(), b.getMsisdn(), 202, failMessage, "Fail to accept the Block request"));
                        }
                    } else {
                        if (insertInBlackListData(b, reason)) {
                            passCount++;
                            responseArray.add(new ResponseArray(b.getActualImei(), b.getMsisdn(), 200, passMessage));
                            a.add(new PrintReponse(b.getActualImei(), b.getMsisdn(), 200, passMessage, "Pass"));
                        } else {
                            failCount++;
                            responseArray.add(new ResponseArray(b.getActualImei(), b.getMsisdn(), 202, failMessage));
                            a.add(new PrintReponse(b.getImei(), b.getMsisdn(), 202, failMessage, "Fail to accept the block request"));
                        }
                    }
                }
            }
            updateRegister(passCount, failCount, apiDetails);
        } catch (Exception e) {
            createFile(globalErrorMsgs("en"), "blockFraudIMEI", "response", apiDetails.getRequestId());
            logger.error(e + "in [" + Arrays.stream(e.getStackTrace()).filter(ste -> ste.getClassName().equals(BlockedImeiRegisterServiceImpl.class.getName())).collect(Collectors.toList()).get(0) + "]");
            throw new InternalServicesException("en", globalErrorMsgs("en"));
        }
        createFile(Arrays.toString(a.toArray()), "blockFraudIMEI", "response", apiDetails.getRequestId());
        return responseArray;
    }

    private void updateRegister(int passCount, int failCount, BlockApiReq obj) {
        obj.setRemark("200");
        obj.setStatusCode(200);
        obj.setStatus("DONE");
        obj.setFailCount(failCount);
        obj.setSuccessCount(passCount);
        rasFraudImeiReqRepo.save(obj);
    }

    private boolean insertInBlackListData(BlackList data, String reason) {
        try {
            data.setSourceOfRequest(reason);
            blackListRepository.save(data);
            return true;
        } catch (Exception e) {
            logger.warn("Not able to insert in blacklist, Exception :{}", e.getLocalizedMessage());
            return false;
        }
    }

    private boolean updateInBlackListData(BlackList data, String reason) {
        try {
            data.setSourceOfRequest(data.getSourceOfRequest() + "," + reason);
            blackListRepository.save(data);
            return true;
        } catch (Exception e) {
            logger.warn("Not able to update  in blacklist, Exception :{}", e.getLocalizedMessage());
            return false;
        }
    }


    private BlackList checkImeiInBlackListData(BlackList data) {
        return blackListRepository.getByImei(data.getImei());
    }

    public String createFile(String prm, String feature, String type, String reqId) {
        try {
            var filepath = systemParamServiceImpl.getValueByTag("BlackListImeiFraudApiFilePath") + "/" + feature + "/" + reqId + "/";
            Files.createDirectories(Paths.get(filepath));
            if (StringUtils.isBlank(prm.trim())) prm = globalErrorMsgs("en");
            logger.info("FullFilePath--" + filepath + reqId + "_" + type + ".txt");
            logger.info("Content-> " + prm);
            FileWriter writer = new FileWriter(filepath + reqId + "_" + type + ".txt");
            writer.write(prm);
            writer.close();
            var fileName = reqId + "_" + type + ".txt";//   callFileCopierApi(filepath, fileName ,reqId);
            return fileName;
        } catch (Exception e) {
            logger.error("Note -> Not able to create block file. Process will still continue {}", e.getLocalizedMessage());
        }
        return null;
    }
}


class PrintReponse {
    String imei;
    String serialNumber;
    int statusCode;
    String statusMessage;
    String response;

    public PrintReponse(String imei, String serialNumber, int statusCode, String statusMessage, String response) {
        this.imei = imei;
        this.serialNumber = serialNumber;
        this.statusCode = statusCode;
        this.statusMessage = statusMessage;
        this.response = response;
    }

    public String getImei() {
        return imei;
    }

    public void setImei(String imei) {
        this.imei = imei;
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public void setStatusMessage(String statusMessage) {
        this.statusMessage = statusMessage;
    }

    public String getResponse() {
        return response;
    }

    public void setResponse(String response) {
        this.response = response;
    }

    public PrintReponse() {
    }

    @Override
    public String toString() {
        return "{" + "imei='" + imei + '\'' + ", serialNumber='" + serialNumber + '\'' + ", statusCode=" + statusCode + ", statusMessage='" + statusMessage + '\'' + ", response='" + response + '\'' + '}';
    }
}

class ResponseArray {

    String imei;
    String serialNumber;
    int statusCode;
    String statusMessage;

    public ResponseArray() {
    }

    public ResponseArray(String imei, String serialNumber, int statusCode, String statusMessage) {
        this.imei = imei;
        this.serialNumber = serialNumber;
        this.statusCode = statusCode;
        this.statusMessage = statusMessage;
    }

    public String getImei() {
        return imei;
    }

    public void setImei(String imei) {
        this.imei = imei;
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public void setStatusMessage(String statusMessage) {
        this.statusMessage = statusMessage;
    }

    @Override
    public String toString() {
        return "{" + "imei='" + imei + '\'' + ", serialNumber='" + serialNumber + '\'' + ", statusCode=" + statusCode + ", statusMessage='" + statusMessage + '\'' + '}';
    }
}

