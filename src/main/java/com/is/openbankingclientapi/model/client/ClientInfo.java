package com.is.openbankingclientapi.model.client;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ClientInfo {
    private String requestId;
    private String mfo;
    private String pinfl;
    private String documentId;
    private String phoneNumber;
    private String bankClientNumber;
    private String firstName;
    private String lastName;
    private String patronymic;
    private String birthDate;
}
