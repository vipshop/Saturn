/**
 * Copyright 2016 vip.com.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 *  the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 * </p>
 **/

/**
 *
 */
package com.vip.saturn.job.console.response.converter;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonFormatVisitorWrapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.DefaultSerializerProvider;
import com.fasterxml.jackson.databind.ser.std.NonTypedScalarSerializerBase;
import java.io.IOException;
import java.lang.reflect.Type;

/**
 * 返回json中null值显示为空
 *
 * @author chembo.huang
 */

public class CustomNullValueMapper extends ObjectMapper {

	private static final long serialVersionUID = 1L;

	public CustomNullValueMapper() {
		DefaultSerializerProvider sp = new DefaultSerializerProvider.Impl();
		sp.setNullValueSerializer(new JsonSerializer<Object>() {

			@Override
			public void serialize(Object value, JsonGenerator jgen, SerializerProvider provider)
					throws IOException, JsonProcessingException {
				jgen.writeString("");
			}
		});
		setSerializerProvider(sp);
		SimpleModule module = new SimpleModule();
		module.addSerializer(String.class, new CustomStringSerializer());
		registerModule(module);
	}

	class CustomStringSerializer extends NonTypedScalarSerializerBase<String> {

		public CustomStringSerializer() {
			super(String.class);
		}

		@Override
		public boolean isEmpty(String value) {
			return (value == null) || (value.length() == 0);
		}

		@Override
		public void serialize(String value, JsonGenerator jgen, SerializerProvider provider) throws IOException {
			jgen.writeString(value == null ? "" : ("null".equals(value) ? "" : value));
		}

		@Override
		public JsonNode getSchema(SerializerProvider provider, Type typeHint) {
			return createSchemaNode("string", true);
		}

		@Override
		public void acceptJsonFormatVisitor(JsonFormatVisitorWrapper visitor, JavaType typeHint)
				throws JsonMappingException {
			if (visitor != null) {
				visitor.expectStringFormat(typeHint);
			}
		}
	}
}
