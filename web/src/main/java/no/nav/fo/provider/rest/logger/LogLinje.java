package no.nav.fo.provider.rest.logger;

public class LogLinje {
    public String level;
    public String message;
    public String url;
    public String jsFileUrl;
    public String userAgent;
    public int lineNumber;
    public int columnNumber;

    @Override
    public String toString() {
        return message + " [url='"+url+"', jsFile='"+jsFileUrl+"', line='"+lineNumber+"', column='"+columnNumber+"', userAgent='"+userAgent+"']";
    }
}
