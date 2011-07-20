package com.mincom.viva.ganttprint;

import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPCellEvent;

class HeaderPdfPCellEventImpl implements PdfPCellEvent {

	private final GanttPrint ganttPrint;

	public HeaderPdfPCellEventImpl(GanttPrint ganttPrint) {
		this.ganttPrint = ganttPrint;
	}

	@Override
	public void cellLayout(PdfPCell cell, Rectangle position,
			PdfContentByte[] canvases) {
		System.out.println("header cell event");
	}
}
