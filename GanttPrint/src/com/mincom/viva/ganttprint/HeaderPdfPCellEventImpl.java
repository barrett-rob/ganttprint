package com.mincom.viva.ganttprint;

import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;

class HeaderPdfPCellEventImpl extends PdfPCellEventImpl {

	public HeaderPdfPCellEventImpl(GanttPrint ganttPrint) {
		super(ganttPrint, null);
	}

	@Override
	public void cellLayout(PdfPCell cell, Rectangle position,
			PdfContentByte[] canvases) {
		PdfContentByte canvas = canvases[PdfPTable.BACKGROUNDCANVAS];
		canvas.saveState();
		paintScaleLines(canvas, position);
		canvas.restoreState();
	}
}
