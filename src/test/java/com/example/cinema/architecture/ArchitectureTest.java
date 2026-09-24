package com.example.cinema.architecture;

import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

@AnalyzeClasses(packages = "com.example.cinema", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    private static final String MODULE_ROOT = "com.example.cinema.modules.";

    // The initial skeleton has no module classes; these rules activate as classes are added.
    @ArchTest
    static final ArchRule business_does_not_depend_on_web_or_data = noClasses()
            .that().resideInAPackage("..modules..business..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "org.springframework.web..",
                    "org.springframework.http..",
                    "org.springframework.data..",
                    "jakarta.servlet..",
                    "jakarta.persistence..",
                    "org.hibernate..",
                    "org.postgresql..",
                    "java.sql..",
                    "javax.sql..",
                    "..modules..data..").allowEmptyShould(true);

    @ArchTest
    static final ArchRule api_does_not_depend_on_data = noClasses()
            .that().resideInAPackage("..modules..api..")
            .should().dependOnClassesThat().resideInAPackage("..modules..data..").allowEmptyShould(true);

    @ArchTest
    static final ArchRule modules_do_not_depend_on_another_modules_api_or_data = classes()
            .that().resideInAPackage("..modules..")
            .should(new ArchCondition<>("not depend on another module's API or data package") {
                @Override
                public void check(JavaClass source, ConditionEvents events) {
                    String sourceModule = moduleName(source.getPackageName());
                    for (Dependency dependency : source.getDirectDependenciesFromSelf()) {
                        JavaClass target = dependency.getTargetClass();
                        String targetModule = internalModuleName(target.getPackageName());
                        if (sourceModule != null && targetModule != null && !sourceModule.equals(targetModule)) {
                            events.add(SimpleConditionEvent.violated(source,
                                    source.getName() + " depends on " + target.getName()));
                        }
                    }
                }
            }).allowEmptyShould(true);

    @ArchTest
    static final ArchRule modules_do_not_form_cycles = slices()
            .matching(MODULE_ROOT + "(*)..")
            .should().beFreeOfCycles().allowEmptyShould(true);

    private static String moduleName(String packageName) {
        if (!packageName.startsWith(MODULE_ROOT)) {
            return null;
        }
        return packageName.substring(MODULE_ROOT.length()).split("\\.")[0];
    }

    private static String internalModuleName(String packageName) {
        if (!packageName.startsWith(MODULE_ROOT)) {
            return null;
        }
        String[] parts = packageName.substring(MODULE_ROOT.length()).split("\\.");
        return parts.length >= 2 && (parts[1].equals("api") || parts[1].equals("data"))
                ? parts[0]
                : null;
    }
}
