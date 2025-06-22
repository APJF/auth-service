package fpt.sep.apjf.dto;

import java.util.List;

public record LoginResponse(String username, List<String> roles, String jwtToken) {
}
