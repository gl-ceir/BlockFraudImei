package com.gl.ceir.config.service.impl;

import com.gl.ceir.config.exceptions.InternalServicesException;
import com.gl.ceir.config.model.app.*;
import com.gl.ceir.config.repository.app.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.gl.ceir.config.service.Validators.globalErrorMsgs;


@Service
public class BlockedImeiRegisterServiceImpl {

    private static final Logger logger = LogManager.getLogger(BlockedImeiRegisterServiceImpl.class);

    @Value("${alreadyBlockedMessage}")
    private String alreadyBlockedMessage;

    @Value("${failMessage}")
    private String failMessage;

    @Value("${passMessage}")
    private String passMessage;

    @Value("${imeiInvalid_Msg}")
    private String imeiInvalid_Msg;

    @Value("${msisdnInvalid_Msg}")
    private String msisdnInvalid_Msg;

    @Value("${imsiInvalid_Msg}")
    private String imsiInvalid_Msg;

    @Value("${reasonInvalid_Msg}")
    private String reasonInvalid_Msg;

    @Value("${imeiReasonNotProvided}")
    private String imeiReasonNotProvided;

    @Autowired
    BlackListRepository blackListRepository;

    @Autowired
    RasFraudImeiReqRepo rasFraudImeiReqRepo;

    @Autowired
    SystemParamServiceImpl systemParamServiceImpl;

    @Autowired
    OperatorSeriesRepository operatorSeriesRepository;

    @Autowired
    BlackListHisRepository blackListHisRepository;

    @Autowired
    private ApplicationContext applicationContext;
    @Autowired
    private GreyListRepository greyListRepository;
    @Autowired
    private GreyListHisRepository greyListHisRepository;

