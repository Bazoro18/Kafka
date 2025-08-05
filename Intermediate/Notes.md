# Kafka Intermediate

## 1. Consumer Pull Loop

### Core Concept
Consumer actively **pulls** messages from broker (not pushed). Single-threaded event loop that continuously fetches batches of records.

### How It Works Internally
```
while (true) {
    ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));
    for (ConsumerRecord<String, String> record : records) {
        // Process record
    }
}
```

**Analogy**: Like checking your mailbox every morning - you decide when to check, not the postman deciding when to deliver.

### Key Function: `poll(timeout)`
- **timeout**: Max time to block if no data available
- Returns batch of records (not single record)
- Handles heartbeat, rebalancing, offset commits internally
- **Best Practice**: Use timeout 100-1000ms for production

### Uber Example
Uber's trip completion service polls driver location updates every 500ms to track real-time position and calculate fare.

---

## 2. Offset Commit Strategies

### Auto Commit (Default)
- **Property**: `enable.auto.commit=true`
- **Interval**: `auto.commit.interval.ms=5000` (5 sec default)
- Commits offset automatically during `poll()`

**Risk**: May lose messages if consumer crashes between processing and auto-commit.

### Manual Commit
```java
// Synchronous
consumer.commitSync();

// Asynchronous  
consumer.commitAsync();
```

### Strategies Comparison
| Strategy | Use Case | Risk |
|----------|----------|------|
| Auto | High throughput, can tolerate duplicates | Message loss |
| Manual Sync | Exactly-once semantics needed | Lower throughput |
| Manual Async | Balance between throughput & reliability | Callback complexity |

### Uber Example
Uber's payment service uses **manual sync commits** for processing ride payments - can't afford to lose/duplicate payment records.

---

## 3. Consumer Groups

### Core Concept
Multiple consumers working together as a **team** to consume from same topic. Each partition assigned to exactly **one consumer** in group.

### How It Works Internally
- **Group Coordinator**: Special broker manages group membership
- **Group ID**: `group.id` property identifies the team
- **Partition Assignment**: Round-robin or range strategy
- **Max Parallelism**: Number of partitions = Max consumers in group

### Key Properties
```properties
group.id=ride-processing-group
session.timeout.ms=30000
heartbeat.interval.ms=3000
```

### Uber Example
Uber has 3 consumers in "trip-completion-group" processing from 6 partitions of trip-events topic. Each consumer handles 2 partitions.

---

## 4. Rebalance

### What Is Rebalance?
Process of **redistributing** partitions among consumers when group membership changes.

### Triggers
1. Consumer joins group
2. Consumer leaves/crashes  
3. Consumer stops sending heartbeats
4. New partitions added to topic

### Rebalance Process
1. **Stop the World**: All consumers stop processing
2. **Revoke**: Current partition assignments revoked
3. **Reassign**: New assignments calculated
4. **Resume**: Consumers start processing new partitions

### Rebalance Listeners
```java
consumer.subscribe(topics, new ConsumerRebalanceListener() {
    public void onPartitionsRevoked(Collection<TopicPartition> partitions) {
        // Commit offsets, cleanup resources
    }
    
    public void onPartitionsAssigned(Collection<TopicPartition> partitions) {
        // Initialize, seek to specific offset
    }
});
```

### Best Practices
- Keep `session.timeout.ms` high (30s)
- Keep `heartbeat.interval.ms` low (3s)  
- Process messages quickly to avoid timeout

### Uber Example
When Uber scales ride-completion service from 3 to 6 consumers during peak hours, rebalance redistributes 6 partitions equally (1 per consumer).

---

## 5. Idempotent Producers

### Problem Solved
Prevents **duplicate messages** when producer retries due to network failures.

### How It Works Internally
- **Producer ID (PID)**: Unique ID assigned by broker
- **Sequence Number**: Incremental number per partition
- **Deduplication**: Broker detects duplicates using PID + Sequence

### Configuration
```properties
enable.idempotence=true
retries=Integer.MAX_VALUE
max.in.flight.requests.per.connection=5
acks=all
```

