package br.com.louvor4.louvor4api.services;

import br.com.louvor4.louvor4api.converter.MinistryConverter;
import br.com.louvor4.louvor4api.dto.MinistryDTO;
import br.com.louvor4.louvor4api.models.Ministry;
import br.com.louvor4.louvor4api.repositories.MinistryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.logging.Logger;

@Service
public class MinistryService {
    private Logger logger = Logger.getLogger(MinistryService.class.getName());
    @Autowired
    MinistryRepository ministryRepository;

    public MinistryDTO create(MinistryDTO ministryDto){
        Ministry ministry = MinistryConverter.INSTANCE.toEntity(ministryDto);
       return  MinistryConverter.INSTANCE.toDto(ministryRepository.save(ministry));
    }
}
