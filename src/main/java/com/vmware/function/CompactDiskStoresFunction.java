package com.vmware.function;


import com.vmware.function.group.DiskStoreGroup;
import com.vmware.function.group.DiskStoreGroup.CompactType;
import com.vmware.function.exceptions.InvalidArgTypeSpecifiedException;
import com.vmware.function.exceptions.NoDiskStoreExistsException;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.geode.cache.CacheFactory;
import org.apache.geode.cache.asyncqueue.AsyncEventQueue;
import org.apache.geode.cache.execute.Function;
import org.apache.geode.cache.execute.FunctionContext;
import org.apache.geode.cache.wan.GatewaySender;
import org.apache.geode.internal.cache.InternalCache;
import org.apache.geode.internal.cache.InternalRegion;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Slf4j
public class CompactDiskStoresFunction implements Function<String> {

    private String getSource(List<DiskStoreGroup> groups, String diskStoreName) {
        for (DiskStoreGroup grp : groups) {
            if (grp.getDiskStoreName().equals(diskStoreName)) {
                return grp.getSourceName();
            }
        }
        return null;
    }

    private List<DiskStoreGroup> getSingleStore(InternalCache cache, String diskStoreName) throws NoDiskStoreExistsException {
        List<DiskStoreGroup> diskStoreGroups = new ArrayList<>();
        if (!cache.listDiskStores().contains(diskStoreName)) {
            throw new NoDiskStoreExistsException("Unable to compact disk store " + diskStoreName + " since it does not exist");
        }
        List<DiskStoreGroup> allDiskStoreGroups = getAll(cache);
        diskStoreGroups.add(new DiskStoreGroup(getSource(allDiskStoreGroups, diskStoreName), CompactType.STORE, diskStoreName));
        return diskStoreGroups;
    }

    private List<DiskStoreGroup> getGatewaySenders(InternalCache cache) {
        final Set<GatewaySender> gateways = cache.getGatewaySenders();
        List<DiskStoreGroup> diskStoreGroups = new ArrayList<>();
        for (GatewaySender sender : gateways) {
            diskStoreGroups.add(new DiskStoreGroup(sender.getId(), CompactType.GATEWAY, sender.getDiskStoreName()));
        }
        return diskStoreGroups;
    }

    private List<DiskStoreGroup> getAsyncQueues(InternalCache cache) {
        final Set<AsyncEventQueue> queues = cache.getAsyncEventQueues();
        List<DiskStoreGroup> diskStoreGroups = new ArrayList<>();
        for (AsyncEventQueue queue : queues) {
            diskStoreGroups.add(new DiskStoreGroup(queue.getId(), CompactType.QUEUE, queue.getDiskStoreName()));
        }
        return diskStoreGroups;
    }

    private List<DiskStoreGroup> getRegions(InternalCache cache) {
        final Set<InternalRegion> regions = cache.getApplicationRegions();
        List<DiskStoreGroup> diskStoreGroups = new ArrayList<>();
        for (InternalRegion region : regions) {
            diskStoreGroups.add(new DiskStoreGroup(region.getName(), CompactType.REGION, region.getDiskStoreName()));
        }
        return diskStoreGroups;
    }

    private List<DiskStoreGroup> getAll(InternalCache cache) {
        List<DiskStoreGroup> diskStoreNames = new ArrayList<>();
        diskStoreNames.addAll(getRegions(cache));
        diskStoreNames.addAll(getGatewaySenders(cache));
        diskStoreNames.addAll(getAsyncQueues(cache));
        return diskStoreNames;
    }

