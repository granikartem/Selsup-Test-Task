package org.example.misc;

import org.json.JSONArray;
import org.json.JSONObject;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public  class CrptApi {
    public class Description{
        private final String participantInn;

        public Description(JSONObject jsonDescription) {
            this(jsonDescription.getString("participantInn"));
        }
        public Description(String participantInn) {
            this.participantInn = participantInn;
        }
    }

    public class Product{
        private final String certificateDocument;
        private final LocalDate certificateDocumentDate;
        private final String certificateDocumentNumber;
        private final String ownerInn;
        private final String producerInn;
        private final LocalDate productionDate;
        private final String tnvedCode;
        private final String uitCode;
        private final String uituCode;

        public Product(String certificateDocument, LocalDate certificateDocumentDate,
                       String certificateDocumentNumber, String ownerInn, String producerInn,
                       LocalDate productionDate, String tnvedCode,
                       String uitCode, String uituCode) {
            this.certificateDocument = certificateDocument;
            this.certificateDocumentDate = certificateDocumentDate;
            this.certificateDocumentNumber = certificateDocumentNumber;
            this.ownerInn = ownerInn;
            this.producerInn = producerInn;
            this.productionDate = productionDate;
            this.tnvedCode = tnvedCode;
            this.uitCode = uitCode;
            this.uituCode = uituCode;
        }

        public Product(Object e){
            this((JSONObject) e);
        }
        public Product(JSONObject jsonProduct) {
            this(jsonProduct.getString("certificate_document"),
                    LocalDate.parse(jsonProduct.getString("certificate_document_date")),
                    jsonProduct.getString("certificate_document_number"),
                    jsonProduct.getString("owner_inn"),
                    jsonProduct.getString("producer_inn"),
                    LocalDate.parse(jsonProduct.getString("production_date")),
                    jsonProduct.getString("tnved_code"),
                    jsonProduct.getString("uit_code"),
                    jsonProduct.getString("uitu_code")
            );
        }
    }
    public enum DocumentType{
        LP_INTRODUCE_GOODS ("LP_INTRODUCE_GOODS");

        DocumentType(String lp_introduce_goods) {
        }
    }
    public class Document{
        private final Description description;
        private final String docId;
        private final String docStatus;
        private final DocumentType documentType;
        private final boolean importRequest;
        private final String ownerInn;
        private final String participantInn;
        private final String producerInn;
        private final LocalDate productionDate;
        private final String productionType;
        private final List<Product> products;
        private final LocalDate regDate;
        private final String regNumber;

        public Document(Description description, String docId, String docStatus, DocumentType documentType,
                        boolean importRequest, String ownerInn, String participantInn, String producerInn,
                        LocalDate productionDate, String productionType, List<Product> products,
                        LocalDate regDate, String regNumber) {
            this.description = description;
            this.docId = docId;
            this.docStatus = docStatus;
            this.documentType = documentType;
            this.importRequest = importRequest;
            this.ownerInn = ownerInn;
            this.participantInn = participantInn;
            this.producerInn = producerInn;
            this.productionDate = productionDate;
            this.productionType = productionType;
            this.products = products;
            this.regDate = regDate;
            this.regNumber = regNumber;
        }

        public Document(JSONObject jsonDocument) {
            this(new Description(jsonDocument.getJSONObject("description")),
                    jsonDocument.getString("doc_id"),
                    jsonDocument.getString("doc_status"),
                    jsonDocument.getEnum(DocumentType.class, "doc_type"),
                    jsonDocument.getBoolean("importRequest"),
                    jsonDocument.getString("owner_inn"),
                    jsonDocument.getString("participant_inn"),
                    jsonDocument.getString("producer_inn"),
                    LocalDate.parse(jsonDocument.getString("production_date")),
                    jsonDocument.getString("production_type"),
                    new ArrayList<>(),
                    LocalDate.parse(jsonDocument.getString("reg_date")),
                    jsonDocument.getString("reg_number")
                    );
            JSONArray jsonArray = jsonDocument.getJSONArray("products");
            for (int i = 0; i < jsonArray.length(); i++) {
                this.products.add(new Product(jsonArray.get(i)));
            }
        }
    }

    private final TimeUnit timeUnit;
    private final int requestLimit;
    private Instant lastSemaphoreStartTime;
    private Semaphore lastSemaphore;

    public CrptApi(TimeUnit timeUnit, int requestLimit) {
        this.timeUnit = timeUnit;
        this.requestLimit = requestLimit;
        this.lastSemaphoreStartTime = null;
        this.lastSemaphore = null;
    }

    /**
     * Метод, имитирующий обработку HTTP Post-запроса
     * @param requestBody - Строка с json-объектом документа
     */
    public void handleHttpRequest(String requestBody) {
        JSONObject document = new JSONObject(requestBody);
        try {
            request(document, "signatureString");
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Основной метод, реализующий основную бизес-логику и ограничения по количеству запросов в интервал времени.
     * @param document - json-объект документа
     * @param signature - подпись
     * @return объект класса Document аналогичный json'у
     * @throws InterruptedException
     */
    public Document request(JSONObject document, String signature) throws InterruptedException {
        Semaphore mySemaphore;
        Document resultDocument;
        synchronized(this) {
            if (lastSemaphoreStartTime == null ||
                    Duration.between(lastSemaphoreStartTime, Instant.now()).compareTo(timeUnit.toChronoUnit().getDuration()) == 1) {
                lastSemaphoreStartTime = Instant.now();
                lastSemaphore = new Semaphore(requestLimit);
            }
            mySemaphore = lastSemaphore;
            notifyAll();
        }
        mySemaphore.acquire();
        checkSignature(signature);
        resultDocument = new Document(document);
        mySemaphore.release();
        return resultDocument;
    }

    /**
    Искусственный метод для проверки подписи
     */

    private void checkSignature(String signature) {
        return;
    }
}
