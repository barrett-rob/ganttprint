package com.mincom.viva.ganttprint;

import java.awt.Color;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfContentByte;

class HeaderPdfPCellEventImpl extends PdfPCellEventImpl {

	private static final int LEFT_PADDING = 5;
	private static final BaseFont SCALE_FONT;

	private final DateTimeFormatter dailyFormatter = DateTimeFormat
			.forPattern("EEE d MMM");
	private final DateTimeFormatter weeklyFormatter = DateTimeFormat
			.forPattern("d MMM");
	private final DateTimeFormatter monthlyFormatter = DateTimeFormat
			.forPattern("MMM");

	static {
		try {
			SCALE_FONT = BaseFont.createFont(BaseFont.HELVETICA_BOLD, "UTF-8",
					false);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public HeaderPdfPCellEventImpl(GanttPrint ganttPrint) {
		super(ganttPrint, null);
	}

	@Override
	protected void paintBar(PdfContentByte canvas, Rectangle position) {
		/* no bar in header cell */
	}
	
	@Override
	protected void paintDay(PdfContentByte canvas, Rectangle position, float f,
			DateTime dt) {
		super.paintDay(canvas, position, f, dt);
		writeDateHeader(canvas, position, f, dt, dailyFormatter);
	}

	@Override
	protected void paintWeek(PdfContentByte canvas, Rectangle position,
			float f, DateTime dt) {
		super.paintWeek(canvas, position, f, dt);
		writeDateHeader(canvas, position, f, dt, weeklyFormatter);
	}

	@Override
	protected void paintMonth(PdfContentByte canvas, Rectangle position,
			float f, DateTime dt) {
		super.paintMonth(canvas, position, f, dt);
		writeDateHeader(canvas, position, f, dt, monthlyFormatter);
	}

	private void writeDateHeader(PdfContentByte canvas, Rectangle position,
			float f, DateTime dt, DateTimeFormatter dtf) {
		canvas.setColorFill(Color.black);
		canvas.setFontAndSize(SCALE_FONT, 8);
		float x = position.getLeft() + f + LEFT_PADDING;
		float y = position.getBottom() + position.getHeight() / 3;
		canvas.showTextAligned(PdfContentByte.ALIGN_LEFT, dt.toString(dtf), x,
				y, 0);
	}

}
