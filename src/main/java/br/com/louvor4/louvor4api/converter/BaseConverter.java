package br.com.louvor4.louvor4api.converter;

import java.util.List;

public interface BaseConverter<T,D> {
    T toEntity(D dto);
    List<T> toEntity(List<D> dto);
    D toDto(T entity);
    List<D> toDto(List<T> entity);
}
