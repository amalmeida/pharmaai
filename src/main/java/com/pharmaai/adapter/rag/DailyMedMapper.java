package com.pharmaai.adapter.rag;

import com.pharmaai.domain.model.DrugInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Mapper que extrai informações de medicamentos do XML HL7 SPL retornado pelo DailyMed.
 *
 * Seções relevantes (identificadas por displayName no XML):
 * - DOSAGE & ADMINISTRATION SECTION
 * - INDICATIONS & USAGE SECTION
 * - WARNINGS AND PRECAUTIONS SECTION / WARNINGS SECTION
 * - CONTRAINDICATIONS SECTION
 * - ADVERSE REACTIONS SECTION
 * - OTC - DO NOT USE SECTION
 * - OTC - STOP USE SECTION
 */
public class DailyMedMapper {

    private static final Logger log = LoggerFactory.getLogger(DailyMedMapper.class);

    private static final String HL7_NS = "urn:hl7-org:v3";

    public DrugInfo mapFromXml(String xml, String drugName) {

        DrugInfo drug = new DrugInfo();
        drug.setName(drugName);

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();

            Document doc = builder.parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));

            // Buscar todas as <section> no namespace HL7
            NodeList sections = doc.getElementsByTagNameNS(HL7_NS, "section");

            for (int i = 0; i < sections.getLength(); i++) {
                Element section = (Element) sections.item(i);

                String displayName = getSectionDisplayName(section);

                if (displayName == null || displayName.isBlank()) {
                    continue;
                }

                // Extrair texto desta seção + todas as sub-seções aninhadas
                String text = extractAllText(section);

                if (text.isBlank()) {
                    continue;
                }

                String displayLower = displayName.toLowerCase();

                if (displayLower.contains("indication") && displayLower.contains("usage")) {
                    drug.setIndication(text);
                    log.debug("  📋 Indicação extraída ({} chars)", text.length());
                }

                if (displayLower.contains("dosage") && displayLower.contains("administration")) {
                    drug.setMaxDose(text);
                    log.debug("  💊 Dosagem extraída ({} chars)", text.length());
                }

                if (displayLower.contains("contraindication")) {
                    drug.setContraindications(List.of(text));
                    log.debug("  ⚠️ Contraindicações extraídas ({} chars)", text.length());
                }

                if (displayLower.contains("warning")) {
                    // Warnings tem sub-seções (do not use, stop use, ask a doctor, etc.)
                    // Já extraímos tudo recursivamente via extractAllText
                    List<String> current = drug.getSideEffects();
                    List<String> updated = new ArrayList<>();
                    if (current != null) {
                        updated.addAll(current);
                    }
                    updated.add(text);
                    drug.setSideEffects(updated);
                    log.debug("  🔴 Warnings extraídos ({} chars)", text.length());
                }

                if (displayLower.contains("adverse")) {
                    List<String> current = drug.getSideEffects();
                    List<String> updated = new ArrayList<>();
                    if (current != null) {
                        updated.addAll(current);
                    }
                    updated.add(text);
                    drug.setSideEffects(updated);
                    log.debug("  🔴 Efeitos adversos extraídos ({} chars)", text.length());
                }
            }

            log.info("✅ Mapeamento XML → DrugInfo concluído para: {}", drugName);

        } catch (Exception e) {
            log.error("❌ Erro ao fazer parse do XML: {}", e.getMessage(), e);
        }

        return drug;
    }

    /**
     * Verifica se o DrugInfo tem conteúdo útil (pelo menos indicação ou dosagem).
     */
    public boolean hasContent(DrugInfo drug) {
        return (drug.getIndication() != null && !drug.getIndication().isBlank())
                || (drug.getMaxDose() != null && !drug.getMaxDose().isBlank());
    }

    /**
     * Extrai o displayName do elemento <code> filho direto de uma <section>.
     */
    private String getSectionDisplayName(Element section) {
        NodeList codes = section.getElementsByTagNameNS(HL7_NS, "code");
        for (int i = 0; i < codes.getLength(); i++) {
            Element code = (Element) codes.item(i);
            if (code.getParentNode() == section) {
                return code.getAttribute("displayName");
            }
        }
        return null;
    }

    /**
     * Extrai todo o texto visível da seção, incluindo sub-seções aninhadas
     * dentro de <component><section>.
     */
    private String extractAllText(Element section) {
        StringBuilder sb = new StringBuilder();

        // 1. Texto direto do <text> desta seção
        NodeList textNodes = section.getElementsByTagNameNS(HL7_NS, "text");
        for (int i = 0; i < textNodes.getLength(); i++) {
            Element textElement = (Element) textNodes.item(i);
            // Pegar <text> que seja filho direto da section
            if (textElement.getParentNode() == section) {
                String raw = textElement.getTextContent();
                String cleaned = raw.replaceAll("\\s+", " ").trim();
                if (!cleaned.isBlank()) {
                    sb.append(cleaned).append(" ");
                }
            }
        }

        // 2. Sub-seções aninhadas: <component><section>
        NodeList components = section.getElementsByTagNameNS(HL7_NS, "component");
        for (int i = 0; i < components.getLength(); i++) {
            Element component = (Element) components.item(i);
            // Somente componentes filhos diretos desta seção
            if (component.getParentNode() != section) {
                continue;
            }
            NodeList subSections = component.getElementsByTagNameNS(HL7_NS, "section");
            for (int j = 0; j < subSections.getLength(); j++) {
                Element subSection = (Element) subSections.item(j);
                // Recursivo: pegar texto da sub-seção e suas sub-sub-seções
                String subText = extractAllText(subSection);
                if (!subText.isBlank()) {
                    sb.append(subText).append(" ");
                }
            }
        }

        return sb.toString().trim();
    }
}