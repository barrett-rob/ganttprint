package com.mincom.viva.ganttprint;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.Interval;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPCellEvent;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPageEvent;
import com.lowagie.text.pdf.PdfWriter;

public class GanttPrint {

	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory
			.getLogger(GanttPrint.class);

	static final Font DATA_FONT = new Font(Font.HELVETICA, 9);
	static final Font HEADER_FONT = new Font(Font.HELVETICA, 8, Font.BOLD);
	static final int TABLE_WIDTH = 500;
	static final int ROW_HEIGHT = 19;
	static final int BORDER_PADDING = 5;

	public enum SIZE {
		A2(PageSize.A2.rotate()), A3(PageSize.A3.rotate()), A4(PageSize.A4
				.rotate());

		final Rectangle rectangle;

		SIZE(Rectangle r) {
			this.rectangle = r;
		}
	}

	private final float[] dataWidths = new float[] { 60, 30, 250, 100, 100 };
	final float dataWidth;
	{
		int dw = 0;
		for (int i = 0; i < dataWidths.length; dw += dataWidths[i++])
			;
		dataWidth = dw;
	}
	private final float[] totalWidths = new float[dataWidths.length + 1];
	private final float barWidth;
	private final PdfPTable table = new PdfPTable(totalWidths.length);
	{
		table.setHorizontalAlignment(PdfPTable.ALIGN_LEFT);
		table.setLockedWidth(true);
	}
	private final PdfWriter pdfWriter;
	private final Document document;
	private final ByteArrayOutputStream baos;
	private final Schedule schedule;
	final SIZE size;

	private final SimpleDateFormat sdf = new SimpleDateFormat(
			"dd/MM/yyyy HH:mm");
	{
		sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
	}

	DateTime first = null, last = null;
	private long range;

	public GanttPrint(Schedule s) {
		this(s, SIZE.A2);
	}

	public GanttPrint(Schedule schedule, SIZE size) {
		if (schedule == null)
			throw new IllegalArgumentException("require non-null model");
		this.schedule = schedule;
		if (size == null)
			throw new IllegalArgumentException("require non-null size");
		this.size = size;
		try {
			logger.debug("creating document for gantt print");
			for (int i = 0; i < dataWidths.length; i++)
				totalWidths[i] = dataWidths[i];
			barWidth = size.rectangle.getWidth() - dataWidth - BORDER_PADDING
					* 2 - 2;
			totalWidths[totalWidths.length - 1] = barWidth;
			pdfWriter = PdfWriter.getInstance(document = new Document(
					this.size.rectangle),
					this.baos = new ByteArrayOutputStream());
			pdfWriter.setPageEvent(new GanttPrintEventListener(this));
			document.addAuthor("Mincom Ltd");
			document.addTitle("Gantt Chart Print");
			document.setMargins(BORDER_PADDING + 1, BORDER_PADDING + 1,
					BORDER_PADDING + 1, BORDER_PADDING + 1);
			document.open();
			if (schedule.isEmpty()) {
				document.add(new Paragraph(BORDER_PADDING * 3,
						"   *** no data ***"));
			} else {
				establishDateRange();
				printScheduleData();
				printSummary();
			}
			document.close();
			baos.flush();
			logger.debug("created document for gantt print");
		} catch (Exception e) {
			logger.debug("exception creating document for gantt print", e);
			throw new RuntimeException(e);
		}
	}

	private void establishDateRange() {
		for (ScheduleItem si : schedule) {
			DateTime start = new DateTime(si.getStart());
			if (first == null)
				first = new DateTime(start);
			if (start.isBefore(first))
				first = start;
			DateTime finish = new DateTime(si.getFinish());
			if (last == null)
				last = new DateTime(finish);
			if (finish.isAfter(last))
				last = finish;
		}
		logger.debug("first: {}", first);
		logger.debug("last: {}", last);
		if (first.isAfter(last))
			throw new IllegalStateException("first start date [" + first
					+ "] is after last finish date [" + last + "]");
		padDateRange();
	}

