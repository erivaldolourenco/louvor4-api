package br.com.louvor4.api.strategy.event;


import br.com.louvor4.api.enums.SetlistItemType;
import br.com.louvor4.api.exceptions.ValidationException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class EventSetlistItemStrategyResolver {

    private final Map<SetlistItemType, EventSetlistItemStrategy> strategies;

    public EventSetlistItemStrategyResolver(List<EventSetlistItemStrategy> strategyList) {
        this.strategies = strategyList.stream()
                .collect(Collectors.toMap(
                        EventSetlistItemStrategy::getType,
                        Function.identity()
                ));
    }

    public EventSetlistItemStrategy resolve(SetlistItemType type) {
        EventSetlistItemStrategy strategy = strategies.get(type);

        if (strategy == null) {
            throw new ValidationException("Tipo de item não suportado: " + type);
        }

        return strategy;
    }
}
