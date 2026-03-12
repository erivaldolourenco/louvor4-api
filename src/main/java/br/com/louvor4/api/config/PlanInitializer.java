package br.com.louvor4.api.config;

import br.com.louvor4.api.models.Plan;
import br.com.louvor4.api.repositories.PlanRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class PlanInitializer implements CommandLineRunner {

    private final PlanRepository planRepository;

    public PlanInitializer(PlanRepository planRepository) {
        this.planRepository = planRepository;
    }

    @Override
    public void run(String... args) {
        if (planRepository.count() > 0) return;

        planRepository.save(buildPlan("FREE", 0));
        planRepository.save(buildPlan("STARTER", 1));
        planRepository.save(buildPlan("PRO", 3));
        planRepository.save(buildPlan("ELITE", 10));
    }

    private Plan buildPlan(String name, int maxProjects) {
        Plan plan = new Plan();
        plan.setName(name);
        plan.setMaxProjects(maxProjects);
        return plan;
    }
}
