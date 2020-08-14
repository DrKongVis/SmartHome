package com.secretbase.Job;

public class Now_TH_Data {
    private int id;
    private float tempValue;
    private float humiValue;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public float getTempValue() {
        return tempValue;
    }

    public void setTempValue(float tempValue) {
        this.tempValue = tempValue;
    }

    public float getHumiValue() {
        return humiValue;
    }

    public void setHumiValue(float humiValue) {
        this.humiValue = humiValue;
    }

    @Override
    public String toString() {
        return "Now_TH_Data{" +
                "id=" + id +
                ", tempValue=" + tempValue +
                ", humiValue=" + humiValue +
                '}';
    }
}
