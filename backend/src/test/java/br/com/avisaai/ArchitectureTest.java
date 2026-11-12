package br.com.avisaai;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Arquitetura hexagonal verificada a cada build: o domínio não conhece framework nem adaptadores,
 * e a aplicação não conhece os adaptadores (web, WhatsApp).
 */
class ArchitectureTest {

    private final JavaClasses classes = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("br.com.avisaai");

    @Test
    void domainDependsOnNoFrameworkNorOuterLayers() {
        noClasses().that().resideInAPackage("..domain..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "org.springframework..", "..application..", "..adapter..", "..config..")
                .check(classes);
    }

    @Test
    void applicationDoesNotKnowTheAdapters() {
        noClasses().that().resideInAPackage("..application..")
                .should().dependOnClassesThat().resideInAnyPackage("..adapter..", "..config..")
                .check(classes);
    }

    @Test
    void adaptersDoNotTalkToEachOther() {
        noClasses().that().resideInAPackage("..adapter.web..")
                .should().dependOnClassesThat().resideInAPackage("..adapter.whatsapp..")
                .check(classes);
    }
}
