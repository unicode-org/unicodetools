package org.unicode.xml;

import com.ibm.icu.util.VersionInfo;
import org.unicode.props.UcdProperty;

public class UcdSectionComponent {
    private final VersionInfo minVersion;
    private final VersionInfo maxVersion;
    private final UcdProperty ucdProperty;

    UcdSectionComponent(VersionInfo minVersion, VersionInfo maxVersion, UcdProperty ucdProperty) {
        this.minVersion = minVersion;
        this.maxVersion = maxVersion;
        this.ucdProperty = ucdProperty;
    }

    public VersionInfo getMinVersion() {
        return this.minVersion;
    }

    public VersionInfo getMaxVersion() {
        return this.maxVersion;
    }

    public UcdProperty getUcdProperty() {
        return this.ucdProperty;
    }
}
