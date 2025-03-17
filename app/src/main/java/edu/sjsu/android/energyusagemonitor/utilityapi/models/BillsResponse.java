package edu.sjsu.android.energyusagemonitor.utilityapi.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class BillsResponse {
    @SerializedName("bills")
    private List<Bill> bills;

    public List<Bill> getBills() {
        return bills;
    }

    public static class Bill {
        @SerializedName("authorization_uid")
        private String authorizationUid;

        @SerializedName("base")
        private Base base;

        @SerializedName("blocks")
        private List<String> blocks;

        @SerializedName("created")
        private String created;

        @SerializedName("line_items")
        private List<LineItem> lineItems;

        @SerializedName("meter_uid")
        private String meterUid;

        @SerializedName("notes")
        private List<String> notes;

        @SerializedName("sources")
        private List<Source> sources;

        @SerializedName("tiers")
        private List<Tier> tiers;

        @SerializedName("uid")
        private String uid;

        @SerializedName("updated")
        private String updated;

        @SerializedName("utility")
        private String utility;

        public Base getBase() {
            return base;
        }

        public List<LineItem> getLineItems() {
            return lineItems;
        }
    }

    public static class Base {
        @SerializedName("bill_end_date")
        private String billEndDate;

        @SerializedName("bill_start_date")
        private String billStartDate;

        @SerializedName("bill_statement_date")
        private String billStatementDate;

        @SerializedName("bill_total_cost")
        private double billTotalCost;

        @SerializedName("bill_total_kwh")
        private double billTotalKwh;

        @SerializedName("bill_total_unit")
        private String billTotalUnit;

        @SerializedName("bill_total_volume")
        private double billTotalVolume;

        @SerializedName("billing_account")
        private String billingAccount;

        @SerializedName("billing_address")
        private String billingAddress;

        @SerializedName("billing_contact")
        private String billingContact;

        @SerializedName("meter_numbers")
        private List<String> meterNumbers;

        @SerializedName("service_address")
        private String serviceAddress;

        @SerializedName("service_class")
        private String serviceClass;

        @SerializedName("service_identifier")
        private String serviceIdentifier;

        @SerializedName("service_tariff")
        private String serviceTariff;

        public String getBillEndDate() {
            return billEndDate;
        }

        public String getBillStartDate() {
            return billStartDate;
        }

        public String getBillStatementDate() {
            return billStatementDate;
        }

        public double getBillTotalCost() {
            return billTotalCost;
        }

        public double getBillTotalKwh() {
            return billTotalKwh;
        }

        public String getBillTotalUnit() {
            return billTotalUnit;
        }

        public double getBillTotalVolume() {
            return billTotalVolume;
        }

        public String getBillingAccount() {
            return billingAccount;
        }

        public String getBillingAddress() {
            return billingAddress;
        }

        public String getBillingContact() {
            return billingContact;
        }

        public List<String> getMeterNumbers() {
            return meterNumbers;
        }

        public String getServiceAddress() {
            return serviceAddress;
        }

        public String getServiceClass() {
            return serviceClass;
        }

        public String getServiceIdentifier() {
            return serviceIdentifier;
        }

        public String getServiceTariff() {
            return serviceTariff;
        }
    }

    public static class LineItem {
        @SerializedName("cost")
        private double cost;

        @SerializedName("end")
        private String end;

        @SerializedName("kind")
        private String kind;

        @SerializedName("name")
        private String name;

        @SerializedName("rate")
        private Double rate;

        @SerializedName("start")
        private String start;

        @SerializedName("unit")
        private String unit;

        @SerializedName("volume")
        private Double volume;

        public double getCost() {
            return cost;
        }

        public String getEnd() {
            return end;
        }

        public String getKind() {
            return kind;
        }

        public String getName() {
            return name;
        }

        public Double getRate() {
            return rate;
        }

        public String getStart() {
            return start;
        }

        public String getUnit() {
            return unit;
        }

        public Double getVolume() {
            return volume;
        }
    }

    public static class Source {
        @SerializedName("raw_url")
        private String rawUrl;

        @SerializedName("type")
        private String type;

        public String getRawUrl() {
            return rawUrl;
        }

        public String getType() {
            return type;
        }
    }

    public static class Tier {
        @SerializedName("cost")
        private double cost;

        @SerializedName("level")
        private int level;

        @SerializedName("name")
        private String name;

        @SerializedName("unit")
        private String unit;

        @SerializedName("volume")
        private double volume;

        public double getCost() {
            return cost;
        }

        public int getLevel() {
            return level;
        }

        public String getName() {
            return name;
        }

        public String getUnit() {
            return unit;
        }

        public double getVolume() {
            return volume;
        }
    }
}