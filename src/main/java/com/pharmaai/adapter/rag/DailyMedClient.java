package com.pharmaai.adapter.rag;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DailyMedClient {

    private static final Logger log = LoggerFactory.getLogger(DailyMedClient.class);

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Mapa de sinônimos: nomes comuns em português/internacional → nome usado no DailyMed (inglês/US).
     */
    private static final Map<String, String> SYNONYMS = Map.ofEntries(
            Map.entry("paracetamol", "acetaminophen"),
            Map.entry("dipirona", "metamizole"),
            Map.entry("ibuprofeno", "ibuprofen"),
            Map.entry("amoxicilina", "amoxicillin"),
            Map.entry("omeprazol", "omeprazole"),
            Map.entry("losartana", "losartan"),
            Map.entry("metformina", "metformin"),
            Map.entry("atorvastatina", "atorvastatin"),
            Map.entry("sinvastatina", "simvastatin"),
            Map.entry("diclofenaco", "diclofenac"),
            Map.entry("nimesulida", "nimesulide"),
            Map.entry("azitromicina", "azithromycin"),
            Map.entry("ciprofloxacino", "ciprofloxacin"),
            Map.entry("clonazepam", "clonazepam"),
            Map.entry("rivotril", "clonazepam"),
            Map.entry("loratadina", "loratadine"),
            Map.entry("prednisona", "prednisone"),
            Map.entry("dexametasona", "dexamethasone"),
            Map.entry("captopril", "captopril"),
            Map.entry("enalapril", "enalapril")
    );

    /**
     * Resolve o nome do medicamento para o equivalente no DailyMed (inglês/US).
     */
    private String resolveDrugName(String drugName) {
        String resolved = SYNONYMS.getOrDefault(drugName.toLowerCase().trim(), drugName);
        if (!resolved.equals(drugName)) {
            log.info("🔄 Sinônimo resolvido: {} → {}", drugName, resolved);
        }
        return resolved;
    }

    /**
     * Busca até 5 setIds do medicamento pelo nome.
     * Endpoint: /spls.json?drug_name=xxx (retorna JSON)
     */
    public List<String> findSetIds(String drugName) {

        List<String> setIds = new ArrayList<>();

        try {
            String resolved = resolveDrugName(drugName);
            String url = "https://dailymed.nlm.nih.gov/dailymed/services/v2/spls.json?drug_name=" + resolved;

            HttpHeaders headers = new HttpHeaders();
            HttpEntity<Void> request = new HttpEntity<>(headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    request,
                    Map.class
            );

            Map body = response.getBody();

            if (body == null) {
                return setIds;
            }

            List data = (List) body.get("data");

            if (data == null || data.isEmpty()) {
                log.warn("❌ Nenhum resultado no DailyMed");
                return setIds;
            }

            int limit = Math.min(data.size(), 10);

            // Separar produtos adultos e infantis pelo título
            List<String> adultIds = new ArrayList<>();
            List<String> childIds = new ArrayList<>();

            for (int i = 0; i < limit; i++) {
                Map item = (Map) data.get(i);
                String setId = item.get("setid").toString();
                String title = item.get("title") != null ? item.get("title").toString().toLowerCase() : "";

                if (title.contains("children") || title.contains("pediatric") || title.contains("infant")) {
                    childIds.add(setId);
                } else {
                    adultIds.add(setId);
                }
            }

            // Adultos primeiro, depois infantis (para fallback)
            setIds.addAll(adultIds);
            setIds.addAll(childIds);

            // Limitar a 5 no total
            if (setIds.size() > 5) {
                setIds = new ArrayList<>(setIds.subList(0, 5));
            }

            log.info("✅ {} setIds encontrados ({} adulto, {} infantil)", setIds.size(), adultIds.size(), childIds.size());

            return setIds;

        } catch (Exception e) {
            log.error("❌ Erro ao buscar setIds: {}", e.getMessage(), e);
            return setIds;
        }
    }

    /**
     * Busca os detalhes do medicamento como XML (HL7 SPL).
     * O endpoint .json NÃO é suportado pela API do DailyMed para SPL individual.
     * Apenas .xml funciona (retorna status 200).
     */
    public String getDrugDetails(String setId) {

        try {
            String url = "https://dailymed.nlm.nih.gov/dailymed/services/v2/spls/" + setId + ".xml";

            HttpClient httpClient = HttpClient.newHttpClient();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Accept", "*/*")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            int statusCode = response.statusCode();
            String body = response.body();

            log.debug("📦 DailyMed status: {}", statusCode);

            if (statusCode != 200) {
                log.warn("❌ DailyMed retornou status: {}", statusCode);
                return null;
            }

            if (body == null || body.isBlank()) {
                log.warn("❌ Resposta vazia do DailyMed");
                return null;
            }

            log.info("✅ XML recebido do DailyMed ({} chars)", body.length());
            return body;

        } catch (Exception e) {
            log.error("❌ Erro ao buscar detalhes do medicamento: {}", e.getMessage(), e);
            return null;
        }
    }
}