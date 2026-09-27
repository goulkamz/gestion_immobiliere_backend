package com.immobilier.gestionImmobiliere.modules.annonces.services;

import com.immobilier.gestionImmobiliere.donnees.annonces.model.Contact;
import com.immobilier.gestionImmobiliere.donnees.annonces.model.StatutContact;
import com.immobilier.gestionImmobiliere.donnees.annonces.repository.ContactRepository;
import com.immobilier.gestionImmobiliere.exceptions.ResourceNotFoundException;
import com.immobilier.gestionImmobiliere.exceptions.TooManyRequestsException;
import com.immobilier.gestionImmobiliere.modules.annonces.dto.requests.CreateContactDTO;
import com.immobilier.gestionImmobiliere.modules.annonces.dto.requests.UpdateStatutContactDTO;
import com.immobilier.gestionImmobiliere.modules.annonces.dto.responses.ContactResponseDTO;
import com.immobilier.gestionImmobiliere.modules.user.jwt.RateLimitService;
import com.immobilier.gestionImmobiliere.utils.ClientIpResolver;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static com.immobilier.gestionImmobiliere.utils.BuildSuccessResponse.buildSuccessResponse;

@Service
public class ContactService {

    private final ContactRepository contactRepository;
    private final RateLimitService rateLimitService;
    private final ClientIpResolver clientIpResolver;

    public ContactService(ContactRepository contactRepository, RateLimitService rateLimitService, ClientIpResolver clientIpResolver) {
        this.contactRepository = contactRepository;
        this.rateLimitService = rateLimitService;
        this.clientIpResolver = clientIpResolver;
    }

    public ResponseEntity<?> getAll(StatutContact statut, Pageable pageable) {
        Page<Contact> page = statut != null ? contactRepository.findByStatut(statut, pageable) : contactRepository.findAll(pageable);
        return buildSuccessResponse(HttpStatus.OK, "Liste des contacts", "CONTACT_LIST", page.map(this::toDto));
    }

    @Transactional
    public ResponseEntity<?> create(CreateContactDTO dto, HttpServletRequest request) {
        if (rateLimitService.estLimiteDepassee("contact:" + clientIpResolver.resolve(request))) {
            throw new TooManyRequestsException("Trop de messages envoyés. Veuillez réessayer plus tard.");
        }

        Contact contact = Contact.builder()
                .nomComplet(dto.getNomComplet())
                .email(dto.getEmail())
                .telephone(dto.getTelephone())
                .sujet(dto.getSujet())
                .message(dto.getMessage())
                .dateEnvoi(LocalDateTime.now())
                .statut(StatutContact.NON_LU)
                .build();
        contactRepository.save(contact);
        return buildSuccessResponse(HttpStatus.CREATED, "Message envoyé avec succès", "CONTACT_CREATED", toDto(contact));
    }

    @Transactional
    public ResponseEntity<?> updateStatut(Integer id, UpdateStatutContactDTO dto) {
        Contact contact = findOrThrow(id);
        contact.setStatut(dto.getStatut());
        contactRepository.save(contact);
        return buildSuccessResponse(HttpStatus.OK, "Statut mis à jour", "CONTACT_STATUT_UPDATED", toDto(contact));
    }

    private Contact findOrThrow(Integer id) {
        return contactRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("contact", id));
    }

    private ContactResponseDTO toDto(Contact c) {
        return ContactResponseDTO.builder()
                .idContact(c.getIdContact())
                .nomComplet(c.getNomComplet())
                .email(c.getEmail())
                .telephone(c.getTelephone())
                .sujet(c.getSujet())
                .message(c.getMessage())
                .dateEnvoi(c.getDateEnvoi())
                .statut(c.getStatut())
                .build();
    }
}