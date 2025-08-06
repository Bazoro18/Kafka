package main.java.com.hospital.model;
public class Vitals {
    private String patientId;
    private String timestamp;
    private double heartRate;
    private double spo2;
    private double temperature;
    private double bloodPressure;

    public Vitals(){}

    public Vitals(String patientId, String timestamp, double heartRate, double spo2, double temperature, double bloodPressure) {
        this.patientId = patientId;
        this.timestamp = timestamp;
        this.heartRate = heartRate;
        this.spo2 = spo2;
        this.temperature = temperature;
        this.bloodPressure = bloodPressure;
    }
    public String getPatientId() {
        return patientId;
    }
    public void setPatientId(String patientId) {
        this.patientId = patientId;
    }
    public String getTimestamp() {
        return timestamp;
    }
    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }
    public double getHeartRate() {
        return heartRate;
    }
    public void setHeartRate(double heartRate) {
        this.heartRate = heartRate;
    }
    public double getSpo2() {
        return spo2;
    }
    public void setSpo2(double spo2) {
        this.spo2 = spo2;
    }
    public double getTemperature() {
        return temperature;
    }
    public void setTemperature(double temperature) {
        this.temperature = temperature; 
    }
    public double getBloodPressure() {
        return bloodPressure;
    }
    public void setBloodPressure(double bloodPressure) {
        this.bloodPressure = bloodPressure;
    }
    @Override
    public String toString() {
        return String.format("Vitals [patientId=%s, timestamp=%s, heartRate=%.2f, spo2=%.2f, temperature=%.2f, bloodPressure=%.2f]",
                patientId, timestamp, heartRate, spo2, temperature, bloodPressure);
    }

}
