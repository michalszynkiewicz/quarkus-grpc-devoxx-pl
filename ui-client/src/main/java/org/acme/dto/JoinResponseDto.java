package org.acme.dto;

public class JoinResponseDto {
    public String token;
    public Result result;
    public enum Result {
        NAME_ALREADY_USED,
        OKAY
    }
}
