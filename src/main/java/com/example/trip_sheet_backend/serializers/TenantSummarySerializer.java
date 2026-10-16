package com.example.trip_sheet_backend.serializers;

import java.io.IOException;

import com.example.trip_sheet_backend.models.Tenant;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

public class TenantSummarySerializer extends StdSerializer<Tenant> {

  public TenantSummarySerializer() {
    super(Tenant.class);
  }

  @Override
  public void serialize(Tenant tenant, JsonGenerator generator, SerializerProvider provider)
      throws IOException {
    if (tenant == null) {
      generator.writeNull();
      return;
    }

    generator.writeStartObject();
    if (tenant.getId() != null) {
      generator.writeStringField("id", tenant.getId().toString());
    }
    generator.writeStringField("tenantName", tenant.getTenantName());
    generator.writeEndObject();
  }
}
