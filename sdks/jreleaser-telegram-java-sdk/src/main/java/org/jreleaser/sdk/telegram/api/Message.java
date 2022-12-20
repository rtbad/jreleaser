/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2020-2022 The JReleaser authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jreleaser.sdk.telegram.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * @author Andres Almiray
 * @since 0.8.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Message {
    private String chat_id;
    private String text;

    public String getChat_id() {
        return chat_id;
    }

    public void setChat_id(String chat_id) {
        this.chat_id = chat_id;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    @Override
    public String toString() {
        return "Message[" +
            "chatId='" + chat_id + "', " +
            "text='" + text + '\'' +
            "]";
    }

    public static Message of(String chatId, String text) {
        Message message = new Message();
        message.chat_id = chatId;
        message.text = text;
        return message;
    }
}