    public List<ResponseArray> registerService(List<Devices> req, BlockApiReq apiDetails, Map<String, String> map) {
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
                    responseArray.add(new ResponseArray(data.getImei(), data.getMsisdn(), data.getImsi(), 203, imeiReasonNotProvided));
                    a.add(new PrintReponse(data.getImei(), data.getMsisdn(), data.getImsi(), 203, failMessage, imeiReasonNotProvided));
                    failCount++;
                } else if (data.getImei().length() < 14 || data.getImei().length() > 16 || !data.getImei().matches("^[0-9]+$")) {
                    logger.info("imei not valid : " + data.getImei());
                    responseArray.add(new ResponseArray(data.getImei(), data.getMsisdn(), data.getImsi(), 204, imeiInvalid_Msg));
                    a.add(new PrintReponse(data.getImei(), data.getMsisdn(), data.getImsi(), 204, failMessage, imeiInvalid_Msg));
                    failCount++;

                } else if (data.getMsisdn() != null && (data.getMsisdn().length() > 20 || !data.getMsisdn().matches("^[0-9]+$"))) {
                    logger.info("msisdn present && msisdn not valid :{}  ", data.getMsisdn());// true
                    responseArray.add(new ResponseArray(data.getImei(), data.getMsisdn(), data.getImsi(), 207, msisdnInvalid_Msg));
                    a.add(new PrintReponse(data.getImei(), data.getMsisdn(), data.getImsi(), 207, failMessage, msisdnInvalid_Msg));
                    failCount++;
                } else if (data.getImsi() != null && (data.getImsi().length() != 15 || !data.getImsi().matches("^[0-9]+$"))) {
                    logger.info("imsi present &&  imsi not valid :{} ", data.getImsi());// true
                    responseArray.add(new ResponseArray(data.getImei(), data.getMsisdn(), data.getImsi(), 205, imsiInvalid_Msg));
                    a.add(new PrintReponse(data.getImei(), data.getMsisdn(), data.getImsi(), 205, failMessage, imsiInvalid_Msg));
                    failCount++;
                } else if (!checkReason(data)) {
                    logger.info("Reason is not valid :{} ", data.getReason());// true
                    responseArray.add(new ResponseArray(data.getImei(), data.getMsisdn(), data.getImsi(), 206, reasonInvalid_Msg));
                    a.add(new PrintReponse(data.getImei(), data.getMsisdn(), data.getImsi(), 206, failMessage, reasonInvalid_Msg));
                    failCount++;
                } else {
                    var os = getOperator(data.getImsi(), data.getMsisdn());
                    logger.info(data);
            
                    var mode = "blacklist";
                    var ldt = LocalDateTime.now();
                    var days = applicationContext.getEnvironment().getProperty(data.getReason() + "_days");
                    GenericList b = new BlackList(data.getImei(), data.getImsi(), data.getMsisdn(), apiDetails.getRequestId(), Math.toIntExact(os.getId()), os.getOperatorName(), map.get("usertype"), map.get("userid"), ldt);
                    if (!days.equalsIgnoreCase("0")) {
                        mode = "greylist";
                        ldt = LocalDateTime.now().plusDays(Integer.parseInt(days));
                        logger.info("Addtional days {}",ldt );
                        b = new GreyList(data.getImei(), data.getImsi(), data.getMsisdn(), apiDetails.getRequestId(), Math.toIntExact(os.getId()), os.getOperatorName(), map.get("usertype"), map.get("userid"), ldt);
                    }
                    var delta = checkImeiInBlackListData(b, mode);
                    if (delta != null && delta.getSourceOfRequest() != null && delta.getSourceOfRequest().toLowerCase().contains(reason.toLowerCase())) { //  present in db
                        failCount++;
                        responseArray.add(new ResponseArray(b.getActualImei(), b.getMsisdn(), b.getImsi(), 201, alreadyBlockedMessage));
                        a.add(new PrintReponse(b.getActualImei(), b.getMsisdn(), b.getImsi(), 201, alreadyBlockedMessage, "Imei Present with source " + reason));
                    } else if (delta != null) {
                        if (updateInBlackListData(delta, reason, mode)) {
                            passCount++;
                            responseArray.add(new ResponseArray(b.getActualImei(), b.getMsisdn(), b.getImsi(), 200, passMessage));
                            a.add(new PrintReponse(b.getActualImei(), b.getMsisdn(), b.getImsi(), 200, passMessage, "Pass"));
                        } else {
                            failCount++;
                            responseArray.add(new ResponseArray(b.getActualImei(), b.getMsisdn(), b.getImsi(), 202, failMessage));
                            a.add(new PrintReponse(b.getImei(), b.getMsisdn(), b.getImsi(), 202, failMessage, "Fail to accept the Block request"));
                        }
                    } else {
                        if (insertInBlackListData(b, reason, mode)) {
                            passCount++;
                            responseArray.add(new ResponseArray(b.getActualImei(), b.getMsisdn(), b.getImsi(), 200, passMessage));
                            a.add(new PrintReponse(b.getActualImei(), b.getMsisdn(), b.getImsi(), 200, passMessage, "Pass"));
                        } else {
                            failCount++;
                            responseArray.add(new ResponseArray(b.getActualImei(), b.getMsisdn(), b.getImsi(), 202, failMessage));
                            a.add(new PrintReponse(b.getImei(), b.getMsisdn(), b.getImsi(), 202, failMessage, "Fail to accept the block request"));
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

    private boolean checkReason(Devices data) {
        try {
            if (applicationContext.getEnvironment().getProperty(data.getReason() + "_days") == null) return false;
            else return true;
        } catch (Exception e) {
            logger.info("No property found for {}", data.getReason());
            return false;
        }
    }

    private OperatorSeries getOperator(String imsi, String msisdn) {
        if (msisdn != null) {
            int ms = Integer.parseInt(msisdn.substring(0, 6));
            var ops = operatorSeriesRepository.findAll().stream().filter(o -> o.getSeriesType().equalsIgnoreCase("msisdn")).filter(o -> o.getSeriesStart() <= ms && o.getSeriesEnd() >= ms).findAny();
            if (ops.isPresent()) return ops.get();
        }
        if (imsi != null) {
            int ms = Integer.parseInt(imsi.substring(0, 5));
            var ops = operatorSeriesRepository.findAll().stream().filter(o -> o.getSeriesType().equalsIgnoreCase("imsi")).filter(o -> o.getSeriesStart() <= ms && o.getSeriesEnd() >= ms).findAny();
            if (ops.isPresent()) return ops.get();
        }
        return new OperatorSeries(0L, "");
    }

    private void updateRegister(int passCount, int failCount, BlockApiReq obj) {
        obj.setRemark("Pass");
        obj.setStatusCode(200);
        obj.setStatus("DONE");
        obj.setFailCount(failCount);
        obj.setSuccessCount(passCount);
        rasFraudImeiReqRepo.save(obj);
    }

    private boolean insertIntoBlackListHistory(BlackList b) {
        try {
            var blh = new BlackListHis(b.getActualImei(), b.getImsi(), b.getMsisdn(), b.getTxnId(), b.getOperator_id(), b.getOperatorName(), b.getSourceOfRequest(), 1, b.getUserType(), b.getUserId());
            blackListHisRepository.save(blh);
            return true;
        } catch (Exception e) {
            logger.warn("Not able to insert in blacklist his, Exception :{}", e.getLocalizedMessage());
            return false;
        }
    }


    private boolean insertIntoGreyListHistory(GreyList b) {
        try {
            var glh = new GreyListHis(b.getActualImei(), b.getImsi(), b.getMsisdn(), b.getTxnId(), b.getOperator_id(), b.getOperatorName(), b.getSourceOfRequest(), 1, b.getUserType(), b.getUserId());
            greyListHisRepository.save(glh);
            return true;
        } catch (Exception e) {
            logger.warn("Not able to insert in greylsit his, Exception :{}", e.getLocalizedMessage());
            return false;
        }
    }

    private boolean insertInBlackListData(GenericList data, String reason, String mode) {
        try {
            data.setSourceOfRequest(reason);
            if (mode.equalsIgnoreCase("greylist")) {
                var gl = greyListRepository.save((GreyList) data);
                if (insertIntoGreyListHistory(gl)) return true;
                else return false;
            } else {
                var bl = blackListRepository.save((BlackList) data);
                if (insertIntoBlackListHistory(bl)) return true;
                else return false;
            }
        } catch (Exception e) {
            logger.warn("Not able to insert in blacklist, Exception :{}", e.getLocalizedMessage());
            return false;
        }
    }


    private boolean updateInBlackListData(GenericList data, String reason, String mode) {
        try {
            data.setSourceOfRequest(data.getSourceOfRequest() + "," + reason);
            if (mode.equalsIgnoreCase("greylist")) {
                greyListRepository.save((GreyList) data);
            } else {
                blackListRepository.save((BlackList) data);
            }
            return true;
        } catch (Exception e) {
            logger.warn("Not able to update  in blacklist, Exception :{}", e.getLocalizedMessage());
            return false;
        }
    }


    private GenericList checkImeiInBlackListData(GenericList data, String mode) {
        if (mode.equalsIgnoreCase("greylist"))
            return greyListRepository.findFirstByImeiAndImsiAndMsisdn(data.getImei(), data.getImsi(), data.getMsisdn());
        else
            return blackListRepository.findFirstByImeiAndImsiAndMsisdn(data.getImei(), data.getImsi(), data.getMsisdn());
    }

    public String createFile(String prm, String feature, String type, String reqId) {
        try {
            if (!systemParamServiceImpl.getValueByTag("generate_req_resp_file").equalsIgnoreCase("Yes")) {
                return "";
            }
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
    String msisdn;
    String imsi;
    int statusCode;
    String statusMessage;
    String response;

    public PrintReponse(String imei, String msisdn, String imsi, int statusCode, String statusMessage, String response) {
        this.imei = imei;
        this.msisdn = msisdn;
        this.imsi = imsi;
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

    public String getMsisdn() {
        return msisdn;
    }

    public void setMsisdn(String msisdn) {
        this.msisdn = msisdn;
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

    public String getImsi() {
        return imsi;
    }

    public void setImsi(String imsi) {
        this.imsi = imsi;
    }

    public PrintReponse() {
    }

    @Override
    public String toString() {
        return "{" + "imei='" + imei + '\'' + ", msisdn='" + msisdn + '\'' + ", statusCode=" + statusCode + ", statusMessage='" + statusMessage + '\'' + ", response='" + response + '\'' + '}';
    }
}

class ResponseArray {

    String imei;
    String msisdn;
    String imsi;
    int statusCode;
    String statusMessage;

    public ResponseArray() {
    }

    public String getImei() {
        return imei;
    }

    public void setImei(String imei) {
        this.imei = imei;
    }

    public String getMsisdn() {
        return msisdn;
    }

    public void setMsisdn(String msisdn) {
        this.msisdn = msisdn;
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

    public String getImsi() {
        return imsi;
    }
    public void setImsi(String imsi) {
        this.imsi = imsi;
    }

    public ResponseArray(String imei, String msisdn, String imsi, int statusCode, String statusMessage) {
        this.imei = imei;
        this.msisdn = msisdn;
        this.imsi = imsi;
        this.statusCode = statusCode;
        this.statusMessage = statusMessage;
    }

    @Override
    public String toString() {
        return "{" + "imei='" + imei + '\'' + ", msisdn='" + msisdn + '\'' + ", imsi='" + imsi + '\'' + ", statusCode=" + statusCode + ", statusMessage='" + statusMessage + '\'' + '}';
    }
}

