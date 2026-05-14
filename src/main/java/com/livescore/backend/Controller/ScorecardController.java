package com.livescore.backend.Controller;

import com.livescore.backend.Service.ScorecardPdfService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ScorecardController {

    @Autowired
    private ScorecardPdfService scorecardPdfService;

    @GetMapping("/match/{id}/scorecard/pdf")
    public ResponseEntity<byte[]> getScorecardPdf(@PathVariable Long id) {
        try {
            byte[] pdfBytes = scorecardPdfService.generateScorecardPdf(id);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "scorecard-" + id + ".pdf");

            return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
