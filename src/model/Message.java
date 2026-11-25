package model;

import java.io.Serializable;
import java.time.LocalDateTime;

/** A message sent between members, always about a specific listing. */
public record Message(
        String fromEmail,
        String fromName,
        String toEmail,
        String listingId,
        String listingTitle,
        String text,
        LocalDateTime sentAt) implements Serializable {
}
