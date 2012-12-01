/*
 * Copyright (C) 2011 Benoit GUEROUT <bguerout at gmail dot com> and Yves AMSELLEM <amsellem dot yves at gmail dot com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jongo.compatibility;

import java.io.StringWriter;
import java.io.Writer;

import org.jongo.bson.Bson;
import org.jongo.bson.BsonDocument;
import org.jongo.marshall.Marshaller;
import org.jongo.marshall.MarshallingException;
import org.jongo.marshall.Unmarshaller;
import org.jongo.marshall.jackson.JacksonMapper;
import org.jongo.marshall.jackson.configuration.DeserializationModifier;
import org.jongo.marshall.jackson.configuration.Mapping;
import org.jongo.marshall.jackson.configuration.SerializationModifier;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.DBObject;
import com.mongodb.util.JSON;

class JsonEngine implements Unmarshaller, Marshaller {

    private final Mapping config;

    public static Mapping createConfig() {
        return new JacksonMapper.Builder(new ObjectMapper())
                .addModule(new JsonModule())
                .addModifier(new SerializationModifier())
                .addModifier(new DeserializationModifier())
                .innerConfig();
    }

    public JsonEngine(Mapping config) {
        this.config = config;
    }

    public <T> T unmarshall(BsonDocument document, Class<T> clazz) throws MarshallingException {
        String json = document.toDBObject().toString();
        try {
            return (T) config.getReader(clazz).readValue(json);
        } catch (Exception e) {
            String message = String.format("Unable to unmarshall from json: %s to %s", json, clazz);
            throw new MarshallingException(message, e);
        }
    }

    public BsonDocument marshall(Object obj) throws MarshallingException {
        try {
            Writer writer = new StringWriter();
            config.getWriter(obj.getClass()).writeValue(writer, obj);
            DBObject dbObject = createDBObjectWithDriver(writer.toString());
            return Bson.createDocument(dbObject);
        } catch (Exception e) {
            String message = String.format("Unable to marshall json from: %s", obj);
            throw new MarshallingException(message, e);
        }
    }

    protected DBObject createDBObjectWithDriver(String json) {
        return (DBObject) JSON.parse(json);
    }
}
