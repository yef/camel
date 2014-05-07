/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.component.smpp;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.jsmpp.bean.DataCoding;
import org.jsmpp.bean.DataSm;
import org.jsmpp.bean.ESMClass;
import org.jsmpp.bean.NumberingPlanIndicator;
import org.jsmpp.bean.OptionalParameter;
import org.jsmpp.bean.OptionalParameter.Byte;
import org.jsmpp.bean.OptionalParameter.COctetString;
import org.jsmpp.bean.OptionalParameter.Int;
import org.jsmpp.bean.OptionalParameter.Null;
import org.jsmpp.bean.OptionalParameter.OctetString;
import org.jsmpp.bean.OptionalParameter.Short;
import org.jsmpp.bean.OptionalParameter.Tag;
import org.jsmpp.bean.RegisteredDelivery;
import org.jsmpp.bean.TypeOfNumber;
import org.jsmpp.session.DataSmResult;
import org.jsmpp.session.SMPPSession;
import org.owasp.encoder.Encode;

public class SmppDataSmCommand extends AbstractSmppCommand {

    public SmppDataSmCommand(SMPPSession session, SmppConfiguration config) {
        super(session, config);
    }

    @Override
    public void execute(Exchange exchange) throws SmppException {
        DataSm dataSm = createDataSm(exchange);

        if (log.isDebugEnabled()) {
            log.debug("Sending a data short message for exchange id '{}'...", exchange.getExchangeId());
        }

        DataSmResult result;
        try {
            result = session.dataShortMessage(
                    dataSm.getServiceType(),
                    TypeOfNumber.valueOf(dataSm.getSourceAddrTon()),
                    NumberingPlanIndicator.valueOf(dataSm.getSourceAddrNpi()),
                    dataSm.getSourceAddr(),
                    TypeOfNumber.valueOf(dataSm.getDestAddrTon()),
                    NumberingPlanIndicator.valueOf(dataSm.getDestAddrNpi()),
                    dataSm.getDestAddress(),
                    new ESMClass(dataSm.getEsmClass()),
                    new RegisteredDelivery(dataSm.getRegisteredDelivery()),
                    DataCoding.newInstance(dataSm.getDataCoding()),
                    dataSm.getOptionalParametes());
        } catch (Exception e) {
            throw new SmppException(e);
        }

        if (log.isDebugEnabled()) {
            log.debug("Sent a data short message for exchange id '{}' and message id '{}'",
                    Encode.forJava(exchange.getExchangeId()), Encode.forJava(result.getMessageId()));
        }

        Message message = getResponseMessage(exchange);
        message.setHeader(SmppConstants.ID, result.getMessageId());
        message.setHeader(SmppConstants.OPTIONAL_PARAMETERS, getOptionalParametersAsMap(result.getOptionalParameters()));
    }

    protected Map<String, String> getOptionalParametersAsMap(OptionalParameter[] optionalParameters) {
        if (optionalParameters == null) {
            return null;
        }

        Map<String, String> optParams = new HashMap<String, String>();
        for (OptionalParameter optionalParameter : optionalParameters) {

            String value = null;
            if (optionalParameter instanceof COctetString) {
                value = ((COctetString) optionalParameter).getValueAsString();
            } else if (optionalParameter instanceof OctetString) {
                value = ((OctetString) optionalParameter).getValueAsString();
            } else if (optionalParameter instanceof Int) {
                value = String.valueOf(((Int) optionalParameter).getValue());
            } else if (optionalParameter instanceof Short) {
                value = String.valueOf(((Short) optionalParameter).getValue());
            } else if (optionalParameter instanceof Byte) {
                value = String.valueOf(((Byte) optionalParameter).getValue());
            } else if (optionalParameter instanceof Null) {
                value = null;
            }
            optParams.put(Tag.valueOf(optionalParameter.tag).name(), value);
        }

        return optParams;
    }

    @SuppressWarnings({"unchecked"})
    protected DataSm createDataSm(Exchange exchange) {
        Message in = exchange.getIn();
        DataSm dataSm = new DataSm();

        if (in.getHeaders().containsKey(SmppConstants.DATA_CODING)) {
            dataSm.setDataCoding(in.getHeader(SmppConstants.DATA_CODING, java.lang.Byte.class));
        } else {
            dataSm.setDataCoding(config.getDataCoding());
        }

        if (in.getHeaders().containsKey(SmppConstants.DEST_ADDR)) {
            dataSm.setDestAddress(in.getHeader(SmppConstants.DEST_ADDR, String.class));
        } else {
            dataSm.setDestAddress(config.getDestAddr());
        }

        if (in.getHeaders().containsKey(SmppConstants.DEST_ADDR_TON)) {
            dataSm.setDestAddrTon(in.getHeader(SmppConstants.DEST_ADDR_TON, java.lang.Byte.class));
        } else {
            dataSm.setDestAddrTon(config.getDestAddrTon());
        }

        if (in.getHeaders().containsKey(SmppConstants.DEST_ADDR_NPI)) {
            dataSm.setDestAddrNpi(in.getHeader(SmppConstants.DEST_ADDR_NPI, java.lang.Byte.class));
        } else {
            dataSm.setDestAddrNpi(config.getDestAddrNpi());
        }

        if (in.getHeaders().containsKey(SmppConstants.SOURCE_ADDR)) {
            dataSm.setSourceAddr(in.getHeader(SmppConstants.SOURCE_ADDR, String.class));
        } else {
            dataSm.setSourceAddr(config.getSourceAddr());
        }

        if (in.getHeaders().containsKey(SmppConstants.SOURCE_ADDR_TON)) {
            dataSm.setSourceAddrTon(in.getHeader(SmppConstants.SOURCE_ADDR_TON, java.lang.Byte.class));
        } else {
            dataSm.setSourceAddrTon(config.getSourceAddrTon());
        }

        if (in.getHeaders().containsKey(SmppConstants.SOURCE_ADDR_NPI)) {
            dataSm.setSourceAddrNpi(in.getHeader(SmppConstants.SOURCE_ADDR_NPI, java.lang.Byte.class));
        } else {
            dataSm.setSourceAddrNpi(config.getSourceAddrNpi());
        }

        if (in.getHeaders().containsKey(SmppConstants.SERVICE_TYPE)) {
            dataSm.setServiceType(in.getHeader(SmppConstants.SERVICE_TYPE, String.class));
        } else {
            dataSm.setServiceType(config.getServiceType());
        }

        if (in.getHeaders().containsKey(SmppConstants.REGISTERED_DELIVERY)) {
            dataSm.setRegisteredDelivery(in.getHeader(SmppConstants.REGISTERED_DELIVERY, java.lang.Byte.class));
        } else {
            dataSm.setRegisteredDelivery(config.getRegisteredDelivery());
        }

        Map<String, String> optinalParamaters = in.getHeader(SmppConstants.OPTIONAL_PARAMETERS, Map.class);
        if (optinalParamaters != null) {
            List<OptionalParameter> optParams = createOptionalParameters(optinalParamaters);
            dataSm.setOptionalParametes(optParams.toArray(new OptionalParameter[optParams.size()]));
        } else {
            dataSm.setOptionalParametes();
        }

        return dataSm;
    }
}