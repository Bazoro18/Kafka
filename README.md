# 📚 Kafka Project-Based Learning Journey

This repository documents my hands-on journey to learn **Apache Kafka** through a structured, project-based approach.  
I explored Kafka concepts ranging from fundamentals to advanced internals — each reinforced by a real-world inspired project and detailed notes.

---

## 🗂 Structure

Each learning day focuses on:
1. **Topics & Subtopics** – Core Kafka concepts for the day.
2. **Difficulty Level** – Fundamentals, Intermediate, or Advanced.
3. **Hands-On Project** – A practical application of the day's concepts.
4. **Notes** – My personal takeaways and explanations.

---

## 📅 Learning Timeline

### **Kafka Fundamentals & Producer Internals**  
**Level:** Fundamentals  
**Topics:**
- Broker architecture
- Partitions
- Replication
- Producer batching
- Serializers
- linger.ms
- Retries
- acks

**Project:** **Movie Ticket-Booking**  
A Kafka producer simulates ticket booking events, showcasing producer configurations, batching, and delivery guarantees.

**Notes:** Covers basics of Kafka brokers, data distribution, and producer tuning parameters.

---

### **Kafka Consumer Internals & Exactly-Once Semantics**  
**Level:** Intermediate  
**Topics:**
- Consumer-pull loop
- Offset commit strategies (auto/manual)
- Consumer groups
- Rebalance listeners
- Idempotent & transactional producers
- read_committed isolation level

**Project:** **Ride-Booking Validation**  
Implements a consumer group that processes ride booking events, applies validations, and demonstrates exactly-once semantics using idempotent and transactional producers.

**Notes:** Deep dive into consumer architecture, offset handling, and avoiding duplicate processing.

---

### **Kafka Storage Engine & Log Compaction**  
**Level:** Advanced  
**Topics:**
- Log segment layout
- Index file
- Segment rolling
- Compaction vs deletion
- Leader election and failover

**Project:** **Patient Monitoring**  
Simulates continuous health data ingestion, leveraging log compaction to keep the latest patient state and exploring leader failover handling.

**Notes:** Insights into Kafka’s internal storage mechanics, high availability, and retention strategies.

---
