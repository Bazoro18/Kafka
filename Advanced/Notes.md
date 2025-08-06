# Kafka Storage Engine & Log Compaction

## 1. Log Segment Layout

### Core Concept
Kafka stores messages in **immutable log segments** on disk. Each partition is a directory containing multiple segment files.

### Physical Structure
```
/kafka-logs/topic-partition-0/
├── 00000000000000000000.log    # Segment file (messages)
├── 00000000000000000000.index  # Offset index
├── 00000000000000000000.timeindex # Time index
├── 00000000000000001000.log    # Next segment
├── 00000000000000001000.index
└── leader-epoch-checkpoint     # Leader epoch info
```

### How It Works Internally
- **Segment Naming**: Uses base offset (first message offset in segment)
- **Active Segment**: Only the latest segment accepts writes
- **Immutable Segments**: Older segments are read-only
- **Sequential Writes**: Messages appended sequentially for optimal disk I/O

**Analogy**: Like a library with numbered volumes - once Volume 1 is full, you start Volume 2. You can only write in the current volume but read from any.

### Key Properties
```properties
log.segment.bytes=1073741824        # 1GB default segment size
log.roll.hours=168                  # 7 days segment roll time
log.segment.delete.delay.ms=60000   # 1 minute deletion delay
```

### Uber Example
Uber's driver location topic has segments rolling every 1GB. Each segment contains ~1 million location updates before rolling to next segment.

---

## 2. Index Files

### Types of Indexes
1. **Offset Index (.index)**: Maps logical offset → physical position
2. **Time Index (.timeindex)**: Maps timestamp → offset
3. **Transaction Index (.txnindex)**: Maps producer transactions

### Offset Index Structure
```
Offset Index (.index):
[Relative Offset] → [Physical Position in .log file]
0 → 0
100 → 15234
200 → 30987
```

### How It Works Internally
- **Sparse Index**: Not every message indexed (every 4KB by default)
- **Binary Search**: Fast offset lookup using binary search
- **Relative Offsets**: Saves space by storing relative to base offset
- **Memory Mapped**: Indexes loaded in memory for fast access

### Key Properties
```properties
log.index.interval.bytes=4096       # Index entry every 4KB
log.index.size.max.bytes=10485760   # 10MB max index size
```

### Index Lookup Process
```
1. Binary search in offset index for target offset
2. Find closest entry ≤ target offset  
3. Sequential scan from that position in .log file
4. Return exact message
```

### Uber Example
When Uber's analytics service queries driver location at offset 150, Kafka uses binary search in index to find entry "100 → 15234", then scans .log file from position 15234.

---

## 3. Segment Rolling

### What Is Segment Rolling?
Process of **closing current active segment** and creating new one when certain conditions met.

### Rolling Triggers
1. **Size Limit**: `log.segment.bytes` exceeded
2. **Time Limit**: `log.roll.hours` elapsed  
3. **Index Full**: `log.index.size.max.bytes` reached
4. **Manual**: Admin command

### Rolling Process
```
1. Close current active segment (make read-only)
2. Flush remaining data to disk
3. Create new segment with next base offset
4. Update partition metadata
5. Start accepting writes to new segment
```

### Internal Mechanics
- **Atomic Operation**: Rolling is atomic - no message loss
- **Background Process**: Log cleaner handles rolling
- **Offset Calculation**: New segment base offset = last offset + 1

### Best Practices
- **Size-based rolling**: Better for uniform message sizes
- **Time-based rolling**: Better for variable throughput
- **Production typical**: 1GB segments, 7-day retention

### Uber Example
Uber's trip events topic rolls segments every 1GB. During peak hours (1M trips/hour), segments roll every 30 minutes. During low traffic, time-based rolling kicks in after 7 days.

---

## 4. Compaction vs Deletion

### Deletion (Default)
**Time/Size-based deletion** of entire old segments.

```properties
log.cleanup.policy=delete
log.retention.hours=168           # 7 days
log.retention.bytes=1073741824    # 1GB
```

**How it works**: 
1. Segments older than retention period marked for deletion
2. Background cleaner deletes entire segments
3. **All messages in segment deleted** regardless of key

### Compaction 
**Key-based compaction** keeps only latest value per key.

```properties
log.cleanup.policy=compact
log.cleaner.min.compaction.lag.ms=0
log.segment.ms=604800000          # 7 days
```

**How it works**:
1. **Head**: Active segment + recent segments (not compacted)
2. **Tail**: Older segments eligible for compaction  
3. **Cleaner**: Background thread removes duplicate keys
4. **Tombstones**: null values eventually deleted

### Compaction Process
```
Before: [k1:v1] [k2:v2] [k1:v3] [k3:v4] [k1:null]
After:  [k2:v2] [k3:v4] [k1:null(tombstone)]
```

### When to Use Which?
| Use Case | Strategy | Example |
|----------|----------|---------|
| Event logs | Deletion | User clickstreams |
| Configuration | Compaction | Database config updates |
| State snapshots | Compaction | User profiles |

