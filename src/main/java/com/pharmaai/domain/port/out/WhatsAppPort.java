package com.pharmaai.domain.port.out;

public interface WhatsAppPort {

    void sendMessage(String to, String text);
}
