package com.vmware.function.group;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class DiskStoreGroup {
    public static enum CompactType {ALL, QUEUE, REGION, GATEWAY, STORE};
    private String sourceName;
    private CompactType type;
    private String diskStoreName;
}
