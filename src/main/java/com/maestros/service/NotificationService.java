package com.maestros.service;

import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.Notification;
import com.maestros.repository.sql.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final UserRepository userRepository;

    // -------------------------------------------------------------------------
    // Notification templates
    // -------------------------------------------------------------------------

    public enum Event {
        REQUEST_CREATED(
                "Nueva solicitud",
                "Tienes una nueva solicitud de {clientName}",
                "CREATED"),
        REQUEST_ACCEPTED(
                "¡Solicitud aceptada!",
                "{maestroName} aceptó tu solicitud",
                "ACCEPTED"),
        REQUEST_REJECTED(
                "Solicitud no aceptada",
                "{maestroName} no pudo tomar tu solicitud",
                "REJECTED"),
        WORK_STARTED(
                "El maestro está en camino",
                "{maestroName} ha iniciado tu trabajo",
                "STARTED"),
        WORK_COMPLETED(
                "Trabajo completado",
                "¡{maestroName} completó tu trabajo! ¿Cómo fue la experiencia?",
                "COMPLETED"),
        REQUEST_CANCELLED(
                "Solicitud cancelada",
                "{clientName} canceló la solicitud",
                "CANCELLED");

        public final String title;
        public final String bodyTemplate;
        public final String action;

        Event(String title, String bodyTemplate, String action) {
            this.title = title;
            this.bodyTemplate = bodyTemplate;
            this.action = action;
        }

        public String formatBody(String name) {
            return bodyTemplate
                    .replace("{clientName}", name)
                    .replace("{maestroName}", name);
        }
    }

    // -------------------------------------------------------------------------
    // Core send method
    // -------------------------------------------------------------------------

    @Transactional
    public void sendPushNotification(String fcmToken, String title, String body, Map<String, String> data) {
        if (FirebaseApp.getApps().isEmpty()) {
            log.debug("Firebase not initialized — skipping push notification: title=[{}]", title);
            return;
        }
        if (fcmToken == null || fcmToken.isBlank()) {
            log.warn("FCM token is null or empty — skipping push notification: title=[{}]", title);
            return;
        }

        Message message = Message.builder()
                .setNotification(Notification.builder()
                        .setTitle(title)
                        .setBody(body)
                        .build())
                .putAllData(data)
                .setToken(fcmToken)
                .build();

        try {
            String messageId = FirebaseMessaging.getInstance().send(message);
            log.debug("FCM message sent successfully: messageId=[{}]", messageId);
        } catch (FirebaseMessagingException e) {
            MessagingErrorCode code = e.getMessagingErrorCode();
            if (code == MessagingErrorCode.UNREGISTERED || code == MessagingErrorCode.INVALID_ARGUMENT) {
                log.info("FCM token is invalid or unregistered (code={}), clearing from DB", code);
                userRepository.clearFcmTokenByValue(fcmToken);
            } else {
                log.error("Failed to send FCM notification (code={}): {}", code, e.getMessage());
            }
            // Best-effort: never propagate — notifications must not break the main flow
        }
    }

    // -------------------------------------------------------------------------
    // Convenience helpers used by ServiceRequestService
    // -------------------------------------------------------------------------

    public void notifyRequestCreated(String maestroFcmToken, String clientName, String serviceRequestId) {
        Event ev = Event.REQUEST_CREATED;
        sendPushNotification(maestroFcmToken, ev.title, ev.formatBody(clientName),
                buildData(ev.action, serviceRequestId));
    }

    public void notifyRequestAccepted(String clientFcmToken, String maestroName, String serviceRequestId) {
        Event ev = Event.REQUEST_ACCEPTED;
        sendPushNotification(clientFcmToken, ev.title, ev.formatBody(maestroName),
                buildData(ev.action, serviceRequestId));
    }

    public void notifyRequestRejected(String clientFcmToken, String maestroName, String serviceRequestId) {
        Event ev = Event.REQUEST_REJECTED;
        sendPushNotification(clientFcmToken, ev.title, ev.formatBody(maestroName),
                buildData(ev.action, serviceRequestId));
    }

    public void notifyWorkStarted(String clientFcmToken, String maestroName, String serviceRequestId) {
        Event ev = Event.WORK_STARTED;
        sendPushNotification(clientFcmToken, ev.title, ev.formatBody(maestroName),
                buildData(ev.action, serviceRequestId));
    }

    public void notifyWorkCompleted(String clientFcmToken, String maestroName, String serviceRequestId) {
        Event ev = Event.WORK_COMPLETED;
        sendPushNotification(clientFcmToken, ev.title, ev.formatBody(maestroName),
                buildData(ev.action, serviceRequestId));
    }

    public void notifyRequestCancelled(String maestroFcmToken, String clientName, String serviceRequestId) {
        Event ev = Event.REQUEST_CANCELLED;
        sendPushNotification(maestroFcmToken, ev.title, ev.formatBody(clientName),
                buildData(ev.action, serviceRequestId));
    }

    private Map<String, String> buildData(String action, String serviceRequestId) {
        return Map.of(
                "type", "SERVICE_REQUEST",
                "serviceRequestId", serviceRequestId,
                "action", action);
    }
}
