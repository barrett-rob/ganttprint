package com.mincom.viva.ganttprint;

import java.awt.Color;

import com.lowagie.text.Document;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfPageEvent;
import com.lowagie.text.pdf.PdfWriter;

class PdfPageEventImpl implements PdfPageEvent {

	private final GanttPrint ganttPrint;

	public PdfPageEventImpl(GanttPrint ganttPrint) {
		this.ganttPrint = ganttPrint;
	}

	@Override
	public void onOpenDocument(PdfWriter writer, Document document) {
	}

	@Override
	public void onStartPage(PdfWriter writer, Document document) {
		PdfContentByte canvas = writer.getDirectContent();
		canvas.saveState();
		printBorders(canvas);
		canvas.restoreState();
	}

	private void printBorders(PdfContentByte canvas) {
		canvas.setLineWidth(0.5f);
		canvas.setColorStroke(Color.black);
		Rectangle size = ganttPrint.size.rectangle;
		canvas.rectangle(GanttPrint.BORDER_PADDING, GanttPrint.BORDER_PADDING,
				size.getWidth() - GanttPrint.BORDER_PADDING * 2,
				size.getHeight() - GanttPrint.BORDER_PADDING * 2);
		canvas.stroke();
	}

	@Override
	public void onEndPage(PdfWriter writer, Document document) {
	}

	@Override
	public void onCloseDocument(PdfWriter writer, Document document) {
	}

	@Override
	public void onParagraph(PdfWriter writer, Document document,
			float paragraphPosition) {
	}

	@Override
	public void onParagraphEnd(PdfWriter writer, Document document,
			float paragraphPosition) {
	}

	@Override
	public void onChapter(PdfWriter writer, Document document,
			float paragraphPosition, Paragraph title) {
	}

	@Override
	public void onChapterEnd(PdfWriter writer, Document document,
			float paragraphPosition) {
	}

	@Override
	public void onSection(PdfWriter writer, Document document,
			float paragraphPosition, int depth, Paragraph title) {
	}

	@Override
	public void onSectionEnd(PdfWriter writer, Document document,
			float paragraphPosition) {
	}

	@Override
	public void onGenericTag(PdfWriter writer, Document document,
			Rectangle rect, String text) {
	}

}
