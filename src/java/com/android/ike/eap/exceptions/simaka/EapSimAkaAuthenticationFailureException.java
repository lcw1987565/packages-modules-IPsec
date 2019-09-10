/*
 * Copyright (C) 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.ike.eap.exceptions.simaka;

/**
 * EapSimAkaAuthenticationFailureException is thrown when an invalid Uicc Challenge is processed
 * during an EAP-SIM or EAP-AKA session.
 */
public class EapSimAkaAuthenticationFailureException extends Exception {
    /**
     * Construct an instance of EapSimAkaAuthenticationFailureException with the specified detail
     * message.
     *
     * @param message the detail message.
     */
    public EapSimAkaAuthenticationFailureException(String message) {
        super(message);
    }

    /**
     * Construct an instance of EapSimAkaAuthenticationFailureException with the specified message
     * and cause.
     *
     * @param message the detail message.
     * @param cause the cause.
     */
    public EapSimAkaAuthenticationFailureException(String message, Throwable cause) {
        super(message, cause);
    }
}