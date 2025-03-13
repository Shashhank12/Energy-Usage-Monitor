package edu.sjsu.android.energyusagemonitor.utilityapi.models;

import java.util.List;

public class BillsResponse {
    private List<Bill> bills;

    public List<Bill> getBills() {
        return bills;
    }

    public static class Bill {
        private Base base;

        public Base getBase() {
            return base;
        }

        public static class Base {
            private double bill_total_volume;
            private String service_tariff;

            public double getBillTotalVolume() {
                return bill_total_volume;
            }

            public String getServiceTariff() {
                return service_tariff;
            }
        }
    }
}