### Internal Flow
```
Producer → (PID=123, Seq=1, Message) → Broker
Network Failure → Producer Retries
Producer → (PID=123, Seq=1, Message) → Broker (DUPLICATE DETECTED)
Broker → Acknowledges without storing duplicate
```

### Uber Example
Uber's driver location service uses idempotent producers to ensure location updates aren't duplicated even with network retries.

---

## 6. Transactional Producers

### Core Concept
**ACID transactions** across multiple partitions/topics. Either all messages commit or none.

### Configuration
```properties
transactional.id=unique-transaction-id-per-producer
enable.idempotence=true
```

### Transaction Flow
```java
producer.initTransactions();
try {
    producer.beginTransaction();
    producer.send(record1);
    producer.send(record2);
    producer.commitTransaction();
} catch (Exception e) {
    producer.abortTransaction();
}
```

### Internal Working
- **Transaction Coordinator**: Special broker manages transactions
- **Transaction Log**: Internal topic `__transaction_state`
- **2-Phase Commit**: Prepare → Commit phases
- **Transaction Markers**: Special control messages mark transaction boundaries

### Uber Example
Uber's billing service uses transactions to ensure ride completion event and payment charge happen atomically - both succeed or both fail.

---

## 7. Read Committed Isolation Level

### Purpose
Consumers only read **committed messages** when transactions are used.

### Configuration
```properties
isolation.level=read_committed  # Default: read_uncommitted
```

### How It Works
- **LSO (Last Stable Offset)**: Highest offset of committed transaction
- **Aborted Messages**: Filtered out by consumer
- **Control Messages**: Transaction markers not returned to application

### Behavior Comparison
| Isolation Level | Reads Uncommitted? | Reads Aborted? |
|----------------|-------------------|----------------|
| read_uncommitted | ✅ | ✅ |
| read_committed | ❌ | ❌ |

### Uber Example
Uber's analytics service uses `read_committed` to ensure financial reports only include completed (committed) transactions, not failed/aborted ones.

---

## Real-World Analogy: Bus Queue System

**Scenario**: People waiting for buses at a busy transit station

### Components Mapping
- **People in Queue** = Producers (generating ride requests)
- **Bus** = Consumer (processing ride requests)  
- **Bus Driver** = Consumer Group Coordinator
- **Bus Route Schedule** = Topic Partitions
- **Queue Position Tickets** = Offsets
- **Station Manager** = Broker

### How It Works

**Consumer Pull Loop**: Bus arrives every 5 minutes (poll interval) and picks up passengers in batches, not one by one.

**Offset Commits**: 
- Auto: Bus driver automatically marks position every 5 stops
- Manual: Driver consciously marks position after safely dropping passengers

**Consumer Groups**: Multiple buses (consumers) on same route (topic) pick up different sections of queue (partitions). Route-7 has 3 buses handling 6 bus stops.

**Rebalance**: When new bus joins Route-7, all buses stop, station manager redistributes bus stops among all buses, then they resume service.

**Idempotent Producers**: People have unique tickets preventing them from accidentally joining queue twice even if ticket machine glitches.

**Transactional Producers**: VIP passengers travel in groups - either entire group gets on bus or none do (no partial groups).

**Read Committed**: Regular passengers only see completed group boardings, not VIP groups still in process of boarding.

### Production Best Practices Summary

1. **Poll Timeout**: 100-1000ms for production workloads
2. **Commit Strategy**: Manual sync for critical data, auto for high throughput
3. **Consumer Groups**: Size = number of partitions for max parallelism  
4. **Rebalance**: Minimize by keeping processing fast and timeouts appropriate
5. **Idempotence**: Always enable for exactly-once semantics
6. **Transactions**: Use for multi-partition atomic operations
7. **Isolation**: read_committed for transactional workflows

**Key Functions to Remember**: `poll()`, `commitSync()`, `commitAsync()`, `beginTransaction()`, `commitTransaction()`
