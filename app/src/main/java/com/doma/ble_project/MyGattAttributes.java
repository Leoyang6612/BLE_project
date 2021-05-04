package com.doma.ble_project;

import java.util.HashMap;

public class MyGattAttributes {
    private static HashMap<String, String> attributes = new HashMap();
    public static String GENERIC_ACESS = "00001800-0000-1000-8000-00805f9b34fb";
    public static String GENERIC_Attribute = "00001801-0000-1000-8000-00805f9b34fb";
    public static String Nordic_UART_UUID = "6e400001-b5a3-f393-e0a9-e50e24dcca9e";
    public static String Tx_CHARACTERISTIC = "6e400003-b5a3-f393-e0a9-e50e24dcca9e";
    public static String Rx_CHARACTERISTIC = "6e400002-b5a3-f393-e0a9-e50e24dcca9e";

    static {
        attributes.put(Nordic_UART_UUID, "Nordic_UART Service");
        attributes.put(GENERIC_ACESS, "Generic Access");
        attributes.put(GENERIC_Attribute, "Generic Attribute");
        attributes.put(Tx_CHARACTERISTIC, "Tx Characteristic\n (press here for notify on/off)");
        attributes.put(Rx_CHARACTERISTIC, "Rx Characteristic\n (press here to input data)");
    }

    public static String lookup(String uuid, String defaultName) {
        String name = attributes.get(uuid);
        return name == null ? defaultName : name;
    }
}
