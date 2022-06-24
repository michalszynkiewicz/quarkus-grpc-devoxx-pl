package org.acme.dto;

public class JoinResponseDto {
    public Result result;
    public String user;

    public enum Result {
        NAME_ALREADY_USED,
        OKAY
    }
}