	private void padDateRange() {
		DateTime f = first;
		DateTime l = last;

		/* pad out to week boundaries */
		f = f.withMillisOfDay(0).withDayOfWeek(1);
		l = l.withMillisOfDay(0).withDayOfWeek(1).plusWeeks(1).minusSeconds(1);

		/* if close to week boundaries pad out by another week */
		if (new Interval(f.getMillis(), first.getMillis()).toDuration()
				.isShorterThan(Duration.standardDays(2))) {
			f = f.minusWeeks(1);
		}
		if (new Interval(last.getMillis(), l.getMillis()).toDuration()
				.isShorterThan(Duration.standardDays(2))) {
			l = l.plusWeeks(1);
		}

		/* set state */
		first = f;
		last = l;
		range = last.getMillis() - first.getMillis();
	}

	private void printScheduleData() throws DocumentException {

		logger.debug("printing schedule ");

		table.setTotalWidth(totalWidths);

		/* set headers */
		table.addCell(newHeaderCell("Work Order"));
		table.addCell(newHeaderCell("Task"));
		table.addCell(newHeaderCell("Description"));
		table.addCell(newHeaderCell("Start"));
		table.addCell(newHeaderCell("Finish"));
		table.addCell(newCell(/* empty */));
		table.setHeaderRows(1);

		logger.debug("schedule contains [{}] items", schedule.size());

		for (ScheduleItem si : schedule) {
			table.addCell(newDataCell(si.getWorkOrder()));
			table.addCell(newDataCell(si.getTaskNo()));
			table.addCell(newDataCell(si.getDescription()));
			String start = sdf.format(si.getStart());
			if (start == null)
				start = "null";
			table.addCell(newDataCell(start));
			String finish = sdf.format(si.getFinish());
			if (finish == null)
				finish = "null";
			table.addCell(newDataCell(finish));
			PdfPCell cell = newCell(/* empty */);
			cell.setCellEvent(new GanttPrintPdfPCellEvent(this, si));
			table.addCell(cell);
		}

		table.setComplete(true);
		document.add(table);
	}

	private PdfPCell newHeaderCell(String content) {
		PdfPCell cell = newCell();
		cell.addElement(newChunk(content, HEADER_FONT));
		return cell;
	}

	private PdfPCell newDataCell(String content) {
		PdfPCell cell = newCell();
		cell.addElement(newChunk(content, DATA_FONT));
		return cell;
	}

	private Chunk newChunk(String content, Font font) {
		Chunk chunk = new Chunk(content);
		chunk.setFont(font);
		return chunk;
	}

	private PdfPCell newCell() {
		PdfPCell cell = new PdfPCell();
		cell.setPaddingBottom(3);
		cell.setPaddingLeft(5);
		cell.setPaddingTop(0);
		cell.setPaddingRight(0);
		cell.setFixedHeight(ROW_HEIGHT);
		cell.setBorderColor(Color.gray);
		cell.setNoWrap(true);
		return cell;
	}

	float getX(long instant) {
		float x = (instant - first.getMillis()) * getScalingFactor();
		return x;
	}

	private float getScalingFactor() {
		return barWidth == 0 ? 1 : (barWidth / range);
	}

	private void printSummary() throws DocumentException {
		DateTimeFormatter dtf = DateTimeFormat.mediumDateTime();
		document.newPage();
		Paragraph p = new Paragraph("printed " + schedule.size()
				+ " schedule items, ranging between " + first.toString(dtf)
				+ " and " + last.toString(dtf));
		p.setAlignment(Paragraph.ALIGN_CENTER);
		p.setLeading(20);
		document.add(p);
		p = new Paragraph("printed at " + new DateTime().toString(dtf));
		p.setAlignment(Paragraph.ALIGN_CENTER);
		p.setLeading(20);
		document.add(p);
	}

	public byte[] getBytes() {
		return baos.toByteArray();
	}
}

class GanttPrintEventListener implements PdfPageEvent {

	private final GanttPrint ganttPrint;

	public GanttPrintEventListener(GanttPrint ganttPrint) {
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

class GanttPrintPdfPCellEvent implements PdfPCellEvent {

	private final ScheduleItem scheduleItem;
	private final GanttPrint ganttPrint;

	public GanttPrintPdfPCellEvent(GanttPrint ganttPrint, ScheduleItem si) {
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
		}
	}

	private void paintDay(PdfContentByte canvas, Rectangle position, float f) {
		canvas.setLineWidth(0.1f);
		canvas.setColorStroke(Color.black);
		float x = position.getLeft() + f;
		canvas.moveTo(x, position.getBottom());
		canvas.lineTo(x, position.getTop());
		canvas.stroke();
	}

	private void paintWeekend(PdfContentByte canvas, Rectangle position,
			float f, float w) {
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