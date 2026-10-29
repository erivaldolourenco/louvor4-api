package br.com.louvor4.api.services.impl;

import br.com.louvor4.api.config.security.CurrentUserProvider;
import br.com.louvor4.api.exceptions.ValidationException;
import br.com.louvor4.api.models.SongCategory;
import br.com.louvor4.api.repositories.SongCategoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SongCategoryServiceImplTest {

    @Mock SongCategoryRepository songCategoryRepository;
    @Mock CurrentUserProvider currentUserProvider;

    @InjectMocks SongCategoryServiceImpl service;

    @Test
    void delete_tiraDasMusicasEMedleysAntesDeExcluir() {
        UUID categoryId = UUID.randomUUID();
        SongCategory category = new SongCategory();
        category.setId(categoryId);
        when(songCategoryRepository.findById(categoryId)).thenReturn(Optional.of(category));

        service.delete(categoryId);

        InOrder inOrder = inOrder(songCategoryRepository);
        inOrder.verify(songCategoryRepository).deleteSongAssignmentsByCategoryId(categoryId);
        inOrder.verify(songCategoryRepository).deleteMedleyAssignmentsByCategoryId(categoryId);
        inOrder.verify(songCategoryRepository).delete(category);
    }

    @Test
    void delete_categoriaInexistenteNaoApagaNada() {
        UUID categoryId = UUID.randomUUID();
        when(songCategoryRepository.findById(categoryId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(categoryId)).isInstanceOf(ValidationException.class);
        verify(songCategoryRepository, never()).delete(any());
        verify(songCategoryRepository, never()).deleteSongAssignmentsByCategoryId(any());
    }
}
