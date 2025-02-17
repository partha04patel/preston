package bio.guoda.preston;

import org.apache.commons.rdf.api.IRI;
import bio.guoda.preston.model.RefNodeFactory;

import java.net.URI;
import java.util.UUID;

import static bio.guoda.preston.model.RefNodeFactory.toIRI;
import static bio.guoda.preston.model.RefNodeFactory.fromUUID;

public class RefNodeConstants {

    public static final IRI HAD_MEMBER = RefNodeFactory.toIRI(URI.create("http://www.w3.org/ns/prov#hadMember"));
    public static final IRI SEE_ALSO = RefNodeFactory.toIRI(URI.create("http://www.w3.org/1999/02/22-rdf-syntax-ns#seeAlso"));

    public static final String PRESTON_URI = "https://preston.guoda.bio";

    public static final IRI PRESTON = RefNodeFactory.toIRI(URI.create(PRESTON_URI));


    public static final IRI HAS_FORMAT = RefNodeFactory.toIRI(URI.create("http://purl.org/dc/elements/1.1/format"));

    public static final IRI HAS_TYPE = RefNodeFactory.toIRI(URI.create("http://www.w3.org/ns/prov#type"));

    public static final IRI HAS_VERSION = RefNodeFactory.toIRI(URI.create("http://purl.org/pav/hasVersion"));
    public static final IRI HAS_PREVIOUS_VERSION = RefNodeFactory.toIRI(URI.create("http://purl.org/pav/previousVersion"));

    public static final IRI GENERATED_AT_TIME = RefNodeFactory.toIRI(URI.create("http://www.w3.org/ns/prov#generatedAtTime"));
    public static final IRI WAS_GENERATED_BY = RefNodeFactory.toIRI(URI.create("http://www.w3.org/ns/prov#wasGeneratedBy"));

    public static final UUID ARCHIVE_COLLECTION = UUID.fromString("0659a54f-b713-4f86-a917-5be166a14110");
    public static final IRI ARCHIVE = fromUUID(ARCHIVE_COLLECTION.toString());

    public static final IRI USED_BY = toIRI("http://www.w3.org/ns/prov#usedBy");
    public static final IRI AGENT = toIRI("http://www.w3.org/ns/prov#Agent");
    public static final IRI SOFTWARE_AGENT = toIRI("http://www.w3.org/ns/prov#SoftwareAgent");
    public static final IRI DESCRIPTION = toIRI("http://purl.org/dc/terms/description");
    public static final IRI COLLECTION = toIRI("http://www.w3.org/ns/prov#Collection");
    public static final IRI ORGANIZATION = toIRI("http://www.w3.org/ns/prov#Organization");
    public static final IRI WAS_ASSOCIATED_WITH = toIRI("http://www.w3.org/ns/prov#wasAssociatedWith");
    public static final IRI IS_A = toIRI("http://www.w3.org/1999/02/22-rdf-syntax-ns#type");
    public static final IRI CREATED_BY = toIRI("http://purl.org/pav/createdBy");
}
