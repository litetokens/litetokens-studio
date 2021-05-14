package org.litetokens.studio.utils;

public interface Parameter {

    interface CommonConstant {
        byte ADD_PRE_FIX_BYTE_MAINNET = (byte) 0x30;   //30 + address
        byte ADD_PRE_FIX_BYTE_TESTNET = (byte) 0xa0;   //a0 + address
        int ADDRESS_SIZE = 21;
    }

}
