package com.is.openbankingclientapi.model.client;

import lombok.Getter;
import lombok.Setter;

// 3.1 (/clients/info) so'rovining payload qismi
@Getter
@Setter
public class ClientPayload {
    // JSHSHIR — jismoniy shaxsning 14 xonali shaxsiy identifikatsiya raqami
    private String pinfl;
}
