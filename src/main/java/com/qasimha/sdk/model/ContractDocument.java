package com.qasimha.sdk.model;

/** The financing contract of an order, from {@code orders().contract(id)}. */
public final class ContractDocument {

  private final byte[] content;
  private final String contentType;
  private final String fileName;
  private final String requestId;

  public ContractDocument(byte[] content, String contentType, String fileName, String requestId) {
    this.content = content == null ? new byte[0] : content.clone();
    this.contentType = contentType;
    this.fileName = fileName;
    this.requestId = requestId;
  }

  /** The document bytes. */
  public byte[] getContent() {
    return content.clone();
  }

  /** The {@code Content-Type} of the document, normally {@code application/pdf}. */
  public String getContentType() {
    return contentType;
  }

  /** The file name from {@code Content-Disposition}, when the API sent one. */
  public String getFileName() {
    return fileName;
  }

  /** The {@code X-Request-Id} of the download. */
  public String getRequestId() {
    return requestId;
  }
}
