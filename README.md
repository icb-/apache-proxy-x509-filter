# Superseded

Don't use this filter. It has been folded into the IdP as of commit `81ef13f4d508a941641c5fc496e36756b949a02f`.

# Configuring IdP

The filter has been merged into mainline IdP, but still needs to be configured.

1. Edit your `web.xml` to apply the filter to `/authn/X509`:
```xml
<filter>
    <filter-name>X509ProxyFilter</filter-name>
    <filter-class>net.shibboleth.idp.authn.impl.X509ProxyFilter</filter-class>
    <!-- Proxied header name for the leaf certificate -->
    <init-param>
        <param-name>leafHeader</param-name>
        <param-value>SSL_CLIENT_CERT</param-value>
    </init-param>
    <!-- Space-separated list of proxied header names for chain certificates. -->
    <init-param>
        <param-name>chainHeaders</param-name>
        <param-value>SSL_CLIENT_CERT_CHAIN_0 SSL_CLIENT_CERT_CHAIN_1 SSL_CLIENT_CERT_CHAIN_2 SSL_CLIENT_CERT_CHAIN_3 SSL_CLIENT_CERT_CHAIN_4</param-value>
    </init-param>
</filter>
    <filter-mapping>
        <filter-name>X509ProxyFilter</filter-name>
        <url-pattern>/Authn/X509</url-pattern>
    </filter-mapping>
```
2. Rebuild your IdP WAR.
3. Restart your IdP process.

# Configure Apache

1. Put something like this inside the VHost:
```
    SSLCACertificateFile /path/to/your/root-cas.pem
    <Location /idp/Authn/X509>
        SSLVerifyClient require
        SSLVerifyDepth  5
        SSLOptions +ExportCertData
        RequestHeader set SSL_CLIENT_CERT            "%{SSL_CLIENT_CERT}s"
        RequestHeader set SSL_CLIENT_CERT_CHAIN_0    "%{SSL_CLIENT_CERT_CHAIN_0}s"
        RequestHeader set SSL_CLIENT_CERT_CHAIN_1    "%{SSL_CLIENT_CERT_CHAIN_1}s"
        RequestHeader set SSL_CLIENT_CERT_CHAIN_2    "%{SSL_CLIENT_CERT_CHAIN_2}s"
        RequestHeader set SSL_CLIENT_CERT_CHAIN_3    "%{SSL_CLIENT_CERT_CHAIN_3}s"
        RequestHeader set SSL_CLIENT_CERT_CHAIN_4    "%{SSL_CLIENT_CERT_CHAIN_4}s"
    </Location>
```
2. Reload

# Jumping from Password flow to X509 flow

1. Edit `conf/authn/authn-events-flow.xml`. Add:
```xml
    <end-state id="WantX509" />
    <global-transitions>
        <transition on="WantX509" to="WantX509" />
    </global-transitions>
```
2. Edit `conf/authn/mfa-authn-config.xml`. Change the transitions map to something like:
```xml
<util:map id="shibboleth.authn.MFA.TransitionMap">
    <entry key="">
        <bean parent="shibboleth.authn.MFA.Transition" p:nextFlow="authn/Password" />
    </entry>
    <entry key="authn/Password">
        <bean parent="shibboleth.authn.MFA.Transition">
            <property name="nextFlowStrategyMap">
                <map>
                    <!-- Maps event to a flow -->
                    <entry key="WantX509" value="authn/X509" />
                    <!-- Maps event to a scripted function bean reference-->
                    <entry key="proceed" value-ref="checkSecondFactor" />
                </map>
            </property>
        </bean>
    </entry>

    <!-- An implicit final rule will return whatever the second flow returns. -->
</util:map>
```
3. Edit `view/login.vm`. Add link/button to the WantX509 event, e.g.
```html
            <form action="$flowExecutionUrl" method="post">
                <button class="form-element form-button" type="submit" name="_eventId_WantX510">X509 Login</button>
            </form>
```
