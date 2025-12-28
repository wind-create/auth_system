package com.core.auth.dto.me;

// package com.mestika.auth.dto.me;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SelfProfileUpdateRequest {

    @Size(max = 200)
    private String fullName;

    // nanti kalau mau tambah field lain (phone, avatar, dll) taruh di sini
}
