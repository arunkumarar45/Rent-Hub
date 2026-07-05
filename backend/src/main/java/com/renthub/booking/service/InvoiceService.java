package com.renthub.booking.service;

import com.lowagie.text.Document;
import com.lowagie.text.Font;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import com.renthub.booking.model.entity.Booking;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
@Slf4j
public class InvoiceService {

    public String generateInvoicePdf(Booking booking) {
        try {
            Path dir = Paths.get("uploads").toAbsolutePath().normalize();
            Files.createDirectories(dir);
            String fileName = "invoice-" + booking.getId() + ".pdf";
            Path targetPath = dir.resolve(fileName);

            Document document = new Document();
            FileOutputStream fos = new FileOutputStream(targetPath.toFile());
            PdfWriter.getInstance(document, fos);

            document.open();
            
            Font titleFont = new Font(Font.HELVETICA, 20, Font.BOLD);
            Font sectionFont = new Font(Font.HELVETICA, 12, Font.BOLD);
            Font regularFont = new Font(Font.HELVETICA, 10, Font.NORMAL);

            document.add(new Paragraph("RENTHUB RENTAL MONOLITH INVOICE", titleFont));
            document.add(new Paragraph("----------------------------------------------------------------", regularFont));
            document.add(new Paragraph("Booking ID: " + booking.getId(), regularFont));
            document.add(new Paragraph("Created Date: " + booking.getCreatedAt(), regularFont));
            document.add(new Paragraph(" ", regularFont));

            document.add(new Paragraph("Equipment Details:", sectionFont));
            document.add(new Paragraph("Title: " + booking.getEquipment().getTitle(), regularFont));
            document.add(new Paragraph("Owner Name: " + booking.getEquipment().getOwner().getFirstName() + " " + booking.getEquipment().getOwner().getLastName(), regularFont));
            document.add(new Paragraph("Location: " + booking.getEquipment().getLocation(), regularFont));
            document.add(new Paragraph(" ", regularFont));

            document.add(new Paragraph("Renter Details:", sectionFont));
            document.add(new Paragraph("Name: " + booking.getCustomer().getFirstName() + " " + booking.getCustomer().getLastName(), regularFont));
            document.add(new Paragraph("Email: " + booking.getCustomer().getEmail(), regularFont));
            document.add(new Paragraph(" ", regularFont));

            document.add(new Paragraph("Rental Period:", sectionFont));
            document.add(new Paragraph("Start Date: " + booking.getStartDate(), regularFont));
            document.add(new Paragraph("End Date: " + booking.getEndDate(), regularFont));
            document.add(new Paragraph(" ", regularFont));

            document.add(new Paragraph("Price Summary:", sectionFont));
            document.add(new Paragraph(String.format("Daily Rate: $%.2f", booking.getDailyRate() / 100.0), regularFont));
            document.add(new Paragraph(String.format("Security Deposit: $%.2f", booking.getDeposit() / 100.0), regularFont));
            document.add(new Paragraph(String.format("Rental Total (after discounts): $%.2f", booking.getRentalPrice() / 100.0), regularFont));
            document.add(new Paragraph(String.format("Grand Total Paid: $%.2f", booking.getTotalPrice() / 100.0), regularFont));
            document.add(new Paragraph(" ", regularFont));

            document.add(new Paragraph("Status: " + booking.getStatus(), sectionFont));
            document.add(new Paragraph("----------------------------------------------------------------", regularFont));
            document.add(new Paragraph("Thank you for using RentHub! For support, contact support@renthub.com", regularFont));
            
            document.close();
            fos.close();

            log.info("Generated invoice PDF for booking ID: {}", booking.getId());
            return "/api/v1/storage/files/" + fileName;
        } catch (Exception e) {
            log.error("Failed to generate PDF for booking ID: {}", booking.getId(), e);
            return null;
        }
    }
}
