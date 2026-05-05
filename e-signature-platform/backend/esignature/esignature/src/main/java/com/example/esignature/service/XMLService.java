package com.example.esignature.service;

import com.example.esignature.exception.BadRequestException;
import com.example.esignature.model.entity.CompanySettings;
import com.example.esignature.model.entity.Invoice;
import com.example.esignature.model.entity.InvoiceItem;
import com.example.esignature.repository.CompanySettingsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
@Slf4j
public class XMLService {

    private final CompanySettingsRepository companySettingsRepository;

    private static final String UBL_NAMESPACE = "urn:oasis:names:specification:ubl:schema:xsd:Invoice-2";
    private static final String CAC_NAMESPACE = "urn:oasis:names:specification:ubl:schema:xsd:CommonAggregateComponents-2";
    private static final String CBC_NAMESPACE = "urn:oasis:names:specification:ubl:schema:xsd:CommonBasicComponents-2";

    public String generateUBLXML(Invoice invoice) {
        try {
            CompanySettings settings = companySettingsRepository.findFirstByOrderByCreatedAtAsc()
                    .orElseThrow(() -> new BadRequestException("Company settings not found"));

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.newDocument();

            // Root element
            Element invoiceElement = doc.createElementNS(UBL_NAMESPACE, "Invoice");
            invoiceElement.setAttribute("xmlns", UBL_NAMESPACE);
            invoiceElement.setAttribute("xmlns:cac", CAC_NAMESPACE);
            invoiceElement.setAttribute("xmlns:cbc", CBC_NAMESPACE);
            doc.appendChild(invoiceElement);

            // UBL Version
            addElement(doc, invoiceElement, CBC_NAMESPACE, "UBLVersionID", "2.1");
            
            // Invoice ID
            addElement(doc, invoiceElement, CBC_NAMESPACE, "ID", invoice.getInvoiceNumber());
            
            // Issue Date
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            addElement(doc, invoiceElement, CBC_NAMESPACE, "IssueDate", invoice.getIssueDate().format(dateFormatter));
            
            // Due Date
            if (invoice.getDueDate() != null) {
                addElement(doc, invoiceElement, CBC_NAMESPACE, "DueDate", invoice.getDueDate().format(dateFormatter));
            }
            
            // Invoice Type Code (380 = Commercial Invoice)
            addElement(doc, invoiceElement, CBC_NAMESPACE, "InvoiceTypeCode", "380");
            
            // Document Currency Code
            addElement(doc, invoiceElement, CBC_NAMESPACE, "DocumentCurrencyCode", invoice.getCurrency());
            
            // Accounting Supplier Party (Company)
            Element supplierParty = doc.createElementNS(CAC_NAMESPACE, "cac:AccountingSupplierParty");
            Element supplierPartyElement = doc.createElementNS(CAC_NAMESPACE, "cac:Party");
            
            Element supplierPartyName = doc.createElementNS(CAC_NAMESPACE, "cac:PartyName");
            addElement(doc, supplierPartyName, CBC_NAMESPACE, "Name", settings.getCompanyName());
            supplierPartyElement.appendChild(supplierPartyName);
            
            Element supplierPostalAddress = doc.createElementNS(CAC_NAMESPACE, "cac:PostalAddress");
            addElement(doc, supplierPostalAddress, CBC_NAMESPACE, "StreetName", settings.getAddress());
            supplierPartyElement.appendChild(supplierPostalAddress);
            
            Element supplierPartyTaxScheme = doc.createElementNS(CAC_NAMESPACE, "cac:PartyTaxScheme");
            addElement(doc, supplierPartyTaxScheme, CBC_NAMESPACE, "CompanyID", settings.getTaxId());
            supplierPartyElement.appendChild(supplierPartyTaxScheme);
            
            supplierParty.appendChild(supplierPartyElement);
            invoiceElement.appendChild(supplierParty);
            
            // Accounting Customer Party (Client)
            Element customerParty = doc.createElementNS(CAC_NAMESPACE, "cac:AccountingCustomerParty");
            Element customerPartyElement = doc.createElementNS(CAC_NAMESPACE, "cac:Party");
            
            Element customerPartyName = doc.createElementNS(CAC_NAMESPACE, "cac:PartyName");
            addElement(doc, customerPartyName, CBC_NAMESPACE, "Name", invoice.getClient().getName());
            customerPartyElement.appendChild(customerPartyName);
            
            if (invoice.getClient().getAddress() != null) {
                Element customerPostalAddress = doc.createElementNS(CAC_NAMESPACE, "cac:PostalAddress");
                addElement(doc, customerPostalAddress, CBC_NAMESPACE, "StreetName", invoice.getClient().getAddress());
                customerPartyElement.appendChild(customerPostalAddress);
            }
            
            if (invoice.getClient().getTaxId() != null) {
                Element customerPartyTaxScheme = doc.createElementNS(CAC_NAMESPACE, "cac:PartyTaxScheme");
                addElement(doc, customerPartyTaxScheme, CBC_NAMESPACE, "CompanyID", invoice.getClient().getTaxId());
                customerPartyElement.appendChild(customerPartyTaxScheme);
            }
            
            customerParty.appendChild(customerPartyElement);
            invoiceElement.appendChild(customerParty);
            
            // Tax Total
            Element taxTotal = doc.createElementNS(CAC_NAMESPACE, "cac:TaxTotal");
            Element taxAmount = doc.createElementNS(CBC_NAMESPACE, "cbc:TaxAmount");
            taxAmount.setAttribute("currencyID", invoice.getCurrency());
            taxAmount.setTextContent(invoice.getTaxAmount().toString());
            taxTotal.appendChild(taxAmount);
            invoiceElement.appendChild(taxTotal);
            
            // Legal Monetary Total
            Element legalMonetaryTotal = doc.createElementNS(CAC_NAMESPACE, "cac:LegalMonetaryTotal");
            
            Element lineExtensionAmount = doc.createElementNS(CBC_NAMESPACE, "cbc:LineExtensionAmount");
            lineExtensionAmount.setAttribute("currencyID", invoice.getCurrency());
            lineExtensionAmount.setTextContent(invoice.getSubtotal().toString());
            legalMonetaryTotal.appendChild(lineExtensionAmount);
            
            Element taxExclusiveAmount = doc.createElementNS(CBC_NAMESPACE, "cbc:TaxExclusiveAmount");
            taxExclusiveAmount.setAttribute("currencyID", invoice.getCurrency());
            taxExclusiveAmount.setTextContent(invoice.getSubtotal().toString());
            legalMonetaryTotal.appendChild(taxExclusiveAmount);
            
            Element taxInclusiveAmount = doc.createElementNS(CBC_NAMESPACE, "cbc:TaxInclusiveAmount");
            taxInclusiveAmount.setAttribute("currencyID", invoice.getCurrency());
            taxInclusiveAmount.setTextContent(invoice.getTotalAmount().toString());
            legalMonetaryTotal.appendChild(taxInclusiveAmount);
            
            Element payableAmount = doc.createElementNS(CBC_NAMESPACE, "cbc:PayableAmount");
            payableAmount.setAttribute("currencyID", invoice.getCurrency());
            payableAmount.setTextContent(invoice.getTotalAmount().toString());
            legalMonetaryTotal.appendChild(payableAmount);
            
            invoiceElement.appendChild(legalMonetaryTotal);
            
            // Invoice Lines
            for (InvoiceItem item : invoice.getItems()) {
                Element invoiceLine = doc.createElementNS(CAC_NAMESPACE, "cac:InvoiceLine");
                
                addElement(doc, invoiceLine, CBC_NAMESPACE, "ID", String.valueOf(item.getSortOrder()));
                
                Element invoicedQuantity = doc.createElementNS(CBC_NAMESPACE, "cbc:InvoicedQuantity");
                invoicedQuantity.setAttribute("unitCode", "EA");
                invoicedQuantity.setTextContent(item.getQuantity().toString());
                invoiceLine.appendChild(invoicedQuantity);
                
                Element lineExtAmount = doc.createElementNS(CBC_NAMESPACE, "cbc:LineExtensionAmount");
                lineExtAmount.setAttribute("currencyID", invoice.getCurrency());
                lineExtAmount.setTextContent(item.getLineTotal().toString());
                invoiceLine.appendChild(lineExtAmount);
                
                Element itemElement = doc.createElementNS(CAC_NAMESPACE, "cac:Item");
                addElement(doc, itemElement, CBC_NAMESPACE, "Description", item.getDescription());
                invoiceLine.appendChild(itemElement);
                
                Element price = doc.createElementNS(CAC_NAMESPACE, "cac:Price");
                Element priceAmount = doc.createElementNS(CBC_NAMESPACE, "cbc:PriceAmount");
                priceAmount.setAttribute("currencyID", invoice.getCurrency());
                priceAmount.setTextContent(item.getUnitPrice().toString());
                price.appendChild(priceAmount);
                invoiceLine.appendChild(price);
                
                invoiceElement.appendChild(invoiceLine);
            }
            
            // Convert to String
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            
            StringWriter writer = new StringWriter();
            transformer.transform(new DOMSource(doc), new StreamResult(writer));
            
            String xml = writer.toString();
            
            log.info("UBL XML generated for invoice: {}", invoice.getInvoiceNumber());
            return xml;
            
        } catch (Exception e) {
            log.error("Error generating UBL XML: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate UBL XML: " + e.getMessage());
        }
    }

    private void addElement(Document doc, Element parent, String namespace, String tagName, String textContent) {
        Element element = doc.createElementNS(namespace, "cbc:" + tagName);
        element.setTextContent(textContent);
        parent.appendChild(element);
    }
}