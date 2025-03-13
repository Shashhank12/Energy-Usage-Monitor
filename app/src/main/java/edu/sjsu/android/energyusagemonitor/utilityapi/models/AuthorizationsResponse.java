package edu.sjsu.android.energyusagemonitor.utilityapi.models;

import java.util.List;

public class AuthorizationsResponse {
    private List<Authorization> authorizations;

    public List<Authorization> getAuthorizations() {
        return authorizations;
    }

    public static class Authorization {
        private Meters meters;

        public Meters getMeters() {
            return meters;
        }

        public static class Meters {
            private List<Meter> meters;

            public List<Meter> getMeters() {
                return meters;
            }

            public static class Meter {
                private String uid;

                public String getUid() {
                    return uid;
                }
            }
        }
    }
}