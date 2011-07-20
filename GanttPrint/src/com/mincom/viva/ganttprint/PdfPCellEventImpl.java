package com.mincom.viva.ganttprint;

import java.awt.Color;

import org.joda.time.DateTime;
import org.joda.time.Duration;

import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPCellEvent;
import com.lowagie.text.pdf.PdfPTable;
import com.mincom.viva.ganttprint.GanttPrint.SCALE_LEVEL;

class PdfPCellEventImpl implements PdfPCellEvent {

	private final ScheduleItem scheduleItem;
	private final GanttPrint ganttPrint;

	public PdfPCellEventImpl(GanttPrint ganttPrint, ScheduleItem si) {
		this.ganttPrint = ganttPrint;
		this.scheduleItem = si;
	}

	@Override
	public void cellLayout(PdfPCell cell, Rectangle position,
			PdfContentByte[] canvases) {
		PdfContentByte canvas = canvases[PdfPTable.BACKGROUNDCANVAS];
		canvas.saveState();
		paintScaleLines(canvas, position);
		paintBar(canvas, position);
		canvas.restoreState();
	}

	private void paintScaleLines(PdfContentByte canvas, Rectangle position) {
		DateTime d0 = new DateTime(ganttPrint.first);
		d0.withMillisOfDay(0);
		while (d0.isBefore(ganttPrint.last)) {
			d0 = d0.plus(Duration.standardDays(1));
			DateTime d0end = d0.plusDays(1).minusSeconds(1);
			float x0 = ganttPrint.getX(d0.getMillis());
			float w0 = ganttPrint.getX(d0end.getMillis()) - x0;
			paintDay(canvas, position, x0);
			int dayOfWeek = d0.getDayOfWeek();
			if (dayOfWeek == 6) {
				/* saturday */
				DateTime d1 = d0.plusDays(1);
				float x1 = ganttPrint.getX(d1.getMillis());
				paintWeekend(canvas, position, x0, w0);
				paintWeekend(canvas, position, x1, w0);
			}
			if (dayOfWeek == 1) {
				paintWeek(canvas, position, x0);
			}
			int dayOfMonth = d0.getDayOfMonth();
			if (dayOfMonth == 1) {
				paintMonth(canvas, position, x0);
			}
		}
	}

	private void paintDay(PdfContentByte canvas, Rectangle position, float f) {
		if (ganttPrint.scaleLevel != SCALE_LEVEL.DAILY)
			return;
		paintVerticalLine(canvas, position, f, 0.2f);
	}

	private void paintWeek(PdfContentByte canvas, Rectangle position, float f) {
		if (ganttPrint.scaleLevel == SCALE_LEVEL.MONTHLY)
			return;
		paintVerticalLine(canvas, position, f, 0.5f);
	}

	private void paintMonth(PdfContentByte canvas, Rectangle position, float f) {
		if (ganttPrint.scaleLevel != SCALE_LEVEL.MONTHLY)
			return;
		paintVerticalLine(canvas, position, f, 0.5f);
	}

	private void paintVerticalLine(PdfContentByte canvas, Rectangle position,
			float f, float w) {
		canvas.setLineWidth(w);
		canvas.setColorStroke(Color.black);
		float x = position.getLeft() + f;
		canvas.moveTo(x, position.getBottom());
		canvas.lineTo(x, position.getTop());
		canvas.stroke();
	}

	private void paintWeekend(PdfContentByte canvas, Rectangle position,
			float f, float w) {
		if (ganttPrint.scaleLevel == SCALE_LEVEL.MONTHLY)
			return;
		canvas.setColorFill(Color.lightGray);
		float x = position.getLeft() + f;
		float y = position.getBottom();
		float h = position.getHeight();
		canvas.rectangle(x, y, w, h);
		canvas.fill();
	}

	private void paintBar(PdfContentByte canvas, Rectangle position) {
		float barStart = ganttPrint.getX(scheduleItem.getStart().getTime());
		float barFinish = ganttPrint.getX(scheduleItem.getFinish().getTime());
		float barWidth = barFinish - barStart;

		float x = position.getLeft() + barStart;
		float y = position.getBottom() + position.getHeight() / 3;
		float w = barWidth;
		float h = position.getHeight() / 3;

		paintBar(canvas, x, w, y, h);
	}

	private void paintBar(PdfContentByte canvas, float x, float w, float y,
			float h) {

		/* shadow */
		canvas.setLineWidth(0.5f);
		canvas.setColorStroke(Color.gray);
		canvas.setColorFill(Color.gray);
		canvas.roundRectangle(x + 2, y - 1, w, h, 1);
		canvas.fillStroke();

		/* bar */
		canvas.setLineWidth(0.5f);
		canvas.setColorStroke(Color.blue);
		canvas.setColorFill(Color.decode("0xaaaaff"));
		canvas.roundRectangle(x, y, w, h, 1);
		canvas.fillStroke();
	}

}
