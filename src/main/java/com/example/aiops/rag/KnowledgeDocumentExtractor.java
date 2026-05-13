package com.example.aiops.rag;

import com.example.aiops.exception.BusinessException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.TikaCoreProperties;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.xml.sax.ContentHandler;

import java.io.InputStream;

@Component
public class KnowledgeDocumentExtractor {

    private static final long MAX_FILE_SIZE = 10L * 1024L * 1024L;

    private final AutoDetectParser parser = new AutoDetectParser();

    public ExtractedDocument extract(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(40040, "import file is empty");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException(40041, "import file exceeds 10MB limit");
        }
        try (InputStream inputStream = file.getInputStream()) {
            ContentHandler handler = new BodyContentHandler(-1);
            Metadata metadata = new Metadata();
            metadata.set(TikaCoreProperties.RESOURCE_NAME_KEY, file.getOriginalFilename());
            parser.parse(inputStream, handler, metadata, new ParseContext());
            String text = KnowledgeTextUtils.normalizeText(handler.toString());
            if (text.isBlank()) {
                throw new BusinessException(40042, "no text could be extracted from file");
            }
            String contentType = file.getContentType();
            if (contentType == null || contentType.isBlank()) {
                contentType = metadata.get(Metadata.CONTENT_TYPE);
            }
            return new ExtractedDocument(text, contentType);
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessException(40043, "failed to parse import file: " + ex.getMessage());
        }
    }

    public record ExtractedDocument(String text, String mimeType) {
    }
}
