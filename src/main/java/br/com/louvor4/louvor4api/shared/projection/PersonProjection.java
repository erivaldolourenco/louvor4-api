package br.com.louvor4.louvor4api.shared.projection;

import java.util.Date;
import java.util.UUID;


public interface PersonProjection {

    public UUID getId();

    public String getEmail();

    public String getFirstName();

    public String getLastName();

    public Date getBirthday();
}
