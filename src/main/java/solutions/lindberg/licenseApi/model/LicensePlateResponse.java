package solutions.lindberg.licenseApi.model;

import java.util.Collections;
import java.util.List;

public class LicensePlateResponse {
    private String originalUri;
    private List<LicensePlate> licensePlates = Collections.emptyList();

    public List<LicensePlate> getLicensePlates() {
        return licensePlates;
    }

    public void setLicensePlates(List<LicensePlate> licensePlates) {
        this.licensePlates = licensePlates;
    }

    public String getOriginalUri() {
        return originalUri;
    }

    public void setOriginalUri(String originalUri) {
        this.originalUri = originalUri;
    }
}