### Uber Example
- **Trip events**: Deletion (don't need old trip history forever)
- **Driver profiles**: Compaction (always need latest driver info)

---

## 5. Leader Election

### Core Concept
Each partition has **one leader** handling all reads/writes and multiple **followers** replicating data.

### ISR (In-Sync Replica Set)
- **Leader**: Current partition leader
- **ISR**: Followers that are "caught up" with leader
- **Lag Threshold**: `replica.lag.time.max.ms=30000` (30 sec)

### Election Process
1. **Controller**: One broker acts as cluster controller
2. **ZooKeeper Watch**: Controller watches for broker failures
3. **ISR Selection**: New leader chosen from ISR list
4. **Metadata Update**: New leader info propagated to all brokers

### Leader Election Triggers
- Current leader broker fails/crashes
- Current leader removed from ISR (too slow)
- Controlled shutdown (maintenance)
- Partition reassignment

### Election Algorithm
```
1. Controller detects leader failure
2. Select new leader from ISR (first available)
3. Update partition metadata in ZooKeeper
4. Send LeaderAndIsr request to all brokers
5. Clients discover new leader via metadata refresh
```

### Key Properties
```properties
replica.lag.time.max.ms=30000           # ISR lag threshold
controller.socket.timeout.ms=30000       # Controller timeout
leader.imbalance.check.interval.seconds=300  # Balance check interval
```

### Uber Example
Uber's payment topic has 3 replicas across different availability zones. When leader in Zone-A fails, controller elects replica from Zone-B as new leader within 5 seconds.

---

## 6. Failover

### What Is Failover?
Process of **switching traffic** from failed leader to new leader with minimal disruption.

### Failover Types

#### 1. Clean Failover (Controlled)
- Leader shuts down gracefully
- Transfers leadership before stopping
- **Zero message loss**

#### 2. Unclean Failover  
- Leader crashes unexpectedly
- **Potential message loss** if followers lag behind
- Controlled by `unclean.leader.election.enable=false`

### Failover Process
```
1. Broker failure detected (heartbeat timeout)
2. Controller removes failed broker from ISR
3. New leader elected from remaining ISR
4. Clients get metadata update
5. Traffic switches to new leader
6. Failed broker marked as out-of-sync
```

### Recovery Scenarios

#### Clean Recovery
```
1. Failed broker restarts
2. Catches up by replicating from leader  
3. Rejoins ISR when caught up
4. Available for future elections
```

#### Unclean Recovery
```properties
unclean.leader.election.enable=true   # Allow out-of-sync replicas
```
**Risk**: Potential data loss but higher availability

### Failover Timeline
- **Detection**: 6-30 seconds (heartbeat timeout)
- **Election**: 1-5 seconds  
- **Propagation**: 2-10 seconds
- **Total**: 10-45 seconds typical

### Best Practices
1. **Multiple AZs**: Spread replicas across availability zones
2. **Min ISR**: `min.insync.replicas=2` for durability
3. **Monitoring**: Alert on ISR shrinkage
4. **Clean shutdown**: Use controlled shutdown for maintenance

### Uber Example
During AWS Zone failure, Uber's core services failover in 15 seconds:
- Payment topic: Zone-A leader → Zone-C leader  
- Trip events: Automatic failover with zero message loss
- Driver locations: Brief 10-second interruption during election

---

## Real-World Analogy: Corporate Office Building

**Scenario**: Multi-floor office building with departments and backup systems

### Components Mapping
- **Office Building** = Kafka Broker
- **Floor** = Topic Partition  
- **Filing Cabinets** = Log Segments
- **Cabinet Drawers** = Individual Messages
- **Index Cards** = Index Files
- **Department Head** = Partition Leader
- **Assistant Managers** = Replica Followers
- **Building Manager** = Controller
- **Fire/Evacuation Plan** = Failover Strategy

### How It Works

**Log Segment Layout**: Each floor has multiple filing cabinets (segments). When Cabinet-1 gets full, start using Cabinet-2. Each cabinet has numbered drawers (offsets) containing documents (messages).

**Index Files**: Each cabinet has index cards showing "Drawer 100 → Shelf Location B-15" for quick document lookup without opening every drawer.

**Segment Rolling**: When filing cabinet gets full (1000 documents) or end of month, seal it and start new cabinet. Old cabinets become archive (read-only).

**Compaction vs Deletion**: 
- **Deletion**: Throw away entire old cabinets after 7 years
- **Compaction**: Keep only latest version of each employee record, remove duplicates

**Leader Election**: Department head (leader) handles all client meetings. If head is sick, assistant manager (ISR replica) becomes acting head. Building manager coordinates the transition.

**Failover**: When Department Head suddenly leaves, Building Manager quickly promotes best Assistant Manager. Brief confusion for 10 minutes while new business cards are printed and clients informed.

### Production Best Practices Summary

1. **Segment Size**: 1GB for balanced I/O performance
2. **Index Interval**: 4KB for good seek performance  
3. **Rolling Strategy**: Size-based for uniform loads, time-based for variable
4. **Cleanup Policy**: Deletion for events, compaction for state
5. **Replication Factor**: 3 replicas across different AZs
6. **Min ISR**: 2 for durability vs availability balance
7. **Unclean Election**: Disabled for critical topics
8. **Monitoring**: ISR size, leader election rate, lag metrics

**Key Functions to Remember**: `log.segment.bytes`, `log.cleanup.policy`, `min.insync.replicas`, `unclean.leader.election.enable`

**Critical Monitoring Metrics**:
- Under-replicated partitions
- ISR shrink/expand rate  
- Leader election rate
- Log size growth
- Compaction lag