    private String processDiskStores(InternalCache cache, List<DiskStoreGroup> groups) {
        List<String> compactResponses = new ArrayList<>();
        StringBuilder sb = new StringBuilder().append("\n");
        for (DiskStoreGroup group : groups) {
            if (cache.findDiskStore(group.getDiskStoreName()).getAllowForceCompaction()) {
                log.info("Compacting disk store {} for type {} region {}", group.getDiskStoreName(), group.getType().toString(), group.getSourceName());
                try {
                    if (cache.findDiskStore(group.getDiskStoreName()).forceCompaction()) {
                        log.info("Disk store {} compacted", group.getDiskStoreName());
                        sb.append("disk store: ")
                                .append(group.getDiskStoreName())
                                .append(" type: ")
                                .append(group.getType().toString())
                                .append(" source: ")
                                .append(group.getSourceName())
                                .append(" compaction successful").append("\n");
                    } else {
                        log.error("Disk store {} compaction failed", group.getDiskStoreName());
                        sb.append("disk store: ")
                                .append(group.getDiskStoreName())
                                .append(" type: ")
                                .append(group.getType().toString())
                                .append(" source: ")
                                .append(group.getSourceName())
                                .append(" compaction failed").append("\n");
                    }
                } catch (Exception ex) {
                    log.error("Disk store {} compaction failed", group.getDiskStoreName(), ex);
                    sb.append("disk store: ")
                            .append(group.getDiskStoreName())
                            .append(" type: ")
                            .append(group.getType().toString())
                            .append(" source: ")
                            .append(group.getSourceName())
                            .append(" exception: ")
                            .append(ex.getMessage())
                            .append(" compaction failed exception message: ").append("\n");
                }
            } else {
                log.warn("Disk store {} cannot be compacted - allow-forced-compaction is false", group.getDiskStoreName());
                sb.append("disk store: ")
                        .append(group.getDiskStoreName())
                        .append(" type: ")
                        .append(group.getType().toString())
                        .append(" source: ")
                        .append(group.getSourceName())
                        .append(" cannot be compacted - allow-forced-compaction is false").append("\n");

            }
        }
        return sb.toString();
    }

    private String compactDiskStores(CompactType type, String diskStoreName) throws NoDiskStoreExistsException, InvalidArgTypeSpecifiedException {
        List<DiskStoreGroup> diskStoreGroups = new ArrayList<>();
        InternalCache cache = (InternalCache) CacheFactory.getAnyInstance();

        switch (type) {
            case STORE:
                return processDiskStores(cache, getSingleStore(cache, diskStoreName));
            case QUEUE:
                return processDiskStores(cache, getAsyncQueues(cache));
            case GATEWAY:
                return processDiskStores(cache, getGatewaySenders(cache));
            case REGION:
                return processDiskStores(cache, getRegions(cache));
            case ALL:
                return processDiskStores(cache, getAll(cache));
            default:
                throw new InvalidArgTypeSpecifiedException("Compaction request type was not specified");
        }
    }

    @SneakyThrows
    @Override
    public void execute(FunctionContext functionContext) {
        String[] functionArgs = (String[]) functionContext.getArguments();
        String response = null;
        if (functionArgs != null && functionArgs.length > 0) {
            functionArgs[0] = functionArgs[0].toUpperCase();
            if (functionArgs.length == 2 && !functionArgs[1].isEmpty()) {
                if (functionArgs[1].equals(CompactType.STORE.toString())) {
                    log.info("Compaction request for store {}", functionArgs[1]);
                    response = compactDiskStores(CompactType.valueOf(functionArgs[0]), functionArgs[1]);
                }
            } else {
                log.info("Compaction request for {}", functionArgs[0]);
                response = compactDiskStores(CompactType.valueOf(functionArgs[0]), null);
            }
        } else {
            throw new InvalidArgTypeSpecifiedException("Compaction request type was not specified");
        }
    }

    @Override
    public boolean hasResult() {
        return Boolean.TRUE;
    }

    @Override
    public String getId() {
        return this.getClass().getSimpleName();
    }

    @Override
    public boolean optimizeForWrite() {
        return Boolean.FALSE;
    }

    @Override
    public boolean isHA() {
        return Boolean.FALSE;
    }
}